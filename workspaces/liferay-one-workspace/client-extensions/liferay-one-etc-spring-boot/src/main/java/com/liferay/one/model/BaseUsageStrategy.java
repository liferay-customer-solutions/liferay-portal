/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.model;

import com.liferay.one.constants.EntitlementConstants;
import com.liferay.portal.kernel.util.Validator;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	protected JSONObject createCapacityUsageJSONObject(
		BigDecimal gibMaxCount, BigDecimal gibUsedCount) {

		if (_isUnlimited(gibMaxCount)) {
			return new JSONObject(
			).put(
				"maxCount", _QUANTITY_UNLIMITED
			).put(
				"percentage", "0"
			).put(
				"usedCount", _getPromotedValue(gibUsedCount)
			).put(
				"usedCountUnits", _getPromotedUnit(gibUsedCount)
			);
		}

		BigDecimal percentage = _getPercentage(gibMaxCount, gibUsedCount);

		return new JSONObject(
		).put(
			"maxCount", _getPromotedValue(gibMaxCount)
		).put(
			"maxCountUnits", _getPromotedUnit(gibMaxCount)
		).put(
			"percentage", percentage.toPlainString()
		).put(
			"usedCount", _getPromotedValue(gibUsedCount)
		).put(
			"usedCountUnits", _getPromotedUnit(gibUsedCount)
		);
	}

	protected JSONObject createUsageJSONObject(
		BigDecimal maxCount, BigDecimal usedCount) {

		if (_isUnlimited(maxCount)) {
			return new JSONObject(
			).put(
				"maxCount", _QUANTITY_UNLIMITED
			).put(
				"percentage", "0"
			).put(
				"usedCount", usedCount
			);
		}

		BigDecimal percentage = _getPercentage(maxCount, usedCount);

		return new JSONObject(
		).put(
			"maxCount", maxCount
		).put(
			"percentage", percentage.toPlainString()
		).put(
			"usedCount", usedCount
		);
	}

	protected BigDecimal getMaxCount(
		Entitlement entitlement, BigDecimal maxCount, Map<Long, String> units) {

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
			_toGibibytes(
				BigDecimal.valueOf(quantity),
				units.get(entitlement.getEntitlementDefinitionId())));
	}

	protected Map<Long, String> getUnits(
		List<EntitlementDefinition> entitlementDefinitions) {

		Map<Long, String> units = new HashMap<>();

		for (EntitlementDefinition entitlementDefinition :
				entitlementDefinitions) {

			units.put(
				entitlementDefinition.getEntitlementDefinitionId(),
				entitlementDefinition.getUnit());
		}

		return units;
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