/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.model;

import com.liferay.one.constants.EntitlementConstants;

import java.util.Arrays;
import java.util.Collections;

import org.json.JSONObject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Drew Brokke
 */
public class LDPEventAllotmentTest {

	@Test
	public void testAddsBaseAndAddOnBuckets() {
		LDPEventAllotment ldpEventAllotment = new LDPEventAllotment(
			Arrays.asList(
				_createEntitlement(
					"fixed", EntitlementConstants.NAME_EVENTS, 1000000D),
				_createEntitlement(
					"fixed", EntitlementConstants.NAME_EVENTS_ADD_ON_BUCKET,
					2D),
				_createEntitlement(
					"fixed", EntitlementConstants.NAME_EVENTS_ADD_ON_BUCKET,
					1D)));

		Assertions.assertEquals(3, ldpEventAllotment.getAddOnBucketCount());
		Assertions.assertEquals(1000000, ldpEventAllotment.getBaseQuantity());
		Assertions.assertEquals(
			1000000 + (3 * EntitlementConstants.QUANTITY_EVENTS_ADD_ON_BUCKET),
			ldpEventAllotment.getEntitledQuantity());
		Assertions.assertFalse(ldpEventAllotment.isUnlimited());
	}

	@Test
	public void testIgnoresUnrelatedEntitlements() {
		LDPEventAllotment ldpEventAllotment = new LDPEventAllotment(
			Arrays.asList(
				_createEntitlement(
					"fixed", EntitlementConstants.NAME_API_REQUESTS, 5000000D),
				_createEntitlement(
					"fixed", EntitlementConstants.NAME_EVENTS, 1000000D)));

		Assertions.assertEquals(
			1000000, ldpEventAllotment.getEntitledQuantity());
	}

	@Test
	public void testNoEntitlementsMeansNoAllotment() {
		LDPEventAllotment ldpEventAllotment = new LDPEventAllotment(
			Collections.emptyList());

		Assertions.assertEquals(0, ldpEventAllotment.getEntitledQuantity());
		Assertions.assertFalse(ldpEventAllotment.isUnlimited());
	}

	@Test
	public void testUnlimitedGrantType() {
		LDPEventAllotment ldpEventAllotment = new LDPEventAllotment(
			Arrays.asList(
				_createEntitlement(
					EntitlementConstants.GRANT_TYPE_UNLIMITED,
					EntitlementConstants.NAME_EVENTS, null),
				_createEntitlement(
					"fixed", EntitlementConstants.NAME_EVENTS_ADD_ON_BUCKET,
					1D)));

		Assertions.assertTrue(ldpEventAllotment.isUnlimited());
		Assertions.assertEquals(
			EntitlementConstants.QUANTITY_EVENTS_ADD_ON_BUCKET,
			ldpEventAllotment.getEntitledQuantity());
	}

	@Test
	public void testUnlimitedNegativeQuantity() {
		LDPEventAllotment ldpEventAllotment = new LDPEventAllotment(
			Collections.singletonList(
				_createEntitlement(
					"fixed", EntitlementConstants.NAME_EVENTS, -1D)));

		Assertions.assertTrue(ldpEventAllotment.isUnlimited());
	}

	private Entitlement _createEntitlement(
		String grantType, String name, Double quantity) {

		JSONObject jsonObject = new JSONObject(
		).put(
			"grantType", grantType
		).put(
			"id", 1L
		).put(
			"name", name
		);

		if (quantity != null) {
			jsonObject.put("quantity", quantity);
		}

		return new Entitlement(jsonObject);
	}

}