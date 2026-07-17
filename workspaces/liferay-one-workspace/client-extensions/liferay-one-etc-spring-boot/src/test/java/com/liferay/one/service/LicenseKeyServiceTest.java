/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.one.exception.NoSuchLicenseKeyException;
import com.liferay.one.model.LicenseKey;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import java.util.Collections;
import java.util.List;

import org.json.JSONObject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author Allen Ziegenfus
 */
public class LicenseKeyServiceTest {

	@BeforeEach
	public void setUp() throws Exception {
		_licenseKeyService = Mockito.spy(new LicenseKeyService());

		_filterCaptor = ArgumentCaptor.forClass(String.class);

		Mockito.doReturn(
			Collections.<LicenseKey>emptyList()
		).when(
			_licenseKeyService
		).getAllItems(
			Mockito.eq("/o/c/licensekeys"), _filterCaptor.capture(),
			Mockito.any()
		);
	}

	@Test
	public void testGetAssetReceiptLicenseLicenseKeysFilter() throws Exception {
		_licenseKeyService.getAssetReceiptLicenseLicenseKeys(
			true, false, "order-1");

		Assertions.assertEquals(
			"(active eq true) and (complimentary eq false) and (orderId eq " +
				"'order-1')",
			_filterCaptor.getValue());
	}

	@Test
	public void testGetFreeTierProductVersionFallsBackToDefault()
		throws Exception {

		Mockito.doReturn(
			null
		).when(
			_licenseKeyService
		).getLatestSupportedProductVersion();

		Assertions.assertEquals(
			"7.4", _licenseKeyService.getFreeTierProductVersion());
	}

	@Test
	public void testGetFreeTierProductVersionFallsBackWhenLookupFails()
		throws Exception {

		Mockito.doThrow(
			new RuntimeException()
		).when(
			_licenseKeyService
		).getLatestSupportedProductVersion();

		Assertions.assertEquals(
			"7.4", _licenseKeyService.getFreeTierProductVersion());
	}

	@Test
	public void testGetFreeTierProductVersionUsesLatest() throws Exception {
		Mockito.doReturn(
			"2025.q3.9"
		).when(
			_licenseKeyService
		).getLatestSupportedProductVersion();

		Assertions.assertEquals(
			"2025.q3.9", _licenseKeyService.getFreeTierProductVersion());
	}

	@Test
	public void testGetLicenseKeyByExternalReferenceCodeThrowsWhenMissing() {
		Assertions.assertThrows(
			NoSuchLicenseKeyException.class,
			() -> _licenseKeyService.getLicenseKeyByExternalReferenceCode(
				"missing-erc"));
	}

	@Test
	public void testGetLicenseKeysByEntitlementFilter() throws Exception {
		_licenseKeyService.getLicenseKeys(true, false, 777L);

		Assertions.assertEquals(
			"(active eq true) and (complimentary eq false) and " +
				"(entitlementId eq '777')",
			_filterCaptor.getValue());
	}

	@Test
	public void testGetLicenseKeysByNameFilter() throws Exception {
		_licenseKeyService.getLicenseKeysByName(true, "DXP", "srv-1");

		Assertions.assertEquals(
			"(active eq true) and (productName eq 'DXP') and (serverId eq " +
				"'srv-1')",
			_filterCaptor.getValue());
	}

	@Test
	public void testGetLicenseKeysByOrderProductServerActiveFilter()
		throws Exception {

		_licenseKeyService.getLicenseKeys(true, "order-1", "portal", "srv-1");

		Assertions.assertEquals(
			"(active eq true) and (orderId eq 'order-1') and " +
				"(productExternalId eq 'portal') and (serverId eq 'srv-1')",
			_filterCaptor.getValue());
	}

	@Test
	public void testGetLicenseKeysByProductAndServerFilter() throws Exception {
		_licenseKeyService.getLicenseKeys("portal", "srv-1");

		Assertions.assertEquals(
			"(productExternalId eq 'portal') and (serverId eq 'srv-1')",
			_filterCaptor.getValue());
	}

	@Test
	public void testGetLicenseKeysByTypeOwnerDomainsFilter() throws Exception {
		_licenseKeyService.getLicenseKeys(
			"example.com", "enterprise", "Acme Corp");

		Assertions.assertEquals(
			"(domains eq 'example.com') and (licenseType eq 'enterprise') " +
				"and (owner eq 'Acme Corp')",
			_filterCaptor.getValue());
	}

	@Test
	public void testHasValidLicenseKeyTypeFreeFilter() throws Exception {
		Assertions.assertFalse(
			_licenseKeyService.hasValidLicenseKeyTypeFree(
				"example.com", "owner@example.com"));

		Assertions.assertEquals(
			"(domains eq 'example.com') and (licenseType eq 'free') and " +
				"(owner eq 'owner@example.com')",
			_filterCaptor.getValue());
	}

	@Test
	public void testHasValidLicenseKeyTypeFreeReturnsFalseWithinRenewalWindow()
		throws Exception {

		LicenseKey licenseKey = _freeLicenseKey(
			Instant.now(
			).plus(
				10, ChronoUnit.DAYS
			));

		Mockito.doReturn(
			List.of(licenseKey)
		).when(
			_licenseKeyService
		).getAllItems(
			Mockito.eq("/o/c/licensekeys"), Mockito.anyString(), Mockito.any()
		);

		Assertions.assertFalse(
			_licenseKeyService.hasValidLicenseKeyTypeFree(
				"example.com", "owner@example.com"));
	}

	@Test
	public void testHasValidLicenseKeyTypeFreeReturnsTrueBeyondRenewalWindow()
		throws Exception {

		LicenseKey licenseKey = _freeLicenseKey(
			Instant.now(
			).plus(
				200, ChronoUnit.DAYS
			));

		Mockito.doReturn(
			List.of(licenseKey)
		).when(
			_licenseKeyService
		).getAllItems(
			Mockito.eq("/o/c/licensekeys"), Mockito.anyString(), Mockito.any()
		);

		Assertions.assertTrue(
			_licenseKeyService.hasValidLicenseKeyTypeFree(
				"example.com", "owner@example.com"));
	}

	@Test
	public void testSearchBuildsFilterAndSkipsNulls() throws Exception {
		_licenseKeyService.search(
			Boolean.TRUE, null, null, null, "enterprise", null, null, null,
			"DXP", null);

		Assertions.assertEquals(
			"(active eq true) and (licenseType eq 'enterprise') and " +
				"(productName eq 'DXP')",
			_filterCaptor.getValue());
	}

	@Test
	public void testSearchEscapesSingleQuotes() throws Exception {
		_licenseKeyService.search(
			null, null, null, null, null, null, "O'Connor", null, null, null);

		Assertions.assertEquals(
			"(owner eq 'O''Connor')", _filterCaptor.getValue());
	}

	@Test
	public void testSearchWithNoCriteriaUsesNullFilter() throws Exception {
		_licenseKeyService.search(
			null, null, null, null, null, null, null, null, null, null);

		Assertions.assertNull(_filterCaptor.getValue());
	}

	private LicenseKey _freeLicenseKey(Instant customExpirationDateInstant) {
		return new LicenseKey(
			new JSONObject(
			).put(
				"customExpirationDate", customExpirationDateInstant.toString()
			).put(
				"id", 1L
			).put(
				"startDate",
				Instant.now(
				).toString()
			));
	}

	private ArgumentCaptor<String> _filterCaptor;
	private LicenseKeyService _licenseKeyService;

}