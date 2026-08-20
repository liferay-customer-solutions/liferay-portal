/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.commerce.admin.catalog.client.dto.v1_0.Product;
import com.liferay.headless.commerce.admin.catalog.client.dto.v1_0.ProductConfiguration;
import com.liferay.headless.commerce.admin.catalog.client.dto.v1_0.ProductShippingConfiguration;
import com.liferay.headless.commerce.admin.catalog.client.dto.v1_0.Sku;
import com.liferay.headless.commerce.admin.catalog.client.problem.Problem;
import com.liferay.headless.commerce.admin.catalog.client.resource.v1_0.ProductResource;
import com.liferay.one.constants.CommerceCatalogConstants;
import com.liferay.one.constants.CommerceProductConstants;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.json.JSONObject;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * @author Kyle Bischof
 */
@Component
public class CommerceProductService extends OneBaseService {

	public void addOrUpdateProduct(
			String description, String externalReferenceCode, String name)
		throws Exception {

		ProductResource productResource = _buildProductResource();

		Product product = new Product();

		product.setActive(() -> Boolean.TRUE);
		product.setCatalogExternalReferenceCode(
			() ->
				CommerceCatalogConstants.
					EXTERNAL_REFERENCE_CODE_SALESFORCE_CATALOG);
		product.setDescription(
			() -> Collections.singletonMap("en_US", description));
		product.setExternalReferenceCode(() -> externalReferenceCode);
		product.setName(() -> Collections.singletonMap("en_US", name));
		product.setProductType(() -> CommerceProductConstants.TYPE_SIMPLE);

		ProductShippingConfiguration productShippingConfiguration =
			new ProductShippingConfiguration();

		productShippingConfiguration.setShippable(() -> Boolean.FALSE);

		ProductConfiguration productConfiguration = new ProductConfiguration();

		productConfiguration.setProductShippingConfiguration(
			() -> productShippingConfiguration);

		product.setProductConfiguration(() -> productConfiguration);

		product.setShippingConfiguration(() -> productShippingConfiguration);

		Sku sku = new Sku();

		sku.setExternalReferenceCode(() -> externalReferenceCode);
		sku.setNeverExpire(() -> Boolean.TRUE);
		sku.setPublished(() -> Boolean.TRUE);
		sku.setPurchasable(() -> Boolean.TRUE);
		sku.setSku(() -> externalReferenceCode);

		product.setSkus(() -> new Sku[] {sku});

		productResource.putProductByExternalReferenceCode(
			externalReferenceCode, product);
	}

	public void deactivateProduct(String externalReferenceCode)
		throws Exception {

		Product existingProduct = _fetchProduct(externalReferenceCode);

		if (existingProduct == null) {
			return;
		}

		ProductResource productResource = _buildProductResource();

		Product product = new Product();

		product.setActive(() -> Boolean.FALSE);
		product.setExternalReferenceCode(() -> externalReferenceCode);

		productResource.patchProductByExternalReferenceCode(
			externalReferenceCode, product);
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

	public List<String> getCategoryExternalReferenceCodes(long id)
		throws Exception {

		return getAllItems(
			"/o/headless-commerce-admin-catalog/v1.0/products/" + id +
				"/categories",
			null, jsonObject -> jsonObject.optString("externalReferenceCode"));
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

	public List<JSONObject> getProductOptions(long id) throws Exception {
		return getAllItems(
			"/o/headless-commerce-admin-catalog/v1.0/products/" + id +
				"/productOptions",
			null, jsonObject -> jsonObject, null, "productOptionValues");
	}

	public String getSpecificationValue(long id, String specificationKey)
		throws Exception {

		List<JSONObject> productSpecificationJSONObjects = getAllItems(
			"/o/headless-commerce-admin-catalog/v1.0/products/" + id +
				"/productSpecifications",
			null, jsonObject -> jsonObject);

		for (JSONObject productSpecificationJSONObject :
				productSpecificationJSONObjects) {

			if (!Objects.equals(
					specificationKey,
					productSpecificationJSONObject.optString(
						"specificationKey"))) {

				continue;
			}

			JSONObject valueJSONObject =
				productSpecificationJSONObject.optJSONObject("value");

			if (valueJSONObject == null) {
				return null;
			}

			return valueJSONObject.optString("en_US");
		}

		return null;
	}

	private ProductResource _buildProductResource() {
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
		ProductResource productResource = _buildProductResource();

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

		ProductResource productResource = _buildProductResource();

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

}