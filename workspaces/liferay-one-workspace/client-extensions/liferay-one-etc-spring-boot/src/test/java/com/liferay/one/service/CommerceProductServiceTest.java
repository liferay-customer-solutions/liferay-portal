/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.commerce.admin.catalog.client.dto.v1_0.Product;
import com.liferay.headless.commerce.admin.catalog.client.dto.v1_0.Sku;
import com.liferay.headless.commerce.admin.catalog.client.resource.v1_0.ProductResource;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Felipe Veloso
 */
public class CommerceProductServiceTest {

	@BeforeEach
	public void setUp() {
		_commerceProductService = Mockito.spy(new CommerceProductService());

		ReflectionTestUtils.setField(
			_commerceProductService, "_commerceSkuService",
			_commerceSkuService);

		Mockito.doReturn(
			_productResource
		).when(
			_commerceProductService
		).buildProductResource();
	}

	@Test
	public void testDeactivateProductDeactivatesProductWhenNoOtherSkuIsPublished()
		throws Exception {

		_setUpSku(_SALESFORCE_PRODUCT_ID, false);

		_setUpSkus(
			_createSku(_SALESFORCE_PRODUCT_ID, false),
			_createSku("PROD-2", false));

		_commerceProductService.deactivateProduct(_SALESFORCE_PRODUCT_ID);

		Assertions.assertFalse(_capturePatchedSku().getPublished());

		Product product = _capturePatchedProduct();

		Assertions.assertFalse(product.getActive());
	}

	@Test
	public void testDeactivateProductIgnoresAbsentSku() throws Exception {
		_commerceProductService.deactivateProduct(_SALESFORCE_PRODUCT_ID);

		Mockito.verify(
			_commerceSkuService, Mockito.never()
		).getSkus(
			Mockito.anyLong()
		);

		Mockito.verifyNoInteractions(_productResource);
	}

	@Test
	public void testDeactivateProductUnpublishesSkuWhenOtherSkuIsPublished()
		throws Exception {

		_setUpSku(_SALESFORCE_PRODUCT_ID, false);

		_setUpSkus(
			_createSku(_SALESFORCE_PRODUCT_ID, false),
			_createSku("PROD-2", true));

		_commerceProductService.deactivateProduct(_SALESFORCE_PRODUCT_ID);

		Assertions.assertFalse(_capturePatchedSku().getPublished());

		Mockito.verifyNoInteractions(_productResource);
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

	@Test
	public void testUpdateProductIgnoresAbsentSku() throws Exception {
		_commerceProductService.updateProduct(
			"A description", "Widget", _SALESFORCE_PRODUCT_ID);

		Mockito.verify(
			_commerceSkuService, Mockito.never()
		).getSkus(
			Mockito.anyLong()
		);

		Mockito.verifyNoInteractions(_productResource);
	}

	@Test
	public void testUpdateProductReactivatesProductWithSeveralSkusWithoutRenaming()
		throws Exception {

		Sku sku = _setUpSku(_SALESFORCE_PRODUCT_ID, true);

		_setUpSkus(sku, _createSku("PROD-2", true));

		_commerceProductService.updateProduct(
			"A description", "Widget", _SALESFORCE_PRODUCT_ID);

		Assertions.assertTrue(_capturePatchedSku().getPublished());

		Product product = _capturePatchedProduct();

		Assertions.assertTrue(product.getActive());
		Assertions.assertNull(product.getDescription());
		Assertions.assertNull(product.getName());
	}

	@Test
	public void testUpdateProductUpdatesProductWithSingleSku()
		throws Exception {

		Sku sku = _setUpSku(_SALESFORCE_PRODUCT_ID, true);

		_setUpSkus(sku);

		_commerceProductService.updateProduct(
			"A description", "Widget", _SALESFORCE_PRODUCT_ID);

		Assertions.assertTrue(_capturePatchedSku().getPublished());

		Product product = _capturePatchedProduct();

		Assertions.assertTrue(product.getActive());

		Map<String, String> description = product.getDescription();

		Assertions.assertEquals("A description", description.get("en_US"));

		Map<String, String> name = product.getName();

		Assertions.assertEquals("Widget", name.get("en_US"));
	}

	private Product _capturePatchedProduct() throws Exception {
		ArgumentCaptor<Product> productArgumentCaptor = ArgumentCaptor.forClass(
			Product.class);

		Mockito.verify(
			_productResource
		).patchProduct(
			Mockito.eq(_PRODUCT_ID), productArgumentCaptor.capture()
		);

		return productArgumentCaptor.getValue();
	}

	private Sku _capturePatchedSku() throws Exception {
		ArgumentCaptor<Sku> skuArgumentCaptor = ArgumentCaptor.forClass(
			Sku.class);

		Mockito.verify(
			_commerceSkuService
		).patchSku(
			Mockito.eq(_SALESFORCE_PRODUCT_ID), skuArgumentCaptor.capture()
		);

		return skuArgumentCaptor.getValue();
	}

	private Sku _createSku(String externalReferenceCode, boolean published) {
		Sku sku = new Sku();

		sku.setExternalReferenceCode(externalReferenceCode);
		sku.setProductId(_PRODUCT_ID);
		sku.setPublished(published);

		return sku;
	}

	private Sku _setUpSku(String externalReferenceCode, boolean published)
		throws Exception {

		Sku sku = _createSku(externalReferenceCode, published);

		Mockito.when(
			_commerceSkuService.patchSku(
				Mockito.eq(externalReferenceCode), Mockito.any())
		).thenReturn(
			sku
		);

		return sku;
	}

	private void _setUpSkus(Sku... skus) throws Exception {
		Mockito.when(
			_commerceSkuService.getSkus(_PRODUCT_ID)
		).thenReturn(
			List.of(skus)
		);
	}

	private static final long _PRODUCT_ID = 77;

	private static final String _SALESFORCE_PRODUCT_ID = "PROD-1";

	private CommerceProductService _commerceProductService;
	private final CommerceSkuService _commerceSkuService = Mockito.mock(
		CommerceSkuService.class);
	private final ProductResource _productResource = Mockito.mock(
		ProductResource.class);

}