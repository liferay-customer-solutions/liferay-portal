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
import com.liferay.one.constants.SupportRegionConstants;
import com.liferay.one.salesforce.model.Opportunity;
import com.liferay.one.salesforce.model.OpportunityLineItem;
import com.liferay.one.salesforce.model.Project;
import com.liferay.one.util.SupportRegionUtil;
import com.liferay.portal.kernel.util.Validator;

import java.math.BigDecimal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
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

	public void cancelOrder(long commerceOrderId) throws Exception {
		OrderResource orderResource = _buildOrderResource();

		Order order = new Order();

		order.setOrderStatus(
			() -> CommerceOrderConstants.ORDER_STATUS_CANCELLED);

		orderResource.patchOrder(commerceOrderId, order);
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

	public String getSupportRegion(long accountId, Long defaultBillingAddressId)
		throws Exception {

		String addressCountry = null;

		if (Validator.isNotNull(defaultBillingAddressId)) {
			PostalAddress postalAddress =
				_postalAddressService.getPostalAddress(defaultBillingAddressId);

			addressCountry = postalAddress.getAddressCountry();
		}

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

			if (Validator.isNull(opportunitySoldBy)) {
				continue;
			}

			return SupportRegionUtil.getSupportRegion(
				opportunitySoldBy, addressCountry);
		}

		return SupportRegionConstants.GLOBAL;
	}

	public void patchOrderCustomFields(
			long commerceOrderId, Map<String, ?> customFields)
		throws Exception {

		OrderResource orderResource = _buildOrderResource();

		Order order = new Order();

		order.setCustomFields(() -> customFields);

		orderResource.patchOrder(commerceOrderId, order);
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
			Long contractId, String currencyCode, Opportunity opportunity,
			List<OpportunityLineItem> opportunityLineItems,
			Project salesforceProject)
		throws Exception {

		Order order = new Order();

		order.setAccountExternalReferenceCode(
			account::getExternalReferenceCode);
		order.setAccountId(account::getId);

		Long channelId = _fetchChannelId();

		order.setChannelId(() -> channelId);

		order.setExternalReferenceCode(opportunity::getId);

		if (account.getDefaultBillingAddressId() != null) {
			order.setBillingAddressId(account::getDefaultBillingAddressId);
		}

		if (account.getDefaultShippingAddressId() != null) {
			order.setShippingAddressId(account::getDefaultShippingAddressId);
		}

		order.setCurrencyCode(() -> currencyCode);

		BigDecimal total = _getTotal(opportunityLineItems);

		if (total != null) {
			order.setTotal(() -> total);
		}

		UserAccount userAccount = null;

		if (Validator.isNotNull(opportunity.getOwnerEmailAddress())) {
			userAccount = _userAccountService.fetchUserAccountByEmailAddress(
				opportunity.getOwnerEmailAddress());
		}

		if (userAccount != null) {
			order.setCreatorEmailAddress(opportunity::getOwnerEmailAddress);
		}

		Map<String, Object> customFields = _getCustomFields(
			contractId, opportunity, salesforceProject);

		order.setCustomFields(() -> customFields);

		Order existingOrder = fetchOrderByExternalReferenceCode(
			opportunity.getId());

		OrderResource orderResource = _buildOrderResource();

		if (existingOrder != null) {
			orderResource.patchOrder(existingOrder.getId(), order);

			return fetchOrderByExternalReferenceCode(opportunity.getId());
		}

		return orderResource.postOrder(order);
	}

	private CurrencyResource _buildCurrencyResource() {
		return CurrencyResource.builder(
		).endpoint(
			lxcDXPMainDomain, lxcDXPServerProtocol
		).header(
			HttpHeaders.AUTHORIZATION, getAuthorization()
		).build();
	}

	private OrderResource _buildOrderResource() {
		return OrderResource.builder(
		).endpoint(
			lxcDXPMainDomain, lxcDXPServerProtocol
		).header(
			HttpHeaders.AUTHORIZATION, getAuthorization()
		).parameters(
			"nestedFields", "account,billingAddress,customFields,orderItems"
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
				_SALESFORCE_CHANNEL
			).toUri());

		if (Validator.isNull(response)) {
			throw new Exception(
				"Unable to find commerce channel " + _SALESFORCE_CHANNEL);
		}

		JSONObject jsonObject = new JSONObject(response);

		_channelId = jsonObject.getLong("id");

		return _channelId;
	}

	private Map<String, Object> _getCustomFields(
		Long contractId, Opportunity opportunity, Project salesforceProject) {

		Map<String, Object> customFields = new HashMap<>();

		if (Validator.isNotNull(opportunity.getOwnerEmailAddress())) {
			customFields.put(
				"accountOwnerEmailAddress", opportunity.getOwnerEmailAddress());
		}

		if (Validator.isNotNull(opportunity.getOwnerFirstName())) {
			customFields.put(
				"accountOwnerFirstName", opportunity.getOwnerFirstName());
		}

		if (Validator.isNotNull(opportunity.getOwnerLastName())) {
			customFields.put(
				"accountOwnerLastName", opportunity.getOwnerLastName());
		}

		if (contractId != null) {
			customFields.put("contractId", contractId);
		}

		if ((salesforceProject != null) &&
			Validator.isNotNull(salesforceProject.getLDPWorkspaceName())) {

			customFields.put(
				"ldpWorkspaceName", salesforceProject.getLDPWorkspaceName());
		}

		if (Validator.isNotNull(opportunity.getProductFamily())) {
			customFields.put(
				"opportunityProductFamily", opportunity.getProductFamily());
		}

		if (Validator.isNotNull(opportunity.getSoldBy())) {
			customFields.put("opportunitySoldBy", opportunity.getSoldBy());
		}

		if (Validator.isNotNull(opportunity.getStageName())) {
			customFields.put(
				"opportunityStageName", opportunity.getStageName());
		}

		if (Validator.isNotNull(opportunity.getType())) {
			customFields.put("opportunityType", opportunity.getType());
		}

		if (Validator.isNotNull(
				opportunity.getAmendedContractOpportunityId())) {

			customFields.put(
				"parentOpportunityId",
				opportunity.getAmendedContractOpportunityId());
		}

		if (Validator.isNotNull(opportunity.getResellerName())) {
			customFields.put("partnerAccount", opportunity.getResellerName());
		}

		customFields.put(
			"partnerFirstLineSupport",
			String.valueOf(opportunity.isFirstLineSupport()));

		if ((salesforceProject != null) &&
			Validator.isNotNull(salesforceProject.getName())) {

			customFields.put("projectName", salesforceProject.getName());
		}

		customFields.put("renewal", opportunity.isRenewal());

		if (Validator.isNotNull(opportunity.getProjectId())) {
			customFields.put("salesforceProjectId", opportunity.getProjectId());
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

	private BigDecimal _getTotal(
		List<OpportunityLineItem> opportunityLineItems) {

		BigDecimal total = null;

		for (OpportunityLineItem opportunityLineItem : opportunityLineItems) {
			Double totalPrice = opportunityLineItem.getTotalPrice();

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

	private static final int _ACCOUNT_TYPE_BUSINESS = 2;

	private static final int _ACCOUNT_TYPE_PERSON = 1;

	private static final int _PAGE_SIZE = 500;

	private static final String _SALESFORCE_CHANNEL = "SALESFORCE_CHANNEL";

	private static final double _TAX_PERCENTAGE = 0.20;

	private static final Set<String> _europeanCountryISOCodes = Set.of(
		"AT", "BE", "BG", "CY", "CZ", "DE", "DK", "EE", "ES", "FI", "FR", "GR",
		"HR", "HU", "IE", "IT", "LT", "LU", "LV", "MT", "NL", "PL", "PT", "RO",
		"SE", "SI", "SK");

	private volatile Long _channelId;

	@Autowired
	private CommerceOrderItemService _commerceOrderItemService;

	@Autowired
	private PostalAddressService _postalAddressService;

	@Autowired
	private UserAccountService _userAccountService;

}