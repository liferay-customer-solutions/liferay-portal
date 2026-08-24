/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.model;

import com.liferay.one.constants.EntitlementConstants;
import com.liferay.portal.kernel.util.Validator;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.json.JSONObject;

/**
 * @author Felipe Veloso
 */
public abstract class BaseUsageStrategy {

	public static final String UNIT_GIB = "GiB";

	public static final String UNIT_TIB = "TiB";

	public boolean hasUsage() {
		if ((_usageJSONObject == null) || _usageJSONObject.isEmpty()) {
			return false;
		}

		return true;
	}

	public abstract JSONObject toJSONObject();

	protected BaseUsageStrategy(JSONObject usageJSONObject) {
		_usageJSONObject = usageJSONObject;
	}

	protected BaseUsageStrategy(String response) {
		if (Validator.isNull(response)) {
			_usageJSONObject = null;
		}
		else {
			_usageJSONObject = new JSONObject(response);
		}
	}

	protected JSONObject createCapacityUsageJSONObject(
		BigDecimal gibMaxCount, BigDecimal gibUsedCount) {

		JSONObject jsonObject = new JSONObject();

		if (_isUnlimited(gibMaxCount)) {
			jsonObject.put("maxCount", _QUANTITY_UNLIMITED);
		}
		else {
			jsonObject.put(
				"maxCount", _getPromotedValue(gibMaxCount)
			).put(
				"maxCountUnits", _getPromotedUnit(gibMaxCount)
			);
		}

		if (!hasUsage()) {
			return jsonObject.put("percentage", "0");
		}

		jsonObject.put(
			"usedCount", _getPromotedValue(gibUsedCount)
		).put(
			"usedCountUnits", _getPromotedUnit(gibUsedCount)
		);

		if (_isUnlimited(gibMaxCount)) {
			return jsonObject.put("percentage", "0");
		}

		BigDecimal percentage = _getPercentage(gibMaxCount, gibUsedCount);

		return jsonObject.put("percentage", percentage.toPlainString());
	}

	protected JSONObject createUsageJSONObject(
		BigDecimal maxCount, BigDecimal usedCount) {

		JSONObject jsonObject = new JSONObject();

		if (_isUnlimited(maxCount)) {
			jsonObject.put("maxCount", _QUANTITY_UNLIMITED);
		}
		else {
			jsonObject.put("maxCount", maxCount);
		}

		if (!hasUsage()) {
			return jsonObject.put("percentage", "0");
		}

		jsonObject.put("usedCount", usedCount);

		if (_isUnlimited(maxCount)) {
			return jsonObject.put("percentage", "0");
		}

		BigDecimal percentage = _getPercentage(maxCount, usedCount);

		return jsonObject.put("percentage", percentage.toPlainString());
	}

	protected BigDecimal getMaxCount(
		BigDecimal maxCount, Entitlement entitlement) {

		if (_isUnlimited(maxCount)) {
			return maxCount;
		}

		String grantType = entitlement.getGrantType();

		if (Validator.isNotNull(grantType) &&
			grantType.equals(EntitlementConstants.GRANT_TYPE_UNLIMITED)) {

			return BigDecimal.valueOf(_QUANTITY_UNLIMITED);
		}

		Double quantity = entitlement.getQuantity();

		if (quantity == null) {
			return maxCount;
		}

		return maxCount.add(
			_toGibibytes(BigDecimal.valueOf(quantity), _getUnit(entitlement)));
	}

	protected JSONObject getUsageJSONObject() {
		return _usageJSONObject;
	}

	private BigDecimal _getPercentage(
		BigDecimal maxCount, BigDecimal usedCount) {

		if ((maxCount == null) || (maxCount.signum() <= 0)) {
			return BigDecimal.ZERO;
		}

		return usedCount.multiply(
			_ONE_HUNDRED
		).divide(
			maxCount, 2, RoundingMode.HALF_UP
		).setScale(
			4, RoundingMode.DOWN
		);
	}

	private String _getPromotedUnit(BigDecimal gibValue) {
		if (_isTebibyteScale(gibValue)) {
			return UNIT_TIB;
		}

		return UNIT_GIB;
	}

	private BigDecimal _getPromotedValue(BigDecimal gibValue) {
		if (_isTebibyteScale(gibValue)) {
			return gibValue.divide(
				_GIBIBYTES_PER_TEBIBYTE, 2, RoundingMode.HALF_UP);
		}

		return gibValue;
	}

	private String _getUnit(Entitlement entitlement) {
		EntitlementDefinition entitlementDefinition =
			entitlement.getEntitlementDefinition();

		if (entitlementDefinition == null) {
			return null;
		}

		return entitlementDefinition.getUnit();
	}

	private boolean _isTebibyteScale(BigDecimal gibValue) {
		if (gibValue == null) {
			return false;
		}

		if (gibValue.compareTo(_GIBIBYTES_PER_TEBIBYTE) >= 0) {
			return true;
		}

		return false;
	}

	private boolean _isUnlimited(BigDecimal maxCount) {
		if (maxCount == null) {
			return false;
		}

		if (maxCount.signum() < 0) {
			return true;
		}

		return false;
	}

	private BigDecimal _toGibibytes(BigDecimal quantity, String unit) {
		if (quantity == null) {
			return BigDecimal.ZERO;
		}

		if (Validator.isNotNull(unit) && unit.equals(UNIT_TIB)) {
			return quantity.multiply(_GIBIBYTES_PER_TEBIBYTE);
		}

		return quantity;
	}

	private static final BigDecimal _GIBIBYTES_PER_TEBIBYTE = new BigDecimal(
		1024);

	private static final BigDecimal _ONE_HUNDRED = new BigDecimal(100);

	private static final long _QUANTITY_UNLIMITED = -1;

	private final JSONObject _usageJSONObject;

}