/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.model;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author Felipe Veloso
 */
public class CommerceOrderItem {

	public CommerceOrderItem(JSONObject jsonObject) {
		_cProductId = jsonObject.optLong("productId");
		_commerceOrderItemId = jsonObject.getLong("id");
		_endDate = _getCustomValueJSONObject(
			jsonObject, "endDate"
		).optString(
			"data", null
		);
		_machineType = _getCustomValueJSONObject(
			jsonObject, "machineType"
		).optString(
			"data", null
		);
		_orderId = jsonObject.optLong("orderId");
		_sizing = _getCustomValueJSONObject(
			jsonObject, "sizing"
		).optDoubleObject(
			"data", null
		);
		_startDate = _getCustomValueJSONObject(
			jsonObject, "startDate"
		).optString(
			"data", null
		);
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

	public String getMachineType() {
		return _machineType;
	}

	public long getOrderId() {
		return _orderId;
	}

	public Double getSizing() {
		return _sizing;
	}

	public String getStartDate() {
		return _startDate;
	}

	private JSONObject _getCustomValueJSONObject(
		JSONObject jsonObject, String name) {

		JSONArray customFieldsJSONArray = jsonObject.optJSONArray(
			"customFields");

		if (customFieldsJSONArray == null) {
			return new JSONObject();
		}

		for (int i = 0; i < customFieldsJSONArray.length(); i++) {
			JSONObject customFieldJSONObject =
				customFieldsJSONArray.getJSONObject(i);

			if (!name.equals(customFieldJSONObject.optString("name"))) {
				continue;
			}

			JSONObject customValueJSONObject =
				customFieldJSONObject.optJSONObject("customValue");

			if (customValueJSONObject == null) {
				return new JSONObject();
			}

			return customValueJSONObject;
		}

		return new JSONObject();
	}

	private final long _commerceOrderItemId;
	private final long _cProductId;
	private final String _endDate;
	private final String _machineType;
	private final long _orderId;
	private final Double _sizing;
	private final String _startDate;

}