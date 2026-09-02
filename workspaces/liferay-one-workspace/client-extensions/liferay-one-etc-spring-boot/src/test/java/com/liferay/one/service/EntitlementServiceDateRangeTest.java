/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.one.constants.EntitlementConstants;

import java.time.Instant;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author Drew Brokke
 */
public class EntitlementServiceDateRangeTest {

	@Test
	public void testFilterMatchesNamesOverlappingTheRange() throws Exception {
		EntitlementService entitlementService = Mockito.spy(
			new EntitlementService());

		Mockito.doReturn(
			Collections.emptyList()
		).when(
			entitlementService
		).getEntitlements(
			Mockito.anyString()
		);

		Instant endInstant = Instant.parse("2026-09-01T00:00:00Z");
		Instant startInstant = Instant.parse("2026-08-01T00:00:00Z");

		entitlementService.getEntitlements(
			endInstant,
			Arrays.asList(
				EntitlementConstants.NAME_EVENTS,
				EntitlementConstants.NAME_EVENTS_ADD_ON_BUCKET),
			startInstant);

		ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(
			String.class);

		Mockito.verify(
			entitlementService
		).getEntitlements(
			argumentCaptor.capture()
		);

		Assertions.assertEquals(
			"(endDate eq null or endDate ge 2026-08-01T00:00:00Z) and (name " +
				"eq 'events' or name eq 'events-add-on-bucket') and " +
					"(startDate eq null or startDate lt 2026-09-01T00:00:00Z)",
			argumentCaptor.getValue());
	}

	@Test
	public void testNoNamesMeansNoQuery() throws Exception {
		EntitlementService entitlementService = Mockito.spy(
			new EntitlementService());

		Assertions.assertTrue(
			entitlementService.getEntitlements(
				Instant.now(), Collections.emptyList(), Instant.now()
			).isEmpty());

		Mockito.verify(
			entitlementService, Mockito.never()
		).getEntitlements(
			Mockito.anyString()
		);
	}

}