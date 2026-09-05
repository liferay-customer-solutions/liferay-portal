/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.model;

import java.util.Collections;

import org.json.JSONObject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Allen Ziegenfus
 */
public class LDPEventUsageStrategyTest {

	@Test
	public void testToJSONObjectMaxCountIsZeroWithoutEntitlements() {
		JSONObject jsonObject = _toJSONObject(null);

		Assertions.assertEquals(0, jsonObject.getInt("addOnBucketCount"));
		Assertions.assertEquals(0, jsonObject.getInt("baseAllotment"));
		Assertions.assertEquals(0, jsonObject.getInt("maxCount"));
	}

	@Test
	public void testToJSONObjectOmitsUsedCountWithoutUsage() {
		Assertions.assertFalse(
			_toJSONObject(
				null
			).has(
				"usedCount"
			));
	}

	private JSONObject _toJSONObject(String response) {
		LDPEventUsageStrategy ldpEventUsageStrategy = new LDPEventUsageStrategy(
			response, Collections.emptyList());

		return ldpEventUsageStrategy.toJSONObject();
	}

}