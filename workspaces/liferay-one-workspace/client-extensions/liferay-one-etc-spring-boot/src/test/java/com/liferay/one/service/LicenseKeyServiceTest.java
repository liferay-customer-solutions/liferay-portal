/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.one.exception.NoSuchLicenseKeyException;
import com.liferay.one.license.LicenseKeyExporter;
import com.liferay.one.model.LicenseKey;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.json.JSONObject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import org.springframework.test.util.ReflectionTestUtils;

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
		).getLatestSupportedProductGroupVersion();

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
		).getLatestSupportedProductGroupVersion();

		Assertions.assertEquals(
			"7.4", _licenseKeyService.getFreeTierProductVersion());
	}

	@Test
	public void testGetFreeTierProductVersionUsesLatestProductGroupVersion()
		throws Exception {

		Mockito.doReturn(
			"2026.Q2"
		).when(
			_licenseKeyService
		).getLatestSupportedProductGroupVersion();

		Assertions.assertEquals(
			"2026.Q2", _licenseKeyService.getFreeTierProductVersion());
	}

	@Test
	public void testGetLicenseKeyByExternalReferenceCodeThrowsWhenMissing() {
		Assertions.assertThrows(
			NoSuchLicenseKeyException.class,
			() -> _licenseKeyService.getLicenseKeyByExternalReferenceCode(
				"missing-erc"));
	}

	@Test
	public void testGetLicenseKeyDownloadFileName() throws Exception {
		LicenseKeyExporter licenseKeyExporter = Mockito.mock(
			LicenseKeyExporter.class);

		ReflectionTestUtils.setField(
			_licenseKeyService, "_licenseKeyExporter", licenseKeyExporter);

		LicenseKey licenseKey = Mockito.mock(LicenseKey.class);

		Mockito.when(
			licenseKey.getName()
		).thenReturn(
			"key-1"
		);

		Mockito.when(
			licenseKey.getProductName()
		).thenReturn(
			"DXP"
		);

		Mockito.when(
			licenseKey.getProductVersion()
		).thenReturn(
			"2025.q3.9"
		);

		Mockito.when(
			licenseKeyExporter.getFileName("DXP", "2025.q3.9", "key-1")
		).thenReturn(
			"activation-key-DXP.xml"
		);

		Assertions.assertEquals(
			"activation-key-DXP.xml",
			_licenseKeyService.getLicenseKeyDownloadFileName(licenseKey));
	}

	@Test
	public void testGetLicenseKeyDownloadXML() throws Exception {
		LicenseKeyExporter licenseKeyExporter = Mockito.mock(
			LicenseKeyExporter.class);

		ReflectionTestUtils.setField(
			_licenseKeyService, "_licenseKeyExporter", licenseKeyExporter);

		LicenseKey licenseKey = new LicenseKey(
			new JSONObject(
			).put(
				"accountName", "Acme"
			).put(
				"customExpirationDate", "2027-01-01T00:00:00Z"
			).put(
				"description", "desc"
			).put(
				"domains", "example.com"
			).put(
				"hostName", "host"
			).put(
				"id", 1L
			).put(
				"ipAddresses", "1.2.3.4"
			).put(
				"key", "KEY"
			).put(
				"licenseName", "License Name"
			).put(
				"licenseType", "enterprise"
			).put(
				"licenseVersion", 3
			).put(
				"macAddresses", "AA:BB"
			).put(
				"maxClusterNodes", 1
			).put(
				"maxConcurrentUsers", 4
			).put(
				"maxHttpSessions", 3
			).put(
				"maxServers", 2
			).put(
				"maxUsers", 5
			).put(
				"name", "key-1"
			).put(
				"owner", "owner@example.com"
			).put(
				"productExternalId", "portal"
			).put(
				"productName", "DXP"
			).put(
				"productVersion", "2025.q3.9"
			).put(
				"serverId", "srv-1"
			).put(
				"sizing", "SIZING-4"
			).put(
				"startDate", "2026-01-01T00:00:00Z"
			));

		Date startDate = Date.from(licenseKey.getStartDateInstant());
		Date expirationDate = Date.from(
			licenseKey.getCustomExpirationDateInstant());

		Mockito.when(
			licenseKeyExporter.toXML(
				"KEY", "Acme", "License Name", "enterprise", 3, "DXP", "portal",
				"2025.q3.9", "owner@example.com", 1, 2, 3, 4L, 5L, "SIZING-4",
				"desc", "example.com", "host", "1.2.3.4", "AA:BB", "srv-1",
				startDate, expirationDate)
		).thenReturn(
			"<license/>"
		);

		Assertions.assertEquals(
			"<license/>",
			_licenseKeyService.getLicenseKeyDownloadXML(licenseKey));
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
	public void testGetLicenseKeysByIdsWhenLicenseKeyIdsIsEmpty()
		throws Exception {

		Assertions.assertEquals(
			Collections.emptyList(),
			_licenseKeyService.getLicenseKeysByIds(null, new long[0]));

		Mockito.verify(
			_licenseKeyService, Mockito.never()
		).getAllItems(
			Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any()
		);
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