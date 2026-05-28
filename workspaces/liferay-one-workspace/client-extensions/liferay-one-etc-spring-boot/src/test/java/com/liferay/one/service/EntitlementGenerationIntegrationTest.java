/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.petra.string.StringBundler;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.nio.charset.StandardCharsets;

import java.time.Duration;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.assertj.core.api.Assertions;

import org.json.JSONArray;
import org.json.JSONObject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * End-to-end integration test for Entitlement generation.
 *
 * <p>
 * Drives a real Liferay instance: creates CProduct + SKU + N
 * EntitlementDefinitions, then a CommerceOrder + CommerceOrderItem. The
 * onAfterAdd Object Action on L_COMMERCE_ORDER_ITEM fires the Spring Boot CX,
 * which generates Entitlement rows. The test retry-asserts on /o/c/entitlements
 * until N rows materialize (or times out).
 * </p>
 *
 * <p>
 * Skipped by default. Enable by setting environment variables:
 * </p>
 *
 * <pre>
 * LIFERAY_URL=http://localhost:8080
 * LIFERAY_USERNAME=test@liferay.com   # optional, defaults to test@liferay.com
 * LIFERAY_PASSWORD=test               # optional, defaults to test
 * </pre>
 *
 * <p>
 * Preconditions: the liferay-one-batch and liferay-one-etc-spring-boot client
 * extensions must be deployed and reachable from Liferay's container so the
 * Object Action webhook can call back into the Spring Boot CX.
 * </p>
 *
 * @author Felipe Veloso
 */
@EnabledIfEnvironmentVariable(matches = ".+", named = "LIFERAY_URL")
public class EntitlementGenerationIntegrationTest {

	@AfterEach
	public void tearDown() {
		if (_commerceOrderItemId > 0) {
			for (JSONObject entitlementJSONObject :
					_safeGetEntitlements(_commerceOrderItemId)) {

				_safeDelete(
					"/o/c/entitlements/" + entitlementJSONObject.getLong("id"));
			}

			_safeDelete(
				"/o/headless-commerce-admin-order/v1.0/orderItems/" +
					_commerceOrderItemId);
		}

		if (_commerceOrderId > 0) {
			_safeDelete(
				"/o/headless-commerce-admin-order/v1.0/orders/" +
					_commerceOrderId);
		}

		for (long id : _entitlementDefinitionIds) {
			_safeDelete("/o/c/entitlementdefinitions/" + id);
		}

		if (_skuId > 0) {
			_safeDelete(
				"/o/headless-commerce-admin-catalog/v1.0/skus/" + _skuId);
		}

		if (_cProductId > 0) {
			_safeDelete(
				"/o/headless-commerce-admin-catalog/v1.0/products/" +
					_cProductId);
		}

		if (_contractId > 0) {
			_safeDelete("/o/c/contracts/" + _contractId);
		}

		if (_accountEntryId > 0) {
			_safeDelete(
				"/o/headless-admin-user/v1.0/accounts/" + _accountEntryId);
		}
	}

	@Test
	public void testEntitlementsGeneratedOnCommerceOrderItemCreate()
		throws Exception {

		long catalogId = _resolveCatalogId();

		JSONObject accountJSONObject = _post(
			"/o/headless-admin-user/v1.0/accounts",
			new JSONObject(
			).put(
				"externalReferenceCode", "IT_ACCT_" + _RUN_ID
			).put(
				"name", "IT Test Account " + _RUN_ID
			).put(
				"type", "business"
			));

		_accountEntryId = accountJSONObject.getLong("id");

		JSONObject contractJSONObject = _post(
			"/o/c/contracts",
			new JSONObject(
			).put(
				"externalReferenceCode", "IT_CONTRACT_" + _RUN_ID
			).put(
				"r_accountEntryToContract_accountEntryId", _accountEntryId
			));

		_contractId = contractJSONObject.getLong("id");

		long channelId = _resolveChannelId();

		JSONObject productJSONObject = _post(
			"/o/headless-commerce-admin-catalog/v1.0/products",
			new JSONObject(
			).put(
				"active", true
			).put(
				"catalogId", catalogId
			).put(
				"externalReferenceCode", "IT_CPROD_" + _RUN_ID
			).put(
				"name",
				new JSONObject(
				).put(
					"en_US", "IT Test Product " + _RUN_ID
				)
			).put(
				"productType", "simple"
			));

		_cProductId = productJSONObject.getLong("productId");

		JSONObject skuJSONObject = _post(
			"/o/headless-commerce-admin-catalog/v1.0/products/" + _cProductId +
				"/skus",
			new JSONObject(
			).put(
				"externalReferenceCode", "IT_SKU_" + _RUN_ID
			).put(
				"price", 0
			).put(
				"published", true
			).put(
				"purchasable", true
			).put(
				"sku", "IT-SKU-" + _RUN_ID
			));

		_skuId = skuJSONObject.getLong("id");

		String[] definitionNames = {
			"it-database-size", "it-vcpu", "it-domains"
		};

		for (int i = 0; i < definitionNames.length; i++) {
			JSONObject definitionJSONObject = _post(
				"/o/c/entitlementdefinitions",
				new JSONObject(
				).put(
					"defaultQuantity", 10.0 + i
				).put(
					"displayName", definitionNames[i]
				).put(
					"entitlementDefinitionActive", true
				).put(
					"externalReferenceCode",
					StringBundler.concat("IT_EDEF_", _RUN_ID, "_", i)
				).put(
					"grantType", "fixed"
				).put(
					"name", definitionNames[i]
				).put(
					"r_commerceProductToEntitlementDefinition_CProductId",
					_cProductId
				));

			_entitlementDefinitionIds.add(definitionJSONObject.getLong("id"));
		}

		JSONObject orderJSONObject = _post(
			"/o/headless-commerce-admin-order/v1.0/orders",
			new JSONObject(
			).put(
				"accountId", _accountEntryId
			).put(
				"channelId", channelId
			).put(
				"currencyCode", "USD"
			).put(
				"r_contractToOrder_c_contractId", _contractId
			));

		_commerceOrderId = orderJSONObject.getLong("id");

		JSONObject orderItemJSONObject = _post(
			"/o/headless-commerce-admin-order/v1.0/orders/" + _commerceOrderId +
				"/orderItems",
			new JSONObject(
			).put(
				"quantity", 1
			).put(
				"skuId", _skuId
			));

		_commerceOrderItemId = orderItemJSONObject.getLong("id");

		_retryAssert(
			Duration.ofSeconds(30),
			() -> {
				List<JSONObject> entitlements = _safeGetEntitlements(
					_commerceOrderItemId);

				Set<String> generatedNames = new HashSet<>();

				for (JSONObject entitlementJSONObject : entitlements) {
					generatedNames.add(entitlementJSONObject.getString("name"));
				}

				Assertions.assertThat(
					generatedNames
				).containsExactlyInAnyOrder(
					definitionNames
				);

				Assertions.assertThat(
					entitlements
				).allSatisfy(
					entitlementJSONObject -> Assertions.assertThat(
						entitlementJSONObject.optLong(
							"r_contractToEntitlement_c_contractId")
					).isEqualTo(
						_contractId
					)
				);
			});
	}

	private String _basicAuthHeader() {
		String username = System.getenv(
		).getOrDefault(
			"LIFERAY_USERNAME", "test@liferay.com"
		);
		String password = System.getenv(
		).getOrDefault(
			"LIFERAY_PASSWORD", "test"
		);

		String credentials = username + ":" + password;

		Base64.Encoder encoder = Base64.getEncoder();

		String encoded = encoder.encodeToString(
			credentials.getBytes(StandardCharsets.UTF_8));

		return "Basic " + encoded;
	}

	private JSONObject _get(String path) throws Exception {
		HttpRequest httpRequest = HttpRequest.newBuilder(
			URI.create(_BASE_URL + path)
		).header(
			"Accept", "application/json"
		).header(
			"Authorization", _basicAuthHeader()
		).GET(
		).build();

		HttpResponse<String> httpResponse = _httpClient.send(
			httpRequest, HttpResponse.BodyHandlers.ofString());

		if (httpResponse.statusCode() >= 400) {
			throw new RuntimeException(
				StringBundler.concat(
					"GET ", path, " returned ", httpResponse.statusCode(), ": ",
					httpResponse.body()));
		}

		return new JSONObject(httpResponse.body());
	}

	private JSONObject _post(String path, JSONObject bodyJSONObject)
		throws Exception {

		HttpRequest httpRequest = HttpRequest.newBuilder(
			URI.create(_BASE_URL + path)
		).header(
			"Accept", "application/json"
		).header(
			"Authorization", _basicAuthHeader()
		).header(
			"Content-Type", "application/json"
		).POST(
			HttpRequest.BodyPublishers.ofString(
				bodyJSONObject.toString(), StandardCharsets.UTF_8)
		).build();

		HttpResponse<String> httpResponse = _httpClient.send(
			httpRequest, HttpResponse.BodyHandlers.ofString());

		if (httpResponse.statusCode() >= 400) {
			throw new RuntimeException(
				StringBundler.concat(
					"POST ", path, " returned ", httpResponse.statusCode(),
					": ", httpResponse.body()));
		}

		return new JSONObject(httpResponse.body());
	}

	private long _resolveCatalogId() throws Exception {
		JSONObject responseJSONObject = _get(
			"/o/headless-commerce-admin-catalog/v1.0/catalog" +
				"/by-externalReferenceCode/SALESFORCE_CATALOG");

		return responseJSONObject.getLong("id");
	}

	private long _resolveChannelId() throws Exception {
		JSONObject responseJSONObject = _get(
			"/o/headless-commerce-admin-channel/v1.0/channels?pageSize=1");

		JSONArray itemsJSONArray = responseJSONObject.getJSONArray("items");

		if (itemsJSONArray.isEmpty()) {
			throw new IllegalStateException(
				"No commerce channels found in Liferay; create one first");
		}

		return itemsJSONArray.getJSONObject(
			0
		).getLong(
			"id"
		);
	}

	private void _retryAssert(Duration timeout, RetryableAssertion assertion)
		throws Exception {

		long deadline = System.currentTimeMillis() + timeout.toMillis();

		while (true) {
			try {
				assertion.run();

				return;
			}
			catch (AssertionError assertionError) {
				if (System.currentTimeMillis() > deadline) {
					throw assertionError;
				}

				Thread.sleep(1000);
			}
		}
	}

	private void _safeDelete(String path) {
		try {
			HttpRequest httpRequest = HttpRequest.newBuilder(
				URI.create(_BASE_URL + path)
			).header(
				"Authorization", _basicAuthHeader()
			).DELETE(
			).build();

			_httpClient.send(
				httpRequest, HttpResponse.BodyHandlers.discarding());
		}
		catch (Exception exception) {
		}
	}

	private List<JSONObject> _safeGetEntitlements(long commerceOrderItemId) {
		List<JSONObject> entitlements = new ArrayList<>();

		try {
			String path = StringBundler.concat(
				"/o/c/entitlements?filter=",
				"r_commerceOrderItemToEntitlement_commerceOrderItemId eq '",
				commerceOrderItemId, "'&pageSize=500");

			JSONObject responseJSONObject = _get(path);

			JSONArray itemsJSONArray = responseJSONObject.getJSONArray("items");

			for (int i = 0; i < itemsJSONArray.length(); i++) {
				entitlements.add(itemsJSONArray.getJSONObject(i));
			}
		}
		catch (Exception exception) {
		}

		return entitlements;
	}

	private static final String _BASE_URL = System.getenv("LIFERAY_URL");

	private static final String _RUN_ID = String.valueOf(
		System.currentTimeMillis());

	private long _accountEntryId;
	private long _commerceOrderId;
	private long _commerceOrderItemId;
	private long _contractId;
	private long _cProductId;
	private final List<Long> _entitlementDefinitionIds = new ArrayList<>();
	private final HttpClient _httpClient = HttpClient.newBuilder(
	).connectTimeout(
		Duration.ofSeconds(10)
	).build();
	private long _skuId;

	@FunctionalInterface
	private interface RetryableAssertion {

		public void run() throws Exception;

	}

}