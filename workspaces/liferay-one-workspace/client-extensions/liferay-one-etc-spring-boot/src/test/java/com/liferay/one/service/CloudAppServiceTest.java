/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.commerce.admin.order.client.dto.v1_0.Order;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.OrderItem;
import com.liferay.one.constants.CommerceOrderConstants;
import com.liferay.one.util.KeyedLock;
import com.liferay.portal.kernel.util.HashMapBuilder;

import java.math.BigDecimal;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Ricardo Mariz
 */
public class CloudAppServiceTest {

	@BeforeEach
	public void setUp() throws Exception {
		_cloudAppService = new CloudAppService();

		_commerceOrderService = Mockito.mock(CommerceOrderService.class);
		_consoleService = Mockito.mock(ConsoleService.class);

		ReflectionTestUtils.setField(
			_cloudAppService, "_commerceOrderService", _commerceOrderService);
		ReflectionTestUtils.setField(
			_cloudAppService, "_consoleService", _consoleService);
		ReflectionTestUtils.setField(
			_cloudAppService, "_keyedLock", new KeyedLock());
	}

	@Test
	public void testDeployCloudAppRecordsDeployment() throws Exception {
		Map<String, String> customFields = new HashMap<>();

		_whenFetchCommerceOrder(
			_createOrder(
				customFields, CommerceOrderConstants.ORDER_STATUS_PENDING,
				"CLOUD_APP",
				CommerceOrderConstants.ORDER_PAYMENT_STATUS_NOT_REQUIRED));

		Mockito.when(
			_consoleService.deployApp(
				_EMAIL_ADDRESS, String.valueOf(_ORDER_ID), _PROJECT_ID)
		).thenReturn(
			new JSONObject(
			).put(
				"id", _DEPLOYMENT_ID
			).put(
				"projectId", _PROJECT_ID
			)
		);

		_cloudAppService.deployCloudApp(_ORDER_ID, _ORDER_ITEM_ID, _PROJECT_ID);

		JSONObject jsonObject = _getCloudProvisioningJSONObject(customFields);

		Assertions.assertEquals(1, jsonObject.getInt("shippedQuantity"));

		JSONArray jsonArray = jsonObject.getJSONArray("deployments");

		Assertions.assertEquals(1, jsonArray.length());

		JSONObject deploymentJSONObject = jsonArray.getJSONObject(0);

		Assertions.assertEquals(
			_DEPLOYMENT_ID, deploymentJSONObject.getString("id"));
	}

	@Test
	public void testDeployCloudAppRejectsExhaustedQuantity() throws Exception {
		_whenFetchCommerceOrder(
			_createOrder(
				HashMapBuilder.put(
					"cloud-provisioning",
					new JSONArray(
					).put(
						new JSONObject(
						).put(
							"deployments", new JSONArray()
						).put(
							"orderItemId", _ORDER_ITEM_ID
						).put(
							"quantity", 1
						).put(
							"shippedQuantity", 1
						)
					).toString()
				).build(),
				CommerceOrderConstants.ORDER_STATUS_PENDING, "CLOUD_APP",
				CommerceOrderConstants.ORDER_PAYMENT_STATUS_NOT_REQUIRED));

		Assertions.assertThrows(
			IllegalStateException.class,
			() -> _cloudAppService.deployCloudApp(
				_ORDER_ID, _ORDER_ITEM_ID, _PROJECT_ID));

		Mockito.verify(
			_consoleService, Mockito.never()
		).deployApp(
			ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
			ArgumentMatchers.anyString()
		);
	}

	@Test
	public void testDeployCloudAppRollsBackTemporaryDeploymentWhenDeployFails()
		throws Exception {

		Map<String, String> customFields = new HashMap<>();

		_whenFetchCommerceOrder(
			_createOrder(
				customFields, CommerceOrderConstants.ORDER_STATUS_PENDING,
				"CLOUD_APP",
				CommerceOrderConstants.ORDER_PAYMENT_STATUS_NOT_REQUIRED));

		Mockito.when(
			_consoleService.deployApp(
				_EMAIL_ADDRESS, String.valueOf(_ORDER_ID), _PROJECT_ID)
		).thenThrow(
			new IllegalStateException("Unable to deploy app")
		);

		Assertions.assertThrows(
			IllegalStateException.class,
			() -> _cloudAppService.deployCloudApp(
				_ORDER_ID, _ORDER_ITEM_ID, _PROJECT_ID));

		JSONObject jsonObject = _getCloudProvisioningJSONObject(customFields);

		Assertions.assertEquals(0, jsonObject.getInt("shippedQuantity"));

		JSONArray jsonArray = jsonObject.getJSONArray("deployments");

		Assertions.assertEquals(0, jsonArray.length());
	}

	@Test
	public void testUninstallCloudAppRemovesDeployment() throws Exception {
		Map<String, String> customFields = HashMapBuilder.put(
			"cloud-provisioning",
			new JSONArray(
			).put(
				new JSONObject(
				).put(
					"deployments",
					new JSONArray(
					).put(
						new JSONObject(
						).put(
							"id", _DEPLOYMENT_ID
						).put(
							"projectId", _PROJECT_ID
						)
					)
				).put(
					"orderItemId", _ORDER_ITEM_ID
				).put(
					"quantity", 1
				).put(
					"shippedQuantity", 1
				)
			).toString()
		).build();

		_whenFetchCommerceOrder(
			_createOrder(
				customFields, CommerceOrderConstants.ORDER_STATUS_COMPLETED,
				"CLOUD_APP",
				CommerceOrderConstants.ORDER_PAYMENT_STATUS_NOT_REQUIRED));

		_cloudAppService.uninstallCloudApp(
			_DEPLOYMENT_ID, _ORDER_ID, _ORDER_ITEM_ID);

		Mockito.verify(
			_consoleService
		).uninstallApp(
			_ORDER_ID
		);

		JSONObject jsonObject = _getCloudProvisioningJSONObject(customFields);

		Assertions.assertEquals(0, jsonObject.getInt("shippedQuantity"));

		JSONArray jsonArray = jsonObject.getJSONArray("deployments");

		Assertions.assertEquals(0, jsonArray.length());
	}

	private Order _createOrder(
		Map<String, String> customFields, int orderStatus,
		String orderTypeExternalReferenceCode, int paymentStatus) {

		Order order = new Order();

		OrderItem orderItem = new OrderItem();

		orderItem.setId(_ORDER_ITEM_ID);
		orderItem.setQuantity(BigDecimal.ONE);
		orderItem.setSku("PRDCT-CLOUD-APP");

		order.setCreatorEmailAddress(_EMAIL_ADDRESS);
		order.setCustomFields(customFields);
		order.setOrderItems(new OrderItem[] {orderItem});
		order.setOrderStatus(orderStatus);
		order.setOrderTypeExternalReferenceCode(orderTypeExternalReferenceCode);
		order.setPaymentStatus(paymentStatus);

		return order;
	}

	private JSONObject _getCloudProvisioningJSONObject(
		Map<String, String> customFields) {

		JSONArray jsonArray = new JSONArray(
			customFields.get("cloud-provisioning"));

		return jsonArray.getJSONObject(0);
	}

	private void _whenFetchCommerceOrder(Order order) throws Exception {
		Mockito.when(
			_commerceOrderService.fetchCommerceOrder(_ORDER_ID)
		).thenReturn(
			order
		);
	}

	private static final String _DEPLOYMENT_ID = "mock-deployment-1";

	private static final String _EMAIL_ADDRESS = "buyer@example.com";

	private static final long _ORDER_ID = 1000L;

	private static final long _ORDER_ITEM_ID = 2000L;

	private static final String _PROJECT_ID = "omnitest-prd";

	private CloudAppService _cloudAppService;
	private CommerceOrderService _commerceOrderService;
	private ConsoleService _consoleService;

}