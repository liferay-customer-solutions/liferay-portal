/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.model;

import com.liferay.one.constants.EntitlementConstants;

import java.util.Collections;
import java.util.List;

import org.json.JSONObject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Allen Ziegenfus
 */
public class LDPUsageStrategyTest {

	@Test
	public void testHasUsageIsFalseForEmptyResponse() {
		LDPUsageStrategy ldpUsageStrategy = new LDPUsageStrategy(
			"{}", Collections.emptyList());

		Assertions.assertFalse(ldpUsageStrategy.hasUsage());
	}

	@Test
	public void testHasUsageIsFalseForNullResponse() {
		LDPUsageStrategy ldpUsageStrategy = new LDPUsageStrategy(
			null, Collections.emptyList());

		Assertions.assertFalse(ldpUsageStrategy.hasUsage());
	}

	@Test
	public void testHasUsageIsTrueForPopulatedResponse() {
		LDPUsageStrategy ldpUsageStrategy = new LDPUsageStrategy(
			"{\"apiRequestsCount\": 1}", Collections.emptyList());

		Assertions.assertTrue(ldpUsageStrategy.hasUsage());
	}

	@Test
	public void testToJSONObjectOmitsUsedCountWithoutUsage() {
		JSONObject jsonObject = _toJSONObject(
			null,
			Collections.singletonList(
				_createEntitlement(
					EntitlementConstants.NAME_API_REQUESTS, 5000.0)));

		Assertions.assertFalse(
			jsonObject.getJSONObject(
				LDPUsageStrategy.METRIC_API_REQUESTS
			).has(
				"usedCount"
			));
	}

	@Test
	public void testToJSONObjectPercentageIsRoundedToTwoDecimalPlaces() {
		JSONObject jsonObject = _toJSONObject(
			"{\"apiRequestsCount\": 1}",
			Collections.singletonList(
				_createEntitlement(
					EntitlementConstants.NAME_API_REQUESTS, 3.0)));

		Assertions.assertEquals(
			"33.3300",
			jsonObject.getJSONObject(
				LDPUsageStrategy.METRIC_API_REQUESTS
			).getString(
				"percentage"
			));
	}

	@Test
	public void testToJSONObjectPercentageIsZeroForZeroMaxCount() {
		JSONObject jsonObject = _toJSONObject(
			"{\"apiRequestsCount\": 10}", Collections.emptyList());

		Assertions.assertEquals(
			"0",
			jsonObject.getJSONObject(
				LDPUsageStrategy.METRIC_API_REQUESTS
			).getString(
				"percentage"
			));
	}

	private Entitlement _createEntitlement(String name, double quantity) {
		return new Entitlement(
			new JSONObject(
			).put(
				"id", ++_entitlementId
			).put(
				"name", name
			).put(
				"quantity", quantity
			));
	}

	private JSONObject _toJSONObject(
		String response, List<Entitlement> entitlements) {

		LDPUsageStrategy ldpUsageStrategy = new LDPUsageStrategy(
			response, entitlements);

		return ldpUsageStrategy.toJSONObject();
	}

	private long _entitlementId;

}