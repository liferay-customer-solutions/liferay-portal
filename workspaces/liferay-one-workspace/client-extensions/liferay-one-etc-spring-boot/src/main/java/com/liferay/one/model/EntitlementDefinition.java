/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.model;

import org.json.JSONObject;

/**
 * @author Felipe Veloso
 */
public class EntitlementDefinition {

	public EntitlementDefinition(JSONObject jsonObject) {
		_active = jsonObject.optBoolean("entitlementDefinitionActive");
		_cProductId = jsonObject.optLong(
			"r_commerceProductToEntitlementDefinition_CProductId");
		_defaultQuantity = jsonObject.optDoubleObject("defaultQuantity", null);
		_displayName = jsonObject.optString("displayName");
		_entitlementDefinitionId = jsonObject.getLong("id");
		_externalReferenceCode = jsonObject.optString("externalReferenceCode");
		_grantType = jsonObject.optString("grantType");
		_machineType = jsonObject.optString("machineType", null);
		_name = jsonObject.optString("name");
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

	public String getMachineType() {
		return _machineType;
	}

	public String getName() {
		return _name;
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

	private final boolean _active;
	private final long _cProductId;
	private final Double _defaultQuantity;
	private final String _displayName;
	private final long _entitlementDefinitionId;
	private final String _externalReferenceCode;
	private final String _grantType;
	private final String _machineType;
	private final String _name;
	private final String _unit;
	private final long _usageDefinitionId;

}