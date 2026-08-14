/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

/**
 * @author Ryan Schuhler
 */
public class WebMvcConfigurationTest {

	@Test
	public void testAddCorsMappingsReadsCommaSeparatedDomains() {
		Assertions.assertEquals(
			List.of("http://localhost:8080", "http://one.localhost"),
			_getAllowedOrigins("localhost:8080,one.localhost", "http"));
	}

	@Test
	public void testAddCorsMappingsReadsNewLineSeparatedDomains() {
		Assertions.assertEquals(
			List.of("https://one.liferay.com", "https://two.liferay.com"),
			_getAllowedOrigins("one.liferay.com\ntwo.liferay.com\n", "https"));
	}

	@Test
	public void testAddCorsMappingsSkipsMappingWithoutDomains() {
		CorsRegistry corsRegistry = new CorsRegistry();

		_createWebMvcConfiguration(
			"", "https"
		).addCorsMappings(
			corsRegistry
		);

		Assertions.assertTrue(
			_getCorsConfigurations(
				corsRegistry
			).isEmpty());
	}

	private WebMvcConfiguration _createWebMvcConfiguration(
		String domains, String serverProtocol) {

		WebMvcConfiguration webMvcConfiguration = new WebMvcConfiguration();

		ReflectionTestUtils.setField(webMvcConfiguration, "_domains", domains);
		ReflectionTestUtils.setField(
			webMvcConfiguration, "_serverProtocol", serverProtocol);

		return webMvcConfiguration;
	}

	private List<String> _getAllowedOrigins(
		String domains, String serverProtocol) {

		CorsRegistry corsRegistry = new CorsRegistry();

		_createWebMvcConfiguration(
			domains, serverProtocol
		).addCorsMappings(
			corsRegistry
		);

		Map<String, CorsConfiguration> corsConfigurations =
			_getCorsConfigurations(corsRegistry);

		CorsConfiguration corsConfiguration = corsConfigurations.get(
			"/invitations/**");

		Assertions.assertNotNull(corsConfiguration);

		return corsConfiguration.getAllowedOrigins();
	}

	@SuppressWarnings("unchecked")
	private Map<String, CorsConfiguration> _getCorsConfigurations(
		CorsRegistry corsRegistry) {

		return (Map<String, CorsConfiguration>)ReflectionTestUtils.invokeMethod(
			corsRegistry, "getCorsConfigurations");
	}

}