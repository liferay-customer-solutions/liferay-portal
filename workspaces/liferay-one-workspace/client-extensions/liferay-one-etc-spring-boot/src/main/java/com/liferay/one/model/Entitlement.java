/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.model;

import org.json.JSONObject;

/**
 * @author Felipe Veloso
 */
public class Entitlement {

	public Entitlement(JSONObject jsonObject) {
		_commerceOrderItemId = jsonObject.optLong(
			"r_commerceOrderItemToEntitlement_commerceOrderItemId");
		_contractId = jsonObject.optLong(
			"r_contractToEntitlement_c_contractId");
		_endDate = jsonObject.optString("endDate");
		_entitlementDefinitionId = jsonObject.optLong(
			"r_entitlementDefinitionToEntitlement_c_entitlementDefinitionId");
		_entitlementId = jsonObject.getLong("id");
		_grantType = jsonObject.optString("grantType");
		_maxQuantity = jsonObject.optDoubleObject("maxQuantity", null);
		_name = jsonObject.optString("name");
		_quantity = jsonObject.optDoubleObject("quantity", null);
		_startDate = jsonObject.optString("startDate");
	}

	public long getCommerceOrderItemId() {
		return _commerceOrderItemId;
	}

	public long getContractId() {
		return _contractId;
	}

	public String getEndDate() {
		return _endDate;
	}

	public long getEntitlementDefinitionId() {
		return _entitlementDefinitionId;
	}

	public long getEntitlementId() {
		return _entitlementId;
	}

	public String getGrantType() {
		return _grantType;
	}

	public Double getMaxQuantity() {
		return _maxQuantity;
	}

	public String getName() {
		return _name;
	}

	public Double getQuantity() {
		return _quantity;
	}

	public String getStartDate() {
		return _startDate;
	}

	private final long _commerceOrderItemId;
	private final long _contractId;
	private final String _endDate;
	private final long _entitlementDefinitionId;
	private final long _entitlementId;
	private final String _grantType;
	private final Double _maxQuantity;
	private final String _name;
	private final Double _quantity;
	private final String _startDate;

}