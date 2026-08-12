/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.commerce.admin.catalog.client.dto.v1_0.Product;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Felipe Veloso
 */
public class CommerceProductServiceTest {

	@BeforeEach
	public void setUp() {
		_commerceProductService = new CommerceProductService();
	}

	@Test
	public void testGetNameReadsLocalizedValue() {
		Product product = new Product();

		product.setName(
			() -> Map.of("en_US", "PaaS Experience", "pt_BR", "ignorado"));

		Assertions.assertEquals(
			"PaaS Experience", _commerceProductService.getName(product));
	}

	@Test
	public void testGetNameReturnsNullWhenNameIsAbsent() {
		Assertions.assertNull(_commerceProductService.getName(new Product()));
	}

	@Test
	public void testGetNameReturnsNullWhenProductIsAbsent() {
		Assertions.assertNull(_commerceProductService.getName(null));
	}

	private CommerceProductService _commerceProductService;

}