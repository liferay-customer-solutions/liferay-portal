/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.model;

import com.liferay.headless.commerce.admin.order.client.custom.field.CustomField;
import com.liferay.headless.commerce.admin.order.client.custom.field.CustomValue;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Felipe Veloso
 */
public class OrderItem {

	public OrderItem(
		com.liferay.headless.commerce.admin.order.client.dto.v1_0.OrderItem
			orderItem) {

		_cProductId = GetterUtil.getLong(orderItem.getProductId());
		_commerceOrderItemId = GetterUtil.getLong(orderItem.getId());
		_name = _getName(orderItem.getName());
		_orderId = GetterUtil.getLong(orderItem.getOrderId());
		_productOptions = _getProductOptions(orderItem.getOptions());

		Map<String, Object> customFieldValues = _getCustomFieldValues(
			orderItem);

		_effectiveEndDate = GetterUtil.getString(
			customFieldValues.get("effectiveEndDate"));
		_endDate = GetterUtil.getString(customFieldValues.get("endDate"));
		_sizing = GetterUtil.getInteger(customFieldValues.get("sizing"));
		_startDate = GetterUtil.getString(customFieldValues.get("startDate"));
		_status = GetterUtil.getString(customFieldValues.get("customStatus"));
	}

	public long getCommerceOrderItemId() {
		return _commerceOrderItemId;
	}

	public long getCProductId() {
		return _cProductId;
	}

	public String getEffectiveEndDate() {
		return _effectiveEndDate;
	}

	public String getEndDate() {
		return _endDate;
	}

	public String getName() {
		return _name;
	}

	public long getOrderId() {
		return _orderId;
	}

	public Map<String, String> getProductOptions() {
		return _productOptions;
	}

	public int getSizing() {
		return _sizing;
	}

	public String getStartDate() {
		return _startDate;
	}

	public String getStatus() {
		return _status;
	}

	private Map<String, Object> _getCustomFieldValues(
		com.liferay.headless.commerce.admin.order.client.dto.v1_0.OrderItem
			orderItem) {

		Map<String, Object> customFieldValues = new HashMap<>();

		CustomField[] customFields = orderItem.getCustomFields();

		if (customFields == null) {
			return customFieldValues;
		}

		for (CustomField customField : customFields) {
			CustomValue customValue = customField.getCustomValue();

			if (customValue == null) {
				continue;
			}

			customFieldValues.put(customField.getName(), customValue.getData());
		}

		return customFieldValues;
	}

	private String _getName(Map<String, String> nameMap) {
		if (nameMap == null) {
			return null;
		}

		return nameMap.get("en_US");
	}

	private String _getOptionValue(JSONObject optionJSONObject) {
		JSONArray valueJSONArray = optionJSONObject.optJSONArray("value");

		if (valueJSONArray != null) {
			if (valueJSONArray.isEmpty()) {
				return null;
			}

			return valueJSONArray.getString(0);
		}

		return optionJSONObject.optString("value", null);
	}

	private Map<String, String> _getProductOptions(String optionsJSON) {
		Map<String, String> productOptions = new HashMap<>();

		if (Validator.isNull(optionsJSON)) {
			return productOptions;
		}

		try {
			JSONArray optionsJSONArray = new JSONArray(optionsJSON);

			for (int i = 0; i < optionsJSONArray.length(); i++) {
				JSONObject optionJSONObject = optionsJSONArray.getJSONObject(i);

				String value = _getOptionValue(optionJSONObject);

				if (value == null) {
					continue;
				}

				productOptions.put(optionJSONObject.optString("key"), value);
			}
		}
		catch (JSONException jsonException) {
			_log.error(jsonException);
		}

		return productOptions;
	}

	private static final Log _log = LogFactory.getLog(OrderItem.class);

	private final long _commerceOrderItemId;
	private final long _cProductId;
	private final String _effectiveEndDate;
	private final String _endDate;
	private final String _name;
	private final long _orderId;
	private final Map<String, String> _productOptions;
	private final int _sizing;
	private final String _startDate;
	private final String _status;

}