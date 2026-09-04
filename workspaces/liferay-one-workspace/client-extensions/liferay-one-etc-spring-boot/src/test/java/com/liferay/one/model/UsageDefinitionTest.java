/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.model;

import org.json.JSONObject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Ryan Schuhler
 */
public class UsageDefinitionTest {

	@Test
	public void testNoOverageMeansNoBucketsAndNoAmount() {
		UsageDefinition usageDefinition = _createUsageDefinition(
			_OVERAGE_BUCKET_SIZE, _OVERAGE_RATE);

		Assertions.assertEquals(0, usageDefinition.getOverageBucketQuantity(0));
		Assertions.assertEquals(0, usageDefinition.getOverageAmount(0));
		Assertions.assertEquals(
			0, usageDefinition.getOverageBucketQuantity(-500000));
		Assertions.assertEquals(0, usageDefinition.getOverageAmount(-500000));
	}

	@Test
	public void testPricesEveryBucketAtTheOverageRate() {
		UsageDefinition usageDefinition = _createUsageDefinition(
			_OVERAGE_BUCKET_SIZE, _OVERAGE_RATE);

		Assertions.assertEquals(
			3 * _OVERAGE_RATE, usageDefinition.getOverageAmount(500000));
	}

	@Test
	public void testRoundsPartialBucketUp() {
		UsageDefinition usageDefinition = _createUsageDefinition(
			_OVERAGE_BUCKET_SIZE, _OVERAGE_RATE);

		Assertions.assertEquals(1, usageDefinition.getOverageBucketQuantity(1));
		Assertions.assertEquals(
			2, usageDefinition.getOverageBucketQuantity(200001));
		Assertions.assertEquals(
			3, usageDefinition.getOverageBucketQuantity(500000));
	}

	@Test
	public void testUsesWholeBucketsForExactMultiples() {
		UsageDefinition usageDefinition = _createUsageDefinition(
			_OVERAGE_BUCKET_SIZE, _OVERAGE_RATE);

		Assertions.assertEquals(
			1, usageDefinition.getOverageBucketQuantity(200000));
		Assertions.assertEquals(
			2, usageDefinition.getOverageBucketQuantity(400000));
	}

	@Test
	public void testWithoutOverageBucketSizeNothingIsBillable() {
		UsageDefinition usageDefinition = _createUsageDefinition(
			null, _OVERAGE_RATE);

		Assertions.assertFalse(usageDefinition.hasOverageBucketSize());
		Assertions.assertEquals(
			0, usageDefinition.getOverageBucketQuantity(500000));
		Assertions.assertEquals(0, usageDefinition.getOverageAmount(500000));
	}

	@Test
	public void testWithoutOverageRateThereIsNoAmount() {
		UsageDefinition usageDefinition = _createUsageDefinition(
			_OVERAGE_BUCKET_SIZE, null);

		Assertions.assertEquals(
			3, usageDefinition.getOverageBucketQuantity(500000));
		Assertions.assertEquals(0, usageDefinition.getOverageAmount(500000));
	}

	private UsageDefinition _createUsageDefinition(
		Double overageBucketSize, Double overageRate) {

		JSONObject jsonObject = new JSONObject(
		).put(
			"id", 1L
		).put(
			"overageCurrency", "USD"
		);

		if (overageBucketSize != null) {
			jsonObject.put("overageBucketSize", overageBucketSize);
		}

		if (overageRate != null) {
			jsonObject.put("overageRate", overageRate);
		}

		return new UsageDefinition(jsonObject);
	}

	private static final double _OVERAGE_BUCKET_SIZE = 200000;

	private static final double _OVERAGE_RATE = 20;

}