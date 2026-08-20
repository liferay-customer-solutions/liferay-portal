/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.commerce.admin.order.client.dto.v1_0.Order;
import com.liferay.one.util.CloudProvisioningUtil;
import com.liferay.one.util.KeyedLock;
import com.liferay.petra.string.StringBundler;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Keven Leone
 * @author Ricardo Mariz
 */
@Component
public class CloudAppService {

	public void deployCloudApp(long orderId, long orderItemId, String projectId)
		throws Exception {

		_keyedLock.withLock(
			"cloud-provisioning#" + orderId,
			() -> {
				Order order = _getOrder(orderId);

				Map<String, String> customFields = _getCustomFields(order);

				JSONArray cloudProvisioningJSONArray = null;

				String cloudProvisioningJSON = customFields.get(
					"cloud-provisioning");

				if (cloudProvisioningJSON != null) {
					cloudProvisioningJSONArray = new JSONArray(
						cloudProvisioningJSON);
				}
				else {
					cloudProvisioningJSONArray =
						CloudProvisioningUtil.createCloudProvisioningJSONArray(
							order.getOrderItems());
				}

				JSONObject cloudProvisioningJSONObject = _getJSONObject(
					cloudProvisioningJSONArray, orderId, orderItemId);

				if (cloudProvisioningJSONObject.getLong("shippedQuantity") >=
						cloudProvisioningJSONObject.getLong("quantity")) {

					throw new IllegalStateException(
						"Unable to install app for order item " + orderItemId +
							" because there are no available resources");
				}

				String temporaryDeploymentId =
					CloudProvisioningUtil.createTemporaryDeployment(
						customFields, cloudProvisioningJSONArray,
						cloudProvisioningJSONObject, projectId);

				_commerceOrderService.updateOrder(
					customFields, orderId, order.getOrderStatus());

				try {
					JSONObject appJSONObject = _consoleService.deployApp(
						order.getCreatorEmailAddress(), String.valueOf(orderId),
						projectId);

					cloudProvisioningJSONObject.put(
						"deployments",
						cloudProvisioningJSONObject.getJSONArray(
							"deployments"
						).put(
							appJSONObject
						)
					).put(
						"shippedQuantity",
						cloudProvisioningJSONObject.getInt("shippedQuantity") +
							1
					);
				}
				catch (Exception exception) {
					_log.error(
						"Unable to install app for order " + orderId,
						exception);
				}

				CloudProvisioningUtil.deleteDeployment(
					temporaryDeploymentId, cloudProvisioningJSONObject);

				customFields.put(
					"cloud-provisioning",
					cloudProvisioningJSONArray.toString());

				_commerceOrderService.updateOrder(
					customFields, orderId, order.getOrderStatus());
			});
	}

	public void uninstallCloudApp(
			String deploymentId, long orderId, long orderItemId)
		throws Exception {

		_keyedLock.withLock(
			"cloud-provisioning#" + orderId,
			() -> {
				Order order = _getOrder(orderId);

				Map<String, String> customFields = _getCustomFields(order);

				JSONArray cloudProvisioningJSONArray = new JSONArray(
					customFields.get("cloud-provisioning"));

				JSONObject cloudProvisioningJSONObject = _getJSONObject(
					cloudProvisioningJSONArray, orderId, orderItemId);

				_consoleService.uninstallApp(orderId);

				CloudProvisioningUtil.deleteDeployment(
					deploymentId, cloudProvisioningJSONObject);

				cloudProvisioningJSONObject.put(
					"shippedQuantity",
					cloudProvisioningJSONObject.getJSONArray(
						"deployments"
					).length());

				customFields.put(
					"cloud-provisioning",
					cloudProvisioningJSONArray.toString());

				_commerceOrderService.updateOrder(
					customFields, orderId, order.getOrderStatus());
			});
	}

	private Map<String, String> _getCustomFields(Order order) {
		Map<String, String> customFields =
			(Map<String, String>)order.getCustomFields();

		if (customFields == null) {
			return new HashMap<>();
		}

		return customFields;
	}

	private JSONObject _getJSONObject(
		JSONArray cloudProvisioningJSONArray, long orderId, long orderItemId) {

		JSONObject cloudProvisioningJSONObject =
			CloudProvisioningUtil.getCloudProvisioningJSONObject(
				cloudProvisioningJSONArray, orderItemId);

		if (cloudProvisioningJSONObject.isEmpty()) {
			throw new IllegalArgumentException(
				StringBundler.concat(
					"No order item ", orderItemId, " exists on order ",
					orderId));
		}

		return cloudProvisioningJSONObject;
	}

	private Order _getOrder(long orderId) throws Exception {
		Order order = _commerceOrderService.fetchCommerceOrder(orderId);

		if (order == null) {
			throw new IllegalArgumentException(
				"No order exists with ID " + orderId);
		}

		return order;
	}

	private static final Log _log = LogFactory.getLog(CloudAppService.class);

	@Autowired
	private CommerceOrderService _commerceOrderService;

	@Autowired
	private ConsoleService _consoleService;

	@Autowired
	private KeyedLock _keyedLock;

}