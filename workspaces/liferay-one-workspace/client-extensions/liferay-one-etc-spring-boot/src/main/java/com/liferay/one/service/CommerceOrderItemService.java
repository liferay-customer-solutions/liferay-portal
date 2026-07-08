/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.commerce.admin.order.client.custom.field.CustomField;
import com.liferay.headless.commerce.admin.order.client.custom.field.CustomValue;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.Order;
import com.liferay.headless.commerce.admin.order.client.problem.Problem;
import com.liferay.headless.commerce.admin.order.client.resource.v1_0.OrderItemResource;
import com.liferay.one.model.OrderItem;
import com.liferay.one.salesforce.model.OpportunityLineItem;
import com.liferay.portal.kernel.util.Validator;

import java.math.BigDecimal;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
			com.liferay.headless.commerce.admin.order.client.dto.v1_0.OrderItem
				orderItem = orderItemResource.getOrderItem(commerceOrderItemId);

			return new OrderItem(orderItem);
		}
		catch (Problem.ProblemException problemException) {
			Problem problem = problemException.getProblem();

			if ((problem != null) && isNotFound(problem.getStatus())) {
				return null;
			}

			throw problemException;
		}
	}

	public void patchOrderItem(
			Long orderItemId,
			com.liferay.headless.commerce.admin.order.client.dto.v1_0.OrderItem
				orderItem)
		throws Exception {

		OrderItemResource orderItemResource = _buildOrderItemResource();

		orderItemResource.patchOrderItem(orderItemId, orderItem);
	}

	public void patchOrderItemCustomFields(
			long commerceOrderItemId, Map<String, Object> customFieldValues)
		throws Exception {

		OrderItemResource orderItemResource = _buildOrderItemResource();

		com.liferay.headless.commerce.admin.order.client.dto.v1_0.OrderItem
			existingOrderItem = orderItemResource.getOrderItem(
				commerceOrderItemId);

		com.liferay.headless.commerce.admin.order.client.dto.v1_0.OrderItem
			orderItem =
				new com.liferay.headless.commerce.admin.order.client.dto.v1_0.
					OrderItem();

		CustomField[] customFields = _toCustomFields(customFieldValues);

		orderItem.setCustomFields(() -> customFields);

		orderItem.setExternalReferenceCode(
			existingOrderItem::getExternalReferenceCode);

		patchOrderItem(commerceOrderItemId, orderItem);
	}

	public com.liferay.headless.commerce.admin.order.client.dto.v1_0.OrderItem
			upsertOrderItem(
				Order order, OpportunityLineItem opportunityLineItem,
				String stageName)
		throws Exception {

		com.liferay.headless.commerce.admin.order.client.dto.v1_0.OrderItem
			orderItem =
				new com.liferay.headless.commerce.admin.order.client.dto.v1_0.
					OrderItem();

		orderItem.setExternalReferenceCode(opportunityLineItem::getId);
		orderItem.setSkuExternalReferenceCode(
			opportunityLineItem::getProduct2Id);

		if (Validator.isNotNull(opportunityLineItem.getProductName())) {
			Map<String, String> name = Map.of(
				"en_US", opportunityLineItem.getProductName());

			orderItem.setName(() -> name);
		}

		if (opportunityLineItem.getQuantity() != null) {
			BigDecimal quantity = BigDecimal.valueOf(
				opportunityLineItem.getQuantity());

			orderItem.setQuantity(() -> quantity);
		}

		if (opportunityLineItem.getUnitPrice() != null) {
			BigDecimal unitPrice = BigDecimal.valueOf(
				opportunityLineItem.getUnitPrice());

			orderItem.setUnitPrice(() -> unitPrice);
		}

		if (opportunityLineItem.getTotalPrice() != null) {
			BigDecimal finalPrice = BigDecimal.valueOf(
				opportunityLineItem.getTotalPrice());

			orderItem.setFinalPrice(() -> finalPrice);
		}

		CustomField[] customFields = _toCustomFields(
			_getCustomFieldValues(opportunityLineItem, stageName));

		orderItem.setCustomFields(() -> customFields);

		com.liferay.headless.commerce.admin.order.client.dto.v1_0.OrderItem
			existingOrderItem = _getExistingOrderItem(
				order, opportunityLineItem.getId());

		OrderItemResource orderItemResource = _buildOrderItemResource();

		if (existingOrderItem != null) {
			return orderItemResource.patchOrderItem(
				existingOrderItem.getId(), orderItem);
		}

		return orderItemResource.postOrderIdOrderItem(order.getId(), orderItem);
	}

	private String _addDays(String dateTime, int days) {
		Matcher matcher = _dateTimePattern.matcher(dateTime);

		if (!matcher.matches()) {
			return null;
		}

		LocalDate localDate = LocalDate.parse(matcher.group(1));

		return _dateFormatter.format(localDate.plusDays(days)) + "T00:00:00Z";
	}

	private OrderItemResource _buildOrderItemResource() {
		return OrderItemResource.builder(
		).endpoint(
			lxcDXPMainDomain, lxcDXPServerProtocol
		).header(
			HttpHeaders.AUTHORIZATION, getAuthorization()
		).parameters(
			"nestedFields", "customFields"
		).build();
	}

	private Map<String, Object> _getCustomFieldValues(
		OpportunityLineItem opportunityLineItem, String stageName) {

		Map<String, Object> customFieldValues = new HashMap<>();

		if (Validator.isNotNull(opportunityLineItem.getCloudRegion())) {
			customFieldValues.put(
				"cloudRegion", opportunityLineItem.getCloudRegion());
		}

		customFieldValues.put("customStatus", _getCustomStatus(stageName));

		String endDate = _toDateTime(opportunityLineItem.getEndDate());

		if (Validator.isNotNull(endDate)) {
			customFieldValues.put("effectiveEndDate", _addDays(endDate, 30));
			customFieldValues.put("endDate", endDate);
		}

		if (Validator.isNotNull(opportunityLineItem.getMachineType())) {
			customFieldValues.put(
				"machineType", opportunityLineItem.getMachineType());
		}

		if (Validator.isNotNull(opportunityLineItem.getProductType())) {
			customFieldValues.put(
				"orderType", opportunityLineItem.getProductType());
		}

		if (opportunityLineItem.getNumberOfPods() != null) {
			customFieldValues.put(
				"sizing", opportunityLineItem.getNumberOfPods());
		}

		String startDate = _toDateTime(opportunityLineItem.getServiceDate());

		if (Validator.isNotNull(startDate)) {
			customFieldValues.put("startDate", startDate);
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

	private com.liferay.headless.commerce.admin.order.client.dto.v1_0.OrderItem
		_getExistingOrderItem(Order order, String externalReferenceCode) {

		com.liferay.headless.commerce.admin.order.client.dto.v1_0.OrderItem[]
			orderItems = order.getOrderItems();

		if (orderItems == null) {
			return null;
		}

		for (com.liferay.headless.commerce.admin.order.client.dto.v1_0.OrderItem
				orderItem : orderItems) {

			if (Objects.equals(
					orderItem.getExternalReferenceCode(),
					externalReferenceCode)) {

				return orderItem;
			}
		}

		return null;
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

	private String _toDateTime(String value) {
		if (Validator.isNull(value)) {
			return null;
		}

		Matcher matcher = _datePattern.matcher(value);

		if (matcher.matches()) {
			return value + "T00:00:00Z";
		}

		return value;
	}

	private static final Log _log = LogFactory.getLog(
		CommerceOrderItemService.class);

	private static final DateTimeFormatter _dateFormatter =
		DateTimeFormatter.ISO_LOCAL_DATE;
	private static final Pattern _datePattern = Pattern.compile(
		"\\d{4}-\\d{2}-\\d{2}");
	private static final Pattern _dateTimePattern = Pattern.compile(
		"(\\d{4}-\\d{2}-\\d{2})T.*");
	private static final Map<String, String> _stageNameCustomStatuses = Map.of(
		"Closed", "Canceled", "Closed Lost", "Canceled", "Closed Won",
		"Approved", "Disqualified", "Canceled", "Rejected", "Canceled");

}