/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.json.JSONObject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Allen Ziegenfus
 */
public class LicenseKeyTest {

	@Test
	public void testSerializationOmitsKey() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();

		objectMapper.registerModule(new JavaTimeModule());

		String json = objectMapper.writeValueAsString(
			new LicenseKey(
				new JSONObject(
				).put(
					"customExpirationDate", "2027-01-01T00:00:00Z"
				).put(
					"id", 1L
				).put(
					"key", _KEY
				).put(
					"name", "key-1"
				).put(
					"startDate", "2026-01-01T00:00:00Z"
				)));

		Assertions.assertFalse(
			json.contains(_KEY),
			"The license key material must not be serialized: " + json);
		Assertions.assertTrue(json.contains("key-1"), json);
	}

	private static final String _KEY = "SECRET-KEY-MATERIAL";

}