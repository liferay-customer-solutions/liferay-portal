/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.admin.user.client.dto.v1_0.PostalAddress;
import com.liferay.headless.commerce.admin.catalog.client.dto.v1_0.Currency;
import com.liferay.headless.commerce.admin.catalog.client.pagination.Pagination;
import com.liferay.headless.commerce.admin.catalog.client.resource.v1_0.CurrencyResource;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.Account;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.BillingAddress;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.Order;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.OrderItem;
import com.liferay.headless.commerce.admin.order.client.problem.Problem;
import com.liferay.headless.commerce.admin.order.client.resource.v1_0.OrderResource;
import com.liferay.one.constants.SupportRegionConstants;
import com.liferay.one.util.SupportRegionUtil;
import com.liferay.portal.kernel.util.Validator;

import java.math.BigDecimal;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.json.JSONArray;
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

		orderResource.patchOrder(
			commerceOrderId,
			new Order() {
				{
					setCustomFields(() -> customFields);
					setTaxAmount(() -> taxAmount);
					setTotal(() -> total);
				}
			});

		for (OrderItem orderItem : order.getOrderItems()) {
			BigDecimal finalPrice = orderItem.getFinalPrice();

			_commerceOrderItemService.patchOrderItem(
				orderItem.getId(),
				new OrderItem() {
					{
						setFinalPrice(() -> finalPrice);
						setFinalPriceWithTaxAmount(
							() -> finalPrice.add(
								finalPrice.multiply(
									BigDecimal.valueOf(_TAX_PERCENTAGE))));
						setPriceManuallyAdjusted(() -> true);
					}
				});
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

	public String getSupportRegion(long accountId, Long defaultBillingAddressId)
		throws Exception {

		String addressCountry = null;

		if (Validator.isNotNull(defaultBillingAddressId)) {
			PostalAddress postalAddress =
				_postalAddressService.getPostalAddress(defaultBillingAddressId);

			addressCountry = postalAddress.getAddressCountry();
		}

		String response = get(
			getAuthorization(),
			UriComponentsBuilder.fromPath(
				"/o/headless-commerce-admin-order/v1.0/orders"
			).queryParam(
				"filter", "accountId/any(x:x eq " + accountId + ")"
			).queryParam(
				"nestedFields", "customFields"
			).build(
			).toUri());

		if (Validator.isNull(response)) {
			return SupportRegionConstants.GLOBAL;
		}

		JSONObject responseJSONObject = new JSONObject(response);

		JSONArray itemsJSONArray = responseJSONObject.optJSONArray("items");

		if (itemsJSONArray == null) {
			return SupportRegionConstants.GLOBAL;
		}

		for (int i = 0; i < itemsJSONArray.length(); i++) {
			JSONObject orderJSONObject = itemsJSONArray.getJSONObject(i);

			JSONObject customFieldsJSONObject = orderJSONObject.optJSONObject(
				"customFields");

			if (customFieldsJSONObject == null) {
				continue;
			}

			String opportunitySoldBy = customFieldsJSONObject.optString(
				"opportunitySoldBy");

			if (Validator.isNull(opportunitySoldBy)) {
				continue;
			}

			return SupportRegionUtil.getSupportRegion(
				opportunitySoldBy, addressCountry);
		}

		return SupportRegionConstants.GLOBAL;
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
			null, "code eq 'EUR'", Pagination.of(1, 1), null
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

	private static final double _TAX_PERCENTAGE = 0.20;

	private static final Set<String> _europeanCountryISOCodes = Set.of(
		"AT", "BE", "BG", "CY", "CZ", "DE", "DK", "EE", "ES", "FI", "FR", "GR",
		"HR", "HU", "IE", "IT", "LT", "LU", "LV", "MT", "NL", "PL", "PT", "RO",
		"SE", "SI", "SK");

	@Autowired
	private CommerceOrderItemService _commerceOrderItemService;

	@Autowired
	private PostalAddressService _postalAddressService;

}