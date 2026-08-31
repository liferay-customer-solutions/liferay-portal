/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.commerce.admin.order.client.dto.v1_0.Order;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.OrderItem;

import java.math.BigDecimal;

import org.json.JSONArray;
import org.json.JSONObject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Felipe Veloso
 */
public class SalesforceServiceTest {

	@Test
	public void testGetLineItemsJSONArraySendsEachOrderItemSku() {
		Order order = new Order();

		order.setOrderItems(
			() -> new OrderItem[] {
				_createOrderItem(1, "PROD-1"), _createOrderItem(3, "PROD-2")
			});
		order.setOrderTypeExternalReferenceCode(() -> "AI_HUB_TOKEN");

		JSONArray jsonArray = _salesforceService.getLineItemsJSONArray(
			"Subscription", order);

		Assertions.assertEquals(2, jsonArray.length());

		JSONObject jsonObject = jsonArray.getJSONObject(0);

		Assertions.assertEquals("New", jsonObject.getString("orderType"));
		Assertions.assertEquals("PROD-1", jsonObject.getString("productId"));
		Assertions.assertEquals(1, jsonObject.getInt("quantity"));

		jsonObject = jsonArray.getJSONObject(1);

		Assertions.assertEquals("PROD-2", jsonObject.getString("productId"));
		Assertions.assertEquals(3, jsonObject.getInt("quantity"));
	}

	private OrderItem _createOrderItem(
		int quantity, String skuExternalReferenceCode) {

		OrderItem orderItem = new OrderItem();

		orderItem.setQuantity(() -> BigDecimal.valueOf(quantity));
		orderItem.setSkuExternalReferenceCode(() -> skuExternalReferenceCode);
		orderItem.setUnitPrice(() -> BigDecimal.TEN);

		return orderItem;
	}

	private final SalesforceService _salesforceService =
		new SalesforceService();

}