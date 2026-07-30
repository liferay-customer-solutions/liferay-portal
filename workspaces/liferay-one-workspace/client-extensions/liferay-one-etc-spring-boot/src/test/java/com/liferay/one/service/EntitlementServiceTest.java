/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.one.model.Entitlement;
import com.liferay.one.model.EntitlementDefinition;

import java.time.Instant;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Allen Ziegenfus
 */
public class EntitlementServiceTest {

	@BeforeEach
	public void setUp() throws Exception {
		_entitlementService = Mockito.spy(new EntitlementService());

		_entitlementDefinitionService = Mockito.mock(
			EntitlementDefinitionService.class);

		ReflectionTestUtils.setField(
			_entitlementService, "_entitlementDefinitionService",
			_entitlementDefinitionService);
	}

	@Test
	public void testHasEntitlementCoveringDateRangeReturnsFalseWhenNoDefinition()
		throws Exception {

		Mockito.when(
			_entitlementDefinitionService.getEntitlementDefinitions(
				Mockito.anyString())
		).thenReturn(
			Collections.emptyList()
		);

		Assertions.assertFalse(
			_entitlementService.hasEntitlementCoveringDateRange(
				1L, "C_ENT_DEF_COMMERCE", Instant.parse("2026-01-01T00:00:00Z"),
				Instant.parse("2027-01-01T00:00:00Z")));

		Mockito.verify(
			_entitlementService, Mockito.never()
		).getAllItems(
			Mockito.eq("/o/c/entitlements"), Mockito.anyString(), Mockito.any()
		);
	}

	@Test
	public void testHasEntitlementCoveringDateRangeScopesToDefinition()
		throws Exception {

		EntitlementDefinition entitlementDefinition = Mockito.mock(
			EntitlementDefinition.class);

		Mockito.when(
			entitlementDefinition.getEntitlementDefinitionId()
		).thenReturn(
			99L
		);

		Mockito.when(
			_entitlementDefinitionService.getEntitlementDefinitions(
				Mockito.anyString())
		).thenReturn(
			List.of(entitlementDefinition)
		);

		ArgumentCaptor<String> filterStringArgumentCaptor =
			ArgumentCaptor.forClass(String.class);

		Mockito.doReturn(
			List.of(Mockito.mock(Entitlement.class))
		).when(
			_entitlementService
		).getAllItems(
			Mockito.eq("/o/c/entitlements"),
			filterStringArgumentCaptor.capture(), Mockito.any()
		);

		Assertions.assertTrue(
			_entitlementService.hasEntitlementCoveringDateRange(
				1L, "C_ENT_DEF_COMMERCE", Instant.parse("2026-01-01T00:00:00Z"),
				Instant.parse("2027-01-01T00:00:00Z")));

		Assertions.assertTrue(
			filterStringArgumentCaptor.getValue(
			).contains(
				"entitlementDefinitionId eq '99'"
			));
	}

	private EntitlementDefinitionService _entitlementDefinitionService;
	private EntitlementService _entitlementService;

}