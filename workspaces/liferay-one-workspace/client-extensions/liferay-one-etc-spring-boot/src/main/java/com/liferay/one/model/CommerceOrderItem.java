/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.model;

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
public class CommerceOrderItem extends LiferayObject {

	public CommerceOrderItem(JSONObject jsonObject) {
		super(jsonObject);

		_cProductId = jsonObject.optLong("productId");
		_commerceOrderItemId = jsonObject.getLong("id");
		_endDate = getCustomFieldString("endDate");
		_orderId = jsonObject.optLong("orderId");
		_productOptions = _getProductOptions(jsonObject);
		_sizing = getCustomFieldInteger("sizing");
		_startDate = getCustomFieldString("startDate");
	}

	public long getCommerceOrderItemId() {
		return _commerceOrderItemId;
	}

	public long getCProductId() {
		return _cProductId;
	}

	public String getEndDate() {
		return _endDate;
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

	private Map<String, String> _getProductOptions(JSONObject jsonObject) {
		Map<String, String> productOptions = new HashMap<>();

		String optionsJSON = jsonObject.optString("options");

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
			_log.error(jsonException, jsonException);
		}

		return productOptions;
	}

	private static final Log _log = LogFactory.getLog(CommerceOrderItem.class);

	private final long _commerceOrderItemId;
	private final long _cProductId;
	private final String _endDate;
	private final long _orderId;
	private final Map<String, String> _productOptions;
	private final int _sizing;
	private final String _startDate;

}