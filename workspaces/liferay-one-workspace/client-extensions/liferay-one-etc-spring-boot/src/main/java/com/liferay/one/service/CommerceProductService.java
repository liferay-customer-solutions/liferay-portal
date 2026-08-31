/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.commerce.admin.catalog.client.dto.v1_0.Product;
import com.liferay.headless.commerce.admin.catalog.client.dto.v1_0.Sku;
import com.liferay.headless.commerce.admin.catalog.client.problem.Problem;
import com.liferay.headless.commerce.admin.catalog.client.resource.v1_0.ProductResource;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * @author Kyle Bischof
 */
@Component
public class CommerceProductService extends OneBaseService {

	public void deactivateProduct(String salesforceProductId) throws Exception {
		Sku sku = _updateSku(false, salesforceProductId);

		if (sku == null) {
			_logMissingSku(salesforceProductId);

			return;
		}

		if (_hasPublishedSku(sku.getProductId())) {
			return;
		}

		ProductResource productResource = buildProductResource();

		Product product = new Product();

		product.setActive(() -> Boolean.FALSE);

		productResource.patchProduct(sku.getProductId(), product);
	}

	@Cacheable("product")
	public Product fetchProduct(long id) throws Exception {
		return _fetchProduct(id);
	}

	public Product fetchProduct(String externalReferenceCode) throws Exception {
		return _fetchProduct(externalReferenceCode);
	}

	@Cacheable("productName")
	public String fetchProductName(long id) throws Exception {
		return getName(_fetchProduct(id));
	}

	@Cacheable("productName")
	public String fetchProductName(String externalReferenceCode)
		throws Exception {

		return getName(_fetchProduct(externalReferenceCode));
	}

	public String getName(Product product) {
		if (product == null) {
			return null;
		}

		Map<String, String> name = product.getName();

		if (name == null) {
			return null;
		}

		return name.get("en_US");
	}

	public void updateProduct(
			String description, String name, String salesforceProductId)
		throws Exception {

		Sku sku = _updateSku(true, salesforceProductId);

		if (sku == null) {
			_logMissingSku(salesforceProductId);

			return;
		}

		ProductResource productResource = buildProductResource();

		Product product = new Product();

		product.setActive(() -> Boolean.TRUE);

		if (_hasSingleSku(sku.getProductId())) {
			product.setDescription(
				() -> Collections.singletonMap("en_US", description));
			product.setName(() -> Collections.singletonMap("en_US", name));
		}

		productResource.patchProduct(sku.getProductId(), product);
	}

	protected ProductResource buildProductResource() {
		return ProductResource.builder(
		).endpoint(
			getDXPEndpointAddress(), lxcDXPServerProtocol
		).header(
			HttpHeaders.AUTHORIZATION, getAuthorization()
		).parameter(
			"nestedFields", "productSpecifications"
		).build();
	}

	private Product _fetchProduct(long id) throws Exception {
		ProductResource productResource = buildProductResource();

		try {
			return productResource.getProduct(id);
		}
		catch (Problem.ProblemException problemException) {
			Problem problem = problemException.getProblem();

			if ((problem != null) && isNotFound(problem.getStatus())) {
				return null;
			}

			throw problemException;
		}
	}

	private Product _fetchProduct(String externalReferenceCode)
		throws Exception {

		ProductResource productResource = buildProductResource();

		try {
			return productResource.getProductByExternalReferenceCode(
				externalReferenceCode);
		}
		catch (Problem.ProblemException problemException) {
			Problem problem = problemException.getProblem();

			if ((problem != null) && isNotFound(problem.getStatus())) {
				return null;
			}

			throw problemException;
		}
	}

	private boolean _hasPublishedSku(long productId) throws Exception {
		for (Sku sku : _commerceSkuService.getSkus(productId)) {
			if (Boolean.TRUE.equals(sku.getPublished())) {
				return true;
			}
		}

		return false;
	}

	private boolean _hasSingleSku(long productId) throws Exception {
		List<Sku> skus = _commerceSkuService.getSkus(productId);

		if (skus.size() == 1) {
			return true;
		}

		return false;
	}

	private void _logMissingSku(String salesforceProductId) {
		if (_log.isWarnEnabled()) {
			_log.warn(
				"No SKU exists for Salesforce product " + salesforceProductId);
		}
	}

	private Sku _updateSku(boolean published, String salesforceProductId)
		throws Exception {

		Sku sku = new Sku();

		sku.setPublished(() -> published);
		sku.setPurchasable(() -> published);

		return _commerceSkuService.patchSku(salesforceProductId, sku);
	}

	private static final Log _log = LogFactory.getLog(
		CommerceProductService.class);

	@Autowired
	private CommerceSkuService _commerceSkuService;

}