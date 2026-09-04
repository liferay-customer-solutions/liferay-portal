/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.model;

import java.math.BigDecimal;

import org.json.JSONObject;

/**
 * @author Drew Brokke
 */
public class UsageDefinition {

	public UsageDefinition(JSONObject jsonObject) {
		_aggregationType = jsonObject.optString("aggregationType");
		_externalReferenceCode = jsonObject.optString("externalReferenceCode");
		_overageBucketSize = jsonObject.optDoubleObject(
			"overageBucketSize", null);
		_overageCurrency = jsonObject.optString("overageCurrency");
		_overageRate = jsonObject.optDoubleObject("overageRate", null);
		_period = jsonObject.optString("period");
		_quantity = jsonObject.optDoubleObject("quantity", null);
		_unit = jsonObject.optString("unit");
		_usageDefinitionId = jsonObject.getLong("id");
	}

	public String getAggregationType() {
		return _aggregationType;
	}

	public String getExternalReferenceCode() {
		return _externalReferenceCode;
	}

	/**
	 * The cost of the overage buckets needed to cover the given overage,
	 * priced per bucket rather than per unit consumed.
	 */
	public double getOverageAmount(double overageQuantity) {
		if (_overageRate == null) {
			return 0;
		}

		BigDecimal overageAmount = BigDecimal.valueOf(
			_overageRate
		).multiply(
			BigDecimal.valueOf(getOverageBucketQuantity(overageQuantity))
		);

		return overageAmount.doubleValue();
	}

	/**
	 * The number of whole overage buckets needed to cover the given overage. A
	 * partial bucket bills as a whole one, because a bucket is the smallest
	 * quantity that can go on an order.
	 */
	public long getOverageBucketQuantity(double overageQuantity) {
		if ((overageQuantity <= 0) || !hasOverageBucketSize()) {
			return 0;
		}

		return (long)Math.ceil(overageQuantity / _overageBucketSize);
	}

	public Double getOverageBucketSize() {
		return _overageBucketSize;
	}

	public String getOverageCurrency() {
		return _overageCurrency;
	}

	public Double getOverageRate() {
		return _overageRate;
	}

	public String getPeriod() {
		return _period;
	}

	public Double getQuantity() {
		return _quantity;
	}

	public String getUnit() {
		return _unit;
	}

	public long getUsageDefinitionId() {
		return _usageDefinitionId;
	}

	public boolean hasOverageBucketSize() {
		if ((_overageBucketSize == null) || (_overageBucketSize <= 0)) {
			return false;
		}

		return true;
	}

	private final String _aggregationType;
	private final String _externalReferenceCode;
	private final Double _overageBucketSize;
	private final String _overageCurrency;
	private final Double _overageRate;
	private final String _period;
	private final Double _quantity;
	private final String _unit;
	private final long _usageDefinitionId;

}