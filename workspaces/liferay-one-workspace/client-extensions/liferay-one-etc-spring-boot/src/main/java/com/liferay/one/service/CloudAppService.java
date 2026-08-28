/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.commerce.admin.order.client.dto.v1_0.Order;
import com.liferay.one.constants.CommerceOrderConstants;
import com.liferay.one.util.CloudProvisioningUtil;
import com.liferay.one.util.KeyedLock;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.Validator;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

	public void completeCloudAppOrder(long orderId) throws Exception {
		_keyedLock.withLock(
			"cloud-provisioning#" + orderId,
			() -> {
				Order order = _commerceOrderService.fetchCommerceOrder(orderId);

				if (order == null) {
					return;
				}

				if (!Objects.equals(
						order.getOrderTypeExternalReferenceCode(),
						"CLOUD_APP")) {

					if (_log.isInfoEnabled()) {
						_log.info(
							StringBundler.concat(
								"Unable to complete order ", orderId,
								" because it is not a cloud app order"));
					}

					return;
				}

				if (Objects.equals(
						order.getOrderStatus(),
						CommerceOrderConstants.ORDER_STATUS_COMPLETED)) {

					return;
				}

				Integer paymentStatus = _getSettledPaymentStatus(order);

				if (paymentStatus == null) {
					if (_log.isInfoEnabled()) {
						_log.info(
							StringBundler.concat(
								"Unable to complete order ", orderId,
								" because its payment is still pending"));
					}

					return;
				}

				_commerceOrderService.completeOrder(orderId, paymentStatus);
			});
	}

	public void deployCloudApp(long orderId, long orderItemId, String projectId)
		throws Exception {

		_keyedLock.withLock(
			"cloud-provisioning#" + orderId,
			() -> {
				Order order = _getOrder(orderId);

				Map<String, String> customFields = _getCustomFields(order);

				JSONArray cloudProvisioningJSONArray =
					_getCloudProvisioningJSONArray(customFields, order);

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

				_commerceOrderService.patchOrderCustomFields(
					orderId, customFields);

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
				finally {
					CloudProvisioningUtil.deleteDeployment(
						temporaryDeploymentId, cloudProvisioningJSONObject);

					customFields.put(
						"cloud-provisioning",
						cloudProvisioningJSONArray.toString());

					_commerceOrderService.patchOrderCustomFields(
						orderId, customFields);
				}
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

				JSONArray cloudProvisioningJSONArray =
					_getCloudProvisioningJSONArray(customFields, order);

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

				_commerceOrderService.patchOrderCustomFields(
					orderId, customFields);
			});
	}

	private JSONArray _getCloudProvisioningJSONArray(
		Map<String, String> customFields, Order order) {

		String cloudProvisioningJSON = customFields.get("cloud-provisioning");

		if (Validator.isNotNull(cloudProvisioningJSON)) {
			return new JSONArray(cloudProvisioningJSON);
		}

		return CloudProvisioningUtil.createCloudProvisioningJSONArray(
			order.getOrderItems());
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

	private static final Log _log = LogFactory.getLog(CloudAppService.class);

	@Autowired
	private CommerceOrderService _commerceOrderService;

	@Autowired
	private ConsoleService _consoleService;

	@Autowired
	private KeyedLock _keyedLock;

}