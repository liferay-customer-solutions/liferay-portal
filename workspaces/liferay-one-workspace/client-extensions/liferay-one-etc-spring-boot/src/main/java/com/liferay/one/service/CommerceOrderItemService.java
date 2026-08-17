/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.commerce.admin.order.client.custom.field.CustomField;
import com.liferay.headless.commerce.admin.order.client.custom.field.CustomValue;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.Order;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.OrderItem;
import com.liferay.headless.commerce.admin.order.client.problem.Problem;
import com.liferay.headless.commerce.admin.order.client.resource.v1_0.OrderItemResource;
import com.liferay.one.constants.CommerceOrderItemConstants;
import com.liferay.one.salesforce.model.SalesforceOpportunityLineItem;
import com.liferay.one.util.OrderItemUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.math.BigDecimal;

import java.time.Instant;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * @author Felipe Veloso
 */
@Component
public class CommerceOrderItemService extends OneBaseService {

	public OrderItem fetchCommerceOrderItem(long commerceOrderItemId)
		throws Exception {

		OrderItemResource orderItemResource = _buildOrderItemResource();

		try {
			return orderItemResource.getOrderItem(commerceOrderItemId);
		}
		catch (Problem.ProblemException problemException) {
			Problem problem = problemException.getProblem();

			if ((problem != null) && isNotFound(problem.getStatus())) {
				return null;
			}

			throw problemException;
		}
	}

	public void patchOrderItem(Long orderItemId, OrderItem orderItem)
		throws Exception {

		OrderItemResource orderItemResource = _buildOrderItemResource();

		orderItemResource.patchOrderItem(orderItemId, orderItem);
	}

	public void patchOrderItemCustomFields(
			long commerceOrderItemId, Map<String, Object> customFieldValues)
		throws Exception {

		OrderItemResource orderItemResource = _buildOrderItemResource();

		OrderItem existingOrderItem = orderItemResource.getOrderItem(
			commerceOrderItemId);

		OrderItem orderItem = new OrderItem();

		CustomField[] customFields = _toCustomFields(customFieldValues);

		orderItem.setCustomFields(() -> customFields);

		orderItem.setExternalReferenceCode(
			existingOrderItem::getExternalReferenceCode);

		patchOrderItem(commerceOrderItemId, orderItem);
	}

	public OrderItem upsertOrderItem(
			Order order,
			SalesforceOpportunityLineItem salesforceOpportunityLineItem,
			String stageName)
		throws Exception {

		OrderItem orderItem = new OrderItem();

		orderItem.setExternalReferenceCode(
			salesforceOpportunityLineItem::getId);
		orderItem.setSkuExternalReferenceCode(
			salesforceOpportunityLineItem::getProduct2Id);

		if (Validator.isNotNull(
				salesforceOpportunityLineItem.getProductName())) {

			Map<String, String> name = Map.of(
				"en_US", salesforceOpportunityLineItem.getProductName());

			orderItem.setName(() -> name);
		}

		if (salesforceOpportunityLineItem.getQuantity() != null) {
			BigDecimal quantity = BigDecimal.valueOf(
				salesforceOpportunityLineItem.getQuantity());

			orderItem.setQuantity(() -> quantity);
		}

		if (salesforceOpportunityLineItem.getUnitPrice() != null) {
			BigDecimal unitPrice = BigDecimal.valueOf(
				salesforceOpportunityLineItem.getUnitPrice());

			orderItem.setUnitPrice(() -> unitPrice);
		}

		if (salesforceOpportunityLineItem.getTotalPrice() != null) {
			BigDecimal finalPrice = BigDecimal.valueOf(
				salesforceOpportunityLineItem.getTotalPrice());

			orderItem.setFinalPrice(() -> finalPrice);
		}

		if (Validator.isNotNull(
				salesforceOpportunityLineItem.getMachineType())) {

			String options = OrderItemUtil.toOptionsJSON(
				"machinetype",
				StringUtil.toLowerCase(
					salesforceOpportunityLineItem.getMachineType()));

			orderItem.setOptions(() -> options);
		}

		Map<String, Object> customFieldValues = _getCustomFieldValues(
			salesforceOpportunityLineItem, stageName);

		OrderItem existingOrderItem = OrderItemUtil.fetchOrderItem(
			salesforceOpportunityLineItem.getId(), order);

		if (existingOrderItem != null) {
			if (OrderItemUtil.isCanceled(existingOrderItem)) {
				customFieldValues.remove("customStatus");
			}

			if (!OrderItemUtil.isUpdateEffectiveEndDate(
					existingOrderItem,
					salesforceOpportunityLineItem.getEndDateInstant())) {

				customFieldValues.remove("effectiveEndDate");
			}
		}

		CustomField[] customFields = _toCustomFields(customFieldValues);

		orderItem.setCustomFields(() -> customFields);

		OrderItemResource orderItemResource = _buildOrderItemResource();

		if (existingOrderItem != null) {
			return orderItemResource.patchOrderItem(
				existingOrderItem.getId(), orderItem);
		}

		return orderItemResource.postOrderIdOrderItem(order.getId(), orderItem);
	}

	private OrderItemResource _buildOrderItemResource() {
		return OrderItemResource.builder(
		).endpoint(
			getDXPEndpointAddress(), lxcDXPServerProtocol
		).header(
			HttpHeaders.AUTHORIZATION, getAuthorization()
		).parameters(
			"nestedFields", "customFields"
		).build();
	}

	private Map<String, Object> _getCustomFieldValues(
		SalesforceOpportunityLineItem salesforceOpportunityLineItem,
		String stageName) {

		Map<String, Object> customFieldValues = new HashMap<>();

		if (Validator.isNotNull(
				salesforceOpportunityLineItem.getCloudRegion())) {

			customFieldValues.put(
				"cloudRegion", salesforceOpportunityLineItem.getCloudRegion());
		}

		customFieldValues.put("customStatus", _getCustomStatus(stageName));

		Instant endDateInstant =
			salesforceOpportunityLineItem.getEndDateInstant();

		if (endDateInstant != null) {
			Instant effectiveEndDateInstant = endDateInstant.plus(
				CommerceOrderItemConstants.EFFECTIVE_END_DATE_GRACE);

			customFieldValues.put(
				"effectiveEndDate", effectiveEndDateInstant.toString());

			customFieldValues.put("endDate", endDateInstant.toString());
		}

		if (Validator.isNotNull(
				salesforceOpportunityLineItem.getProductType())) {

			customFieldValues.put(
				"orderType", salesforceOpportunityLineItem.getProductType());
		}

		if (salesforceOpportunityLineItem.getNumberOfPods() != null) {
			customFieldValues.put(
				"sizing", salesforceOpportunityLineItem.getNumberOfPods());
		}

		Instant serviceDateInstant =
			salesforceOpportunityLineItem.getServiceDateInstant();

		if (serviceDateInstant != null) {
			customFieldValues.put("startDate", serviceDateInstant.toString());
		}

		return customFieldValues;
	}

	private String _getCustomStatus(String stageName) {
		String customStatus = _stageNameCustomStatuses.get(stageName);

		if (customStatus != null) {
			return customStatus;
		}

		if (_log.isWarnEnabled()) {
			_log.warn(
				"Unable to map stage name " + stageName +
					" to a custom status");
		}

		return "On Hold";
	}

	private CustomField[] _toCustomFields(Map<String, ?> customFieldValues) {
		CustomField[] customFields = new CustomField[customFieldValues.size()];

		int i = 0;

		for (Map.Entry<String, ?> entry : customFieldValues.entrySet()) {
			CustomField customField = new CustomField();

			customField.setName(entry::getKey);

			CustomValue customValue = new CustomValue();

			customValue.setData(entry::getValue);

			customField.setCustomValue(() -> customValue);

			customFields[i++] = customField;
		}

		return customFields;
	}

	private static final Log _log = LogFactory.getLog(
		CommerceOrderItemService.class);

	private static final Map<String, String> _stageNameCustomStatuses = Map.of(
		"Closed", "Canceled", "Closed Lost", "Canceled", "Closed Won",
		"Approved", "Disqualified", "Canceled", "Rejected", "Canceled");

}