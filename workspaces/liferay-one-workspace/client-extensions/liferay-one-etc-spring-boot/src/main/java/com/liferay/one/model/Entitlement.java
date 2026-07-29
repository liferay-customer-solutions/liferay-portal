/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.model;

import com.liferay.portal.kernel.util.Validator;

import java.time.Instant;

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
		_entitlementDefinitionId = jsonObject.optLong(
			"r_entitlementDefinitionToEntitlement_c_entitlementDefinitionId");
		_entitlementId = jsonObject.getLong("id");
		_grantType = jsonObject.optString("grantType");
		_maxQuantity = jsonObject.optDoubleObject("maxQuantity", null);
		_name = jsonObject.optString("name");
		_projectExternalReferenceCode = jsonObject.optString(
			"r_projectToEntitlement_c_projectERC");
		_quantity = jsonObject.optDoubleObject("quantity", null);

		String endDate = jsonObject.optString("endDate");

		if (Validator.isNull(endDate)) {
			_endDateInstant = null;
		}
		else {
			_endDateInstant = Instant.parse(endDate);
		}

		String startDate = jsonObject.optString("startDate");

		if (Validator.isNull(startDate)) {
			_startDateInstant = null;
		}
		else {
			_startDateInstant = Instant.parse(startDate);
		}
	}

	public long getCommerceOrderItemId() {
		return _commerceOrderItemId;
	}

	public long getContractId() {
		return _contractId;
	}

	public Instant getEndDateInstant() {
		return _endDateInstant;
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

	public String getProjectExternalReferenceCode() {
		return _projectExternalReferenceCode;
	}

	public Double getQuantity() {
		return _quantity;
	}

	public Instant getStartDateInstant() {
		return _startDateInstant;
	}

	public boolean isExpired() {
		if (_endDateInstant == null) {
			return false;
		}

		return _endDateInstant.isBefore(Instant.now());
	}

	private final long _commerceOrderItemId;
	private final long _contractId;
	private final Instant _endDateInstant;
	private final long _entitlementDefinitionId;
	private final long _entitlementId;
	private final String _grantType;
	private final Double _maxQuantity;
	private final String _name;
	private final String _projectExternalReferenceCode;
	private final Double _quantity;
	private final Instant _startDateInstant;

}