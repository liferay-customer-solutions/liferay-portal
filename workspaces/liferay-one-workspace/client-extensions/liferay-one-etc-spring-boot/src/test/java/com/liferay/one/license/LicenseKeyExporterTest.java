/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.license;

import com.liferay.portal.ee.license.shared.LicenseConstants;

import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Allen Ziegenfus
 */
public class LicenseKeyExporterTest {

	@BeforeEach
	public void setUp() {
		_licenseKeyExporter = new LicenseKeyExporter();

		ReflectionTestUtils.setField(
			_licenseKeyExporter, "_licenseKeyGenerator",
			new LicenseKeyGenerator());
	}

	@Test
	public void testAggregateXMLsCombinesLicenses() throws Exception {
		String aggregateXML = _licenseKeyExporter.aggregateXMLs(
			new String[] {_toXML("KEY1"), _toXML("KEY2")});

		JSONObject jsonObject = XML.toJSONObject(aggregateXML);

		JSONObject licensesJSONObject = jsonObject.getJSONObject("licenses");

		JSONArray licenseJSONArray = licensesJSONObject.getJSONArray("license");

		Assertions.assertEquals(2, licenseJSONArray.length());

		JSONObject firstLicenseJSONObject = licenseJSONArray.getJSONObject(0);

		Assertions.assertEquals(
			"KEY1", firstLicenseJSONObject.getString("key"));

		JSONObject hostNamesJSONObject = firstLicenseJSONObject.getJSONObject(
			"host-names");

		Assertions.assertEquals(
			"host.example.com", hostNamesJSONObject.getString("host-name"));

		JSONObject secondLicenseJSONObject = licenseJSONArray.getJSONObject(1);

		Assertions.assertEquals(
			"KEY2", secondLicenseJSONObject.getString("key"));
	}

	@Test
	public void testGetFileNameStripsPathSeparators() {
		Assertions.assertEquals(
			"activation-key-dxp-7.4-......etccron.d.xml",
			_licenseKeyExporter.getFileName(
				"DXP", "7.4", "../../../etc/cron.d"));
	}

	@Test
	public void testGetFileNameStripsQuotes() {
		Assertions.assertEquals(
			"activation-key-dxp-7.4-evil.xml",
			_licenseKeyExporter.getFileName("DXP", "7.4", "ev\"il"));
	}

	@Test
	public void testToXMLEscapesSpecialCharacters() throws Exception {
		String xml = _licenseKeyExporter.toXML(
			"TESTKEY", "Acme Corp", "Enterprise",
			LicenseConstants.TYPE_ENTERPRISE, 3, "Liferay DXP", "", "7.4",
			"Acme Corp", 0, 0, 0, 0L, 0L, "", "R&D <secure> \"team\"", "",
			"host.example.com", "127.0.0.1", "00:11:22:33:44:55", "srv-1",
			new Date(1000000000000L), new Date(2000000000000L));

		Assertions.assertTrue(xml.startsWith("<?xml "));

		JSONObject jsonObject = XML.toJSONObject(xml);

		JSONObject licenseJSONObject = jsonObject.getJSONObject("license");

		Assertions.assertEquals(
			"Acme Corp", licenseJSONObject.getString("owner"));
		Assertions.assertEquals(
			"R&D <secure> \"team\"",
			licenseJSONObject.getString("description"));
		Assertions.assertEquals("TESTKEY", licenseJSONObject.getString("key"));
	}

	private String _toXML(String key) throws Exception {
		return _licenseKeyExporter.toXML(
			key, "Acme Corp", "Enterprise", LicenseConstants.TYPE_PRODUCTION, 4,
			"Liferay DXP", "", "7.4", "Acme Corp", 0, 1, 0, 0L, 0L, "",
			"Production license", "", "host.example.com", "127.0.0.1",
			"00:11:22:33:44:55", "srv-1", new Date(1000000000000L),
			new Date(2000000000000L));
	}

	private LicenseKeyExporter _licenseKeyExporter;

}