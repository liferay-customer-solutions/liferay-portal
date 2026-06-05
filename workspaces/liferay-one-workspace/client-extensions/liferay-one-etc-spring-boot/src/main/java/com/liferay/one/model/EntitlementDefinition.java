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

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Felipe Veloso
 */
public class EntitlementDefinition {

	public EntitlementDefinition(JSONObject jsonObject) {
		_active = jsonObject.optBoolean("active");
		_cProductId = jsonObject.optLong(
			"r_commerceProductToEntitlementDefinition_CProductId");
		_defaultQuantity = jsonObject.optDoubleObject("defaultQuantity", null);
		_displayName = jsonObject.optString("displayName");
		_entitlementDefinitionId = jsonObject.getLong("id");
		_externalReferenceCode = jsonObject.optString("externalReferenceCode");
		_grantType = jsonObject.optString("grantType");
		_name = jsonObject.optString("name");
		_productOptions = _getProductOptions(jsonObject);
		_unit = jsonObject.optString("unit");
		_usageDefinitionId = jsonObject.optLong("usageDefinitionId");
	}

	public long getCProductId() {
		return _cProductId;
	}

	public Double getDefaultQuantity() {
		return _defaultQuantity;
	}

	public String getDisplayName() {
		return _displayName;
	}

	public long getEntitlementDefinitionId() {
		return _entitlementDefinitionId;
	}

	public String getExternalReferenceCode() {
		return _externalReferenceCode;
	}

	public String getGrantType() {
		return _grantType;
	}

	public String getName() {
		return _name;
	}

	public Map<String, String> getProductOptions() {
		return _productOptions;
	}

	public String getUnit() {
		return _unit;
	}

	public long getUsageDefinitionId() {
		return _usageDefinitionId;
	}

	public boolean isActive() {
		return _active;
	}

	private Map<String, String> _getProductOptions(JSONObject jsonObject) {
		Map<String, String> productOptions = new HashMap<>();

		String productOptionsJSON = jsonObject.optString("productOptions");

		if (Validator.isNull(productOptionsJSON)) {
			return productOptions;
		}

		try {
			JSONObject productOptionsJSONObject = new JSONObject(
				productOptionsJSON);

			for (String key : productOptionsJSONObject.keySet()) {
				productOptions.put(
					key, productOptionsJSONObject.optString(key));
			}
		}
		catch (JSONException jsonException) {
			_log.error(jsonException, jsonException);
		}

		return productOptions;
	}

	private static final Log _log = LogFactory.getLog(
		EntitlementDefinition.class);

	private final boolean _active;
	private final long _cProductId;
	private final Double _defaultQuantity;
	private final String _displayName;
	private final long _entitlementDefinitionId;
	private final String _externalReferenceCode;
	private final String _grantType;
	private final String _name;
	private final Map<String, String> _productOptions;
	private final String _unit;
	private final long _usageDefinitionId;

}