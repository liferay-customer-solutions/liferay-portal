/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.admin.user.client.dto.v1_0.PostalAddress;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.headless.commerce.admin.catalog.client.dto.v1_0.Currency;
import com.liferay.headless.commerce.admin.catalog.client.resource.v1_0.CurrencyResource;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.Account;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.BillingAddress;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.Order;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.OrderItem;
import com.liferay.headless.commerce.admin.order.client.pagination.Page;
import com.liferay.headless.commerce.admin.order.client.pagination.Pagination;
import com.liferay.headless.commerce.admin.order.client.problem.Problem;
import com.liferay.headless.commerce.admin.order.client.resource.v1_0.OrderResource;
import com.liferay.one.constants.CommerceOrderConstants;
import com.liferay.one.model.AccountSupportInfo;
import com.liferay.one.salesforce.model.SalesforceOpportunity;
import com.liferay.one.salesforce.model.SalesforceOpportunityLineItem;
import com.liferay.one.salesforce.model.SalesforceProject;
import com.liferay.one.util.KeyedLock;
import com.liferay.one.util.SupportLanguageUtil;
import com.liferay.one.util.SupportRegionUtil;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.Validator;

import java.math.BigDecimal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Felipe Veloso
 */
@Component
public class CommerceOrderService extends OneBaseService {

	public void calculateTax(long commerceOrderId) throws Exception {
		OrderResource orderResource = _buildOrderResource();

		Order order = orderResource.getOrder(commerceOrderId);

		BillingAddress billingAddress = order.getBillingAddress();

		if ((billingAddress == null) ||
			!_isTaxApplicable(order.getAccount(), billingAddress)) {

			return;
		}

		Map<String, String> customFields = _getCustomFields(order);

		BigDecimal subtotalAmount = BigDecimal.valueOf(
			order.getSubtotalAmount());

		BigDecimal taxAmount = subtotalAmount.multiply(
			BigDecimal.valueOf(_TAX_PERCENTAGE));

		BigDecimal total = subtotalAmount.add(taxAmount);

		Order taxedOrder = new Order();

		taxedOrder.setCustomFields(() -> customFields);
		taxedOrder.setTaxAmount(() -> taxAmount);
		taxedOrder.setTotal(() -> total);

		orderResource.patchOrder(commerceOrderId, taxedOrder);

		for (OrderItem orderItem : order.getOrderItems()) {
			BigDecimal finalPrice = orderItem.getFinalPrice();

			OrderItem taxedOrderItem = new OrderItem();

			taxedOrderItem.setFinalPrice(() -> finalPrice);
			taxedOrderItem.setFinalPriceWithTaxAmount(
				() -> finalPrice.add(
					finalPrice.multiply(BigDecimal.valueOf(_TAX_PERCENTAGE))));
			taxedOrderItem.setPriceManuallyAdjusted(() -> true);

			_commerceOrderItemService.patchOrderItem(
				orderItem.getId(), taxedOrderItem);
		}
	}

	public void completeOrder(long orderId, int paymentStatus)
		throws Exception {

		completeOrder(null, orderId, paymentStatus);
	}

	public void completeOrder(
			Map<String, ?> customFields, long orderId, int paymentStatus)
		throws Exception {

		updateOrder(
			customFields, orderId, CommerceOrderConstants.ORDER_STATUS_PENDING);

		updateOrder(
			null, orderId, CommerceOrderConstants.ORDER_STATUS_PROCESSING);

		updateOrder(
			null, orderId, CommerceOrderConstants.ORDER_STATUS_COMPLETED,
			paymentStatus);

		Order order = fetchCommerceOrder(orderId);

		if ((order != null) &&
			Objects.equals(
				order.getOrderTypeExternalReferenceCode(), "AI_HUB")) {

			_provisionAiHub(order);
		}
	}

	public void completeSettledOrder(long orderId) throws Exception {
		if (!_inFlightOrderIds.add(orderId)) {
			return;
		}

		try {
			Order order = fetchCommerceOrder(orderId);

			if (order == null) {
				return;
			}

			String orderTypeExternalReferenceCode =
				order.getOrderTypeExternalReferenceCode();

			if ((orderTypeExternalReferenceCode == null) ||
				!_completableOrderTypeExternalReferenceCodes.contains(
					orderTypeExternalReferenceCode)) {

				if (_log.isDebugEnabled()) {
					_log.debug(
						StringBundler.concat(
							"Unable to complete order ", orderId,
							" because its order type does not complete on ",
							"payment"));
				}

				return;
			}

			if (_awaitSettledPaymentStatus(order, orderId) == null) {
				return;
			}

			_keyedLock.withLock(
				"order-completion#" + orderId,
				() -> {
					Order settledOrder = fetchCommerceOrder(orderId);

					if (settledOrder == null) {
						return;
					}

					Integer paymentStatus = _getSettledPaymentStatus(
						settledOrder);

					if ((paymentStatus == null) ||
						!_isCompletableOrderStatus(settledOrder)) {

						return;
					}

					completeOrder(orderId, paymentStatus);
				});
		}
		finally {
			_inFlightOrderIds.remove(orderId);
		}
	}

	@Scheduled(cron = "0 0 0 * * *")
	public void completeSettledOrders() throws Exception {
		List<Order> orders = getOrders(
			StringBundler.concat(
				"(orderStatus/any(x:x eq ",
				CommerceOrderConstants.ORDER_STATUS_PENDING,
				") or orderStatus/any(x:x eq ",
				CommerceOrderConstants.ORDER_STATUS_PROCESSING,
				")) and ((paymentStatus eq ",
				CommerceOrderConstants.ORDER_PAYMENT_STATUS_COMPLETED,
				") or (paymentStatus eq ",
				CommerceOrderConstants.ORDER_PAYMENT_STATUS_NOT_REQUIRED,
				"))"));

		for (Order order : orders) {
			try {
				completeSettledOrder(order.getId());
			}
			catch (Exception exception) {
				_log.error(
					"Unable to complete order " + order.getId(), exception);
			}
		}
	}

	public Order fetchCommerceOrder(long commerceOrderId) throws Exception {
		OrderResource orderResource = _buildOrderResource();

		try {
			return orderResource.getOrder(commerceOrderId);
		}
		catch (Problem.ProblemException problemException) {
			Problem problem = problemException.getProblem();

			if ((problem != null) && isNotFound(problem.getStatus())) {
				return null;
			}

			throw problemException;
		}
	}

	public Order fetchOrderByExternalReferenceCode(String externalReferenceCode)
		throws Exception {

		OrderResource orderResource = _buildOrderResource();

		try {
			return orderResource.getOrderByExternalReferenceCode(
				externalReferenceCode);
		}
		catch (Problem.ProblemException problemException) {
			Problem problem = problemException.getProblem();

			if ((problem != null) && isNotFound(problem.getStatus())) {
				return null;
			}

			throw problemException;
		}
	}

	public List<Order> getAccountOrders(long accountId) throws Exception {
		OrderResource orderResource = _buildOrderResource();

		List<Order> orders = new ArrayList<>();

		int page = 1;

		while (true) {
			Page<Order> ordersPage = orderResource.getOrdersPage(
				null, "accountId/any(x:x eq " + accountId + ")",
				Pagination.of(page, _PAGE_SIZE), null);

			orders.addAll(ordersPage.getItems());

			if (page >= ordersPage.getLastPage()) {
				break;
			}

			page++;
		}

		return orders;
	}

	public AccountSupportInfo getAccountSupportInfo(
			long accountId, Long defaultBillingAddressId)
		throws Exception {

		String opportunitySoldBy = _getOpportunitySoldBy(accountId);
		String addressCountry = _getAddressCountry(defaultBillingAddressId);

		return new AccountSupportInfo(
			SupportLanguageUtil.getLanguage(opportunitySoldBy, addressCountry),
			SupportRegionUtil.getSupportRegion(
				opportunitySoldBy, addressCountry));
	}

	public Order getCommerceOrder(long commerceOrderId) throws Exception {
		OrderResource orderResource = _buildOrderResource();

		return orderResource.getOrder(commerceOrderId);
	}

	public List<Order> getOrders(String filterString) throws Exception {
		List<Order> orders = new ArrayList<>();

		OrderResource orderResource = _buildOrderResource();

		int page = 1;

		while (true) {
			Page<Order> ordersPage = orderResource.getOrdersPage(
				null, filterString, Pagination.of(page, _PAGE_SIZE), null);

			orders.addAll(ordersPage.getItems());

			if (page >= ordersPage.getLastPage()) {
				break;
			}

			page++;
		}

		return orders;
	}

	@Async
	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReady() {
		try {
			completeSettledOrders();
		}
		catch (Exception exception) {
			_log.error(
				"Unable to complete settled orders on application startup",
				exception);
		}
	}

	public void patchOrderCustomFields(
			long commerceOrderId, Map<String, ?> customFields)
		throws Exception {

		OrderResource orderResource = _buildOrderResource();

		Order order = new Order();

		order.setCustomFields(() -> customFields);

		orderResource.patchOrder(commerceOrderId, order);
	}

	public void patchOrderExternalReferenceCode(
			long orderId, String externalReferenceCode)
		throws Exception {

		OrderResource orderResource = _buildOrderResource();

		Order order = new Order();

		order.setExternalReferenceCode(() -> externalReferenceCode);

		orderResource.patchOrder(orderId, order);
	}

	public void updateOrder(
			Map<String, ?> customFields, long orderId, int orderStatus)
		throws Exception {

		OrderResource orderResource = _buildOrderResource();

		Order order = new Order();

		order.setCustomFields(() -> customFields);
		order.setOrderStatus(() -> orderStatus);

		orderResource.patchOrder(orderId, order);
	}

	public void updateOrder(
			Map<String, ?> customFields, long orderId, int orderStatus,
			int paymentStatus)
		throws Exception {

		OrderResource orderResource = _buildOrderResource();

		Order order = new Order();

		order.setCustomFields(() -> customFields);
		order.setOrderStatus(() -> orderStatus);
		order.setPaymentStatus(() -> paymentStatus);

		orderResource.patchOrder(orderId, order);
	}

	public Order upsertOrder(
			com.liferay.headless.admin.user.client.dto.v1_0.Account account,
			Long contractId, String currencyCode,
			SalesforceOpportunity salesforceOpportunity,
			List<SalesforceOpportunityLineItem> salesforceOpportunityLineItems,
			SalesforceProject salesforceProject)
		throws Exception {

		Order order = new Order();

		order.setAccountExternalReferenceCode(
			account::getExternalReferenceCode);
		order.setAccountId(account::getId);

		Long channelId = _fetchChannelId();

		order.setChannelId(() -> channelId);

		order.setExternalReferenceCode(salesforceOpportunity::getId);

		if (account.getDefaultBillingAddressId() != null) {
			order.setBillingAddressId(account::getDefaultBillingAddressId);
		}

		if (account.getDefaultShippingAddressId() != null) {
			order.setShippingAddressId(account::getDefaultShippingAddressId);
		}

		order.setCurrencyCode(() -> currencyCode);

		BigDecimal total = _getTotal(salesforceOpportunityLineItems);

		if (total != null) {
			order.setTotal(() -> total);
		}

		UserAccount userAccount = null;

		if (Validator.isNotNull(salesforceOpportunity.getOwnerEmailAddress())) {
			userAccount = _userAccountService.fetchUserAccountByEmailAddress(
				salesforceOpportunity.getOwnerEmailAddress());
		}

		if (userAccount != null) {
			order.setCreatorEmailAddress(
				salesforceOpportunity::getOwnerEmailAddress);
		}

		Map<String, Object> customFields = _getCustomFields(
			contractId, salesforceOpportunity, salesforceProject);

		order.setCustomFields(() -> customFields);

		Order existingOrder = fetchOrderByExternalReferenceCode(
			salesforceOpportunity.getId());

		OrderResource orderResource = _buildOrderResource();

		if (existingOrder != null) {
			return orderResource.patchOrder(existingOrder.getId(), order);
		}

		return orderResource.postOrder(order);
	}

	private Integer _awaitSettledPaymentStatus(Order order, long orderId)
		throws Exception {

		int retryIndex = 0;

		while (true) {
			if (_isCompletableOrderStatus(order)) {
				Integer paymentStatus = _getSettledPaymentStatus(order);

				if (paymentStatus != null) {
					return paymentStatus;
				}
			}
			else if (!_isTransitionalOrderStatus(order)) {
				return null;
			}

			if (retryIndex >= _settledPaymentRetryDelays.length) {
				if (_isCompletableOrderStatus(order) && _log.isInfoEnabled()) {
					_log.info(
						StringBundler.concat(
							"Unable to complete order ", orderId,
							" because its payment is still pending"));
				}

				return null;
			}

			try {
				Thread.sleep(_settledPaymentRetryDelays[retryIndex++]);
			}
			catch (InterruptedException interruptedException) {
				if (_log.isDebugEnabled()) {
					_log.debug(interruptedException);
				}

				Thread thread = Thread.currentThread();

				thread.interrupt();

				return null;
			}

			order = fetchCommerceOrder(orderId);

			if (order == null) {
				return null;
			}
		}
	}

	private CurrencyResource _buildCurrencyResource() {
		return CurrencyResource.builder(
		).endpoint(
			getDXPEndpointAddress(), lxcDXPServerProtocol
		).header(
			HttpHeaders.AUTHORIZATION, getAuthorization()
		).build();
	}

	private OrderResource _buildOrderResource() {
		return OrderResource.builder(
		).endpoint(
			getDXPEndpointAddress(), lxcDXPServerProtocol
		).header(
			HttpHeaders.AUTHORIZATION, getAuthorization()
		).parameters(
			"nestedFields",
			"account,billingAddress,customFields,orderItems," +
				"orderItems.customFields"
		).build();
	}

	private Long _fetchChannelId() throws Exception {
		if (_channelId != null) {
			return _channelId;
		}

		String response = get(
			getAuthorization(),
			UriComponentsBuilder.fromPath(
				"/o/headless-commerce-admin-channel/v1.0/channels" +
					"/by-externalReferenceCode/{externalReferenceCode}"
			).buildAndExpand(
				_LIFERAY_ONE_CHANNEL
			).toUri());

		if (Validator.isNull(response)) {
			throw new Exception(
				"Unable to find commerce channel " + _LIFERAY_ONE_CHANNEL);
		}

		JSONObject jsonObject = new JSONObject(response);

		_channelId = jsonObject.getLong("id");

		return _channelId;
	}

	private String _getAddressCountry(Long defaultBillingAddressId)
		throws Exception {

		if (Validator.isNull(defaultBillingAddressId)) {
			return null;
		}

		PostalAddress postalAddress = _postalAddressService.getPostalAddress(
			defaultBillingAddressId);

		return postalAddress.getAddressCountry();
	}

	private Map<String, Object> _getCustomFields(
		Long contractId, SalesforceOpportunity salesforceOpportunity,
		SalesforceProject salesforceProject) {

		Map<String, Object> customFields = new HashMap<>();

		if (Validator.isNotNull(salesforceOpportunity.getOwnerEmailAddress())) {
			customFields.put(
				"accountOwnerEmailAddress",
				salesforceOpportunity.getOwnerEmailAddress());
		}

		if (Validator.isNotNull(salesforceOpportunity.getOwnerFirstName())) {
			customFields.put(
				"accountOwnerFirstName",
				salesforceOpportunity.getOwnerFirstName());
		}

		if (Validator.isNotNull(salesforceOpportunity.getOwnerLastName())) {
			customFields.put(
				"accountOwnerLastName",
				salesforceOpportunity.getOwnerLastName());
		}

		if (contractId != null) {
			customFields.put("contractId", contractId);
		}

		if ((salesforceProject != null) &&
			Validator.isNotNull(salesforceProject.getLDPWorkspaceName())) {

			customFields.put(
				"ldpWorkspaceName", salesforceProject.getLDPWorkspaceName());
		}

		if (Validator.isNotNull(salesforceOpportunity.getProductFamily())) {
			customFields.put(
				"opportunityProductFamily",
				salesforceOpportunity.getProductFamily());
		}

		if (Validator.isNotNull(salesforceOpportunity.getSoldBy())) {
			customFields.put(
				"opportunitySoldBy", salesforceOpportunity.getSoldBy());
		}

		if (Validator.isNotNull(salesforceOpportunity.getStageName())) {
			customFields.put(
				"opportunityStageName", salesforceOpportunity.getStageName());
		}

		if (Validator.isNotNull(salesforceOpportunity.getType())) {
			customFields.put(
				"opportunityType", salesforceOpportunity.getType());
		}

		if (Validator.isNotNull(
				salesforceOpportunity.getAmendedContractOpportunityId())) {

			customFields.put(
				"parentOpportunityId",
				salesforceOpportunity.getAmendedContractOpportunityId());
		}

		if (Validator.isNotNull(salesforceOpportunity.getResellerName())) {
			customFields.put(
				"partnerAccount", salesforceOpportunity.getResellerName());
		}

		customFields.put(
			"partnerFirstLineSupport",
			String.valueOf(salesforceOpportunity.isFirstLineSupport()));

		if ((salesforceProject != null) &&
			Validator.isNotNull(salesforceProject.getName())) {

			customFields.put("projectName", salesforceProject.getName());
		}

		customFields.put("renewal", salesforceOpportunity.isRenewal());

		if (Validator.isNotNull(salesforceOpportunity.getPricebook2Id())) {
			customFields.put(
				"salesforcePricebookId",
				salesforceOpportunity.getPricebook2Id());
		}

		if (Validator.isNotNull(salesforceOpportunity.getProjectId())) {
			customFields.put(
				"salesforceProjectId", salesforceOpportunity.getProjectId());
		}

		return customFields;
	}

	private Map<String, String> _getCustomFields(Order order) throws Exception {
		Map<String, String> customFields =
			(Map<String, String>)order.getCustomFields();

		JSONObject orderMetadataJSONObject = new JSONObject(
			customFields.getOrDefault("order-metadata", "{}"));

		if (orderMetadataJSONObject.has("exchangeRate")) {
			return customFields;
		}

		CurrencyResource currencyResource = _buildCurrencyResource();

		Currency currency = currencyResource.getCurrenciesPage(
			null, "code eq 'EUR'",
			com.liferay.headless.commerce.admin.catalog.client.pagination.
				Pagination.of(1, 1),
			null
		).fetchFirstItem();

		if (currency == null) {
			return customFields;
		}

		customFields.put(
			"order-metadata",
			orderMetadataJSONObject.put(
				"exchangeRate", currency.getRate()
			).toString());

		return customFields;
	}

	private String _getOpportunitySoldBy(long accountId) throws Exception {
		OrderResource orderResource = _buildOrderResource();

		Page<Order> ordersPage = orderResource.getOrdersPage(
			null, "accountId/any(x:x eq " + accountId + ")", null, null);

		for (Order order : ordersPage.getItems()) {
			Map<String, String> customFields =
				(Map<String, String>)order.getCustomFields();

			if (customFields == null) {
				continue;
			}

			String opportunitySoldBy = customFields.get("opportunitySoldBy");

			if (Validator.isNotNull(opportunitySoldBy)) {
				return opportunitySoldBy;
			}
		}

		return null;
	}

	private Integer _getSettledPaymentStatus(Order order) {
		Integer paymentStatus = order.getPaymentStatus();

		if (Objects.equals(
				paymentStatus,
				CommerceOrderConstants.ORDER_PAYMENT_STATUS_COMPLETED) ||
			Objects.equals(
				paymentStatus,
				CommerceOrderConstants.ORDER_PAYMENT_STATUS_NOT_REQUIRED)) {

			return paymentStatus;
		}

		return null;
	}

	private BigDecimal _getTotal(
		List<SalesforceOpportunityLineItem> salesforceOpportunityLineItems) {

		BigDecimal total = null;

		for (SalesforceOpportunityLineItem salesforceOpportunityLineItem :
				salesforceOpportunityLineItems) {

			Double totalPrice = salesforceOpportunityLineItem.getTotalPrice();

			if (totalPrice == null) {
				continue;
			}

			if (total == null) {
				total = BigDecimal.valueOf(totalPrice);
			}
			else {
				total = total.add(BigDecimal.valueOf(totalPrice));
			}
		}

		if ((total != null) && (total.compareTo(BigDecimal.ZERO) < 0)) {
			return null;
		}

		return total;
	}

	private boolean _isCompletableOrderStatus(Order order) {
		Integer orderStatus = order.getOrderStatus();

		if (Objects.equals(
				orderStatus, CommerceOrderConstants.ORDER_STATUS_PENDING) ||
			Objects.equals(
				orderStatus, CommerceOrderConstants.ORDER_STATUS_PROCESSING)) {

			return true;
		}

		return false;
	}

	private boolean _isTaxApplicable(
		Account account, BillingAddress billingAddress) {

		String countryISOCode = billingAddress.getCountryISOCode();

		if (Objects.equals(account.getType(), _ACCOUNT_TYPE_BUSINESS)) {
			return Objects.equals(countryISOCode, "IE");
		}

		if (Objects.equals(account.getType(), _ACCOUNT_TYPE_PERSON)) {
			return _europeanCountryISOCodes.contains(countryISOCode);
		}

		return false;
	}

	private boolean _isTransitionalOrderStatus(Order order) {
		Integer orderStatus = order.getOrderStatus();

		if (Objects.equals(
				orderStatus, CommerceOrderConstants.ORDER_STATUS_IN_PROGRESS) ||
			Objects.equals(
				orderStatus, CommerceOrderConstants.ORDER_STATUS_OPEN)) {

			return true;
		}

		return false;
	}

	private void _provisionAiHub(Order order) throws Exception {
		Map<String, String> customFields =
			(Map<String, String>)order.getCustomFields();

		if ((customFields == null) ||
			!customFields.containsKey("order-metadata")) {

			return;
		}

		try {
			JSONObject orderMetadataJSONObject = new JSONObject(
				customFields.get("order-metadata"));

			if (!orderMetadataJSONObject.has("aiHubForm")) {
				return;
			}

			JSONObject aiHubFormJSONObject =
				orderMetadataJSONObject.getJSONObject("aiHubForm");

			String emailAddress = aiHubFormJSONObject.getString(
				"administratorEmailAddress");

			String firstName = "AI Hub";
			String lastName = "Administrator";

			try {
				UserAccount userAccount =
					_userAccountService.getUserAccountByEmailAddress(
						emailAddress);

				if (userAccount != null) {
					firstName = userAccount.getGivenName();
					lastName = userAccount.getFamilyName();
				}
			}
			catch (Exception exception) {
				if (_log.isWarnEnabled()) {
					_log.warn(
						"Unable to fetch user account details for " +
							emailAddress,
						exception);
				}
			}

			JSONObject provisionJSONObject = new JSONObject(
			).put(
				"accountName", aiHubFormJSONObject.getString("aiHubAccountName")
			).put(
				"companyName",
				order.getAccount(
				).getName()
			).put(
				"userAccounts",
				new JSONArray(
				).put(
					new JSONObject(
					).put(
						"emailAddress", emailAddress
					).put(
						"firstName", firstName
					).put(
						"lastName", lastName
					)
				)
			);

			JSONObject aiHubJSONObject = _aiHubService.provision(
				provisionJSONObject);

			if (aiHubJSONObject != null) {
				_aiHubService.putAIHubApplication(
					"AI-HUB-" + order.getAccountExternalReferenceCode(),
					new JSONObject(
					).put(
						"accountEntryId",
						aiHubJSONObject.getInt("accountEntryId")
					).put(
						"accountName",
						aiHubFormJSONObject.getString("aiHubAccountName")
					).put(
						"administratorEmailAddress",
						aiHubFormJSONObject.getString(
							"administratorEmailAddress")
					).put(
						"r_accountToAIHubApplication_accountEntryERC",
						order.getAccount(
						).getExternalReferenceCode()
					).put(
						"r_orderToAIHubApplication_commerceOrderERC",
						order.getExternalReferenceCode()
					));
			}
		}
		catch (Exception exception) {
			_log.error(
				"Unable to provision AI Hub for order: " + order.getId(),
				exception);
		}
	}

	private static final int _ACCOUNT_TYPE_BUSINESS = 2;

	private static final int _ACCOUNT_TYPE_PERSON = 1;

	private static final String _LIFERAY_ONE_CHANNEL = "LIFERAY_ONE_CHANNEL";

	private static final int _PAGE_SIZE = 500;

	private static final double _TAX_PERCENTAGE = 0.20;

	private static final Log _log = LogFactory.getLog(
		CommerceOrderService.class);

	private static final Set<String>
		_completableOrderTypeExternalReferenceCodes = Set.of(
			"CLIENT_EXTENSION", "CLOUD_APP", "COMPOSITE_APP", "DXP_APP",
			"LOW_CODE_CONFIGURATION", "OTHER");
	private static final Set<String> _europeanCountryISOCodes = Set.of(
		"AT", "BE", "BG", "CY", "CZ", "DE", "DK", "EE", "ES", "FI", "FR", "GR",
		"HR", "HU", "IE", "IT", "LT", "LU", "LV", "MT", "NL", "PL", "PT", "RO",
		"SE", "SI", "SK");

	@Autowired
	private AIHubService _aiHubService;

	private volatile Long _channelId;

	@Autowired
	private CommerceOrderItemService _commerceOrderItemService;

	private final Set<Long> _inFlightOrderIds = ConcurrentHashMap.newKeySet();

	@Autowired
	private KeyedLock _keyedLock;

	@Autowired
	private PostalAddressService _postalAddressService;

	@Value(
		"${liferay.one.commerce.order.settled.payment.retry.delays:2000,5000,10000}"
	)
	private long[] _settledPaymentRetryDelays;

	@Autowired
	private UserAccountService _userAccountService;

}