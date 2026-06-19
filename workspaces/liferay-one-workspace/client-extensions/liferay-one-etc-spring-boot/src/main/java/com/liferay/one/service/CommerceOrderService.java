/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.commerce.admin.order.client.dto.v1_0.Account;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.BillingAddress;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.Order;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.OrderItem;
import com.liferay.headless.commerce.admin.order.client.resource.v1_0.OrderItemResource;
import com.liferay.headless.commerce.admin.order.client.resource.v1_0.OrderResource;
import com.liferay.one.model.CommerceOrder;
import com.liferay.portal.kernel.util.Validator;

import java.math.BigDecimal;

import java.util.Objects;
import java.util.Set;

import org.json.JSONObject;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Felipe Veloso
 */
@Component
public class CommerceOrderService extends OneBaseService {

	public CommerceOrder fetchCommerceOrder(long commerceOrderId)
		throws Exception {

		String response = get(
			getAuthorization(),
			UriComponentsBuilder.fromPath(
				"/o/headless-commerce-admin-order/v1.0/orders/" +
					commerceOrderId
			).queryParam(
				"nestedFields", "customFields"
			).build(
			).toUri());

		if (Validator.isNull(response)) {
			return null;
		}

		return new CommerceOrder(new JSONObject(response));
	}

	public void taxCalculate(long commerceOrderId) throws Exception {
		OrderResource orderResource = _getOrderResource();

		Order order = orderResource.getOrder(commerceOrderId);

		BillingAddress billingAddress = order.getBillingAddress();

		if ((billingAddress == null) ||
			!_isTaxApplicable(order.getAccount(), billingAddress)) {

			return;
		}

		BigDecimal subtotalAmount = BigDecimal.valueOf(
			order.getSubtotalAmount());

		BigDecimal taxAmount = subtotalAmount.multiply(
			BigDecimal.valueOf(_TAX_PERCENTAGE));

		BigDecimal total = subtotalAmount.add(taxAmount);

		OrderItemResource orderItemResource = _getOrderItemResource();

		for (OrderItem orderItem : order.getOrderItems()) {
			BigDecimal finalPrice = orderItem.getFinalPrice();

			orderItemResource.patchOrderItem(
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

		orderResource.patchOrder(
			commerceOrderId,
			new Order() {
				{
					setTaxAmount(() -> taxAmount);
					setTotal(() -> total);
				}
			});
	}

	private OrderItemResource _getOrderItemResource() {
		return OrderItemResource.builder(
		).endpoint(
			lxcDXPMainDomain, lxcDXPServerProtocol
		).header(
			HttpHeaders.AUTHORIZATION, getAuthorization()
		).build();
	}

	private OrderResource _getOrderResource() {
		return OrderResource.builder(
		).endpoint(
			lxcDXPMainDomain, lxcDXPServerProtocol
		).header(
			HttpHeaders.AUTHORIZATION, getAuthorization()
		).parameters(
			"nestedFields", "account,billingAddress,orderItems"
		).build();
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

}