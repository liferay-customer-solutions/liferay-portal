/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.one.license.CommonLicenseKeyData;
import com.liferay.one.license.CommonLicenseKeyParser;

import java.time.Instant;

import java.util.Collections;
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
public class CommonLicenseKeyServiceTest {

	@BeforeEach
	public void setUp() throws Exception {
		_commonLicenseKeyService = Mockito.spy(new CommonLicenseKeyService());

		_commonLicenseKeyParser = Mockito.mock(CommonLicenseKeyParser.class);

		ReflectionTestUtils.setField(
			_commonLicenseKeyService, "_commonLicenseKeyParser",
			_commonLicenseKeyParser);

		Mockito.doReturn(
			Collections.<JSONObject>emptyList()
		).when(
			_commonLicenseKeyService
		).getAllItems(
			Mockito.eq("/o/c/commonlicensekeys"), Mockito.anyString(),
			Mockito.any()
		);
	}

	@Test
	public void testAddCommonLicenseKeyCommerce() throws Exception {
		Mockito.doReturn(
			new CommonLicenseKeyData(
				Instant.parse("2027-01-01T00:00:00Z"), "production",
				Instant.parse("2026-01-01T00:00:00Z"))
		).when(
			_commonLicenseKeyParser
		).parseCommerce(
			"<license/>"
		);

		JSONObject jsonObject = _capturePostBody(
			"<license/>", "commerce.xml", 500L, "COMMERCE");

		Assertions.assertEquals(
			"commerce.xml", jsonObject.getString("fileName"));
		Assertions.assertEquals(500L, jsonObject.getLong("fileSize"));
		Assertions.assertEquals(
			"<license/>", jsonObject.getString("fileContent"));
		Assertions.assertEquals(
			"commerce",
			jsonObject.getJSONObject(
				"productFamily"
			).getString(
				"key"
			));
		Assertions.assertEquals(
			"production",
			jsonObject.getJSONObject(
				"environmentType"
			).getString(
				"key"
			));
		Assertions.assertEquals(
			"2026-01-01T00:00:00Z", jsonObject.getString("startDate"));
		Assertions.assertEquals(
			"2027-01-01T00:00:00Z", jsonObject.getString("endDate"));
	}

	@Test
	public void testAddCommonLicenseKeyMapsNonproductionEnvironment()
		throws Exception {

		Mockito.doReturn(
			new CommonLicenseKeyData(
				Instant.parse("2027-01-01T00:00:00Z"), "non-production",
				Instant.parse("2026-01-01T00:00:00Z"))
		).when(
			_commonLicenseKeyParser
		).parseEnterpriseSearch(
			"{}"
		);

		JSONObject jsonObject = _capturePostBody(
			"{}", "es.json", 100L, "ENTERPRISE_SEARCH");

		Assertions.assertEquals(
			"enterpriseSearch",
			jsonObject.getJSONObject(
				"productFamily"
			).getString(
				"key"
			));
		Assertions.assertEquals(
			"nonProduction",
			jsonObject.getJSONObject(
				"environmentType"
			).getString(
				"key"
			));
	}

	@Test
	public void testGetCommonLicenseKey() throws Exception {
		Assertions.assertNull(
			_commonLicenseKeyService.getCommonLicenseKey(
				"production", "commerce"));

		JSONObject commonLicenseKeyJSONObject = new JSONObject(
		).put(
			"fileName", "commerce-prod.xml"
		);

		Mockito.doReturn(
			List.of(commonLicenseKeyJSONObject)
		).when(
			_commonLicenseKeyService
		).getAllItems(
			Mockito.eq("/o/c/commonlicensekeys"), Mockito.anyString(),
			Mockito.any()
		);

		Assertions.assertSame(
			commonLicenseKeyJSONObject,
			_commonLicenseKeyService.getCommonLicenseKey(
				"production", "commerce"));
	}

	@Test
	public void testHasCommonLicenseKey() throws Exception {
		Assertions.assertFalse(
			_commonLicenseKeyService.hasCommonLicenseKey("new.xml"));

		Mockito.doReturn(
			List.of(new JSONObject())
		).when(
			_commonLicenseKeyService
		).getAllItems(
			Mockito.eq("/o/c/commonlicensekeys"), Mockito.anyString(),
			Mockito.any()
		);

		Assertions.assertTrue(
			_commonLicenseKeyService.hasCommonLicenseKey("dup.xml"));
	}

	private JSONObject _capturePostBody(
			String fileContent, String fileName, long fileSize,
			String productGroup)
		throws Exception {

		ArgumentCaptor<JSONObject> argumentCaptor = ArgumentCaptor.forClass(
			JSONObject.class);

		Mockito.doNothing(
		).when(
			_commonLicenseKeyService
		).postCommonLicenseKey(
			argumentCaptor.capture()
		);

		_commonLicenseKeyService.addCommonLicenseKey(
			fileContent, fileName, fileSize, productGroup);

		return argumentCaptor.getValue();
	}

	private CommonLicenseKeyParser _commonLicenseKeyParser;
	private CommonLicenseKeyService _commonLicenseKeyService;

}