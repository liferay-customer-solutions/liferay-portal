/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.one.model.ProductVersion;
import com.liferay.one.util.comparator.VersionComparator;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.net.URI;

import java.time.Duration;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import reactor.util.retry.Retry;

/**
 * @author Allen Ziegenfus
 */
@Component
public class ProductVersionService extends OneBaseService {

	public String getLatestProductVersion(String productGroup)
		throws Exception {

		String latestVersion = null;

		VersionComparator versionComparator = new VersionComparator();

		for (ProductVersion productVersion :
				getProductVersions(productGroup, true)) {

			String version = productVersion.getVersion();

			if ((latestVersion == null) ||
				(versionComparator.compare(version, latestVersion) > 0)) {

				latestVersion = version;
			}
		}

		return latestVersion;
	}

	public ProductVersion getProductVersion(String productGroup, String version)
		throws Exception {

		List<ProductVersion> productVersions = _getProductVersions(
			StringBundler.concat(
				"(productGroup eq '", productGroup,
				"') and (productVersion eq '", version, "')"));

		if (productVersions.isEmpty()) {
			return null;
		}

		return productVersions.get(0);
	}

	public List<ProductVersion> getProductVersions(String productGroup)
		throws Exception {

		return _getProductVersions(
			StringBundler.concat("productGroup eq '", productGroup, "'"));
	}

	public List<ProductVersion> getProductVersions(
			String productGroup, boolean supported)
		throws Exception {

		return _getProductVersions(
			StringBundler.concat(
				"(productGroup eq '", productGroup, "') and (supported eq ",
				supported, ")"));
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReady() {
		try {
			syncProductVersions();
		}
		catch (Exception exception) {
			_log.error(
				"Unable to sync product versions on application startup",
				exception);
		}
	}

	@Scheduled(cron = "${liferay.one.product.version.sync.cron}")
	public void syncProductVersions() throws Exception {
		if (_log.isInfoEnabled()) {
			_log.info("Syncing product versions from " + _releasesURL);
		}

		JSONArray releasesJSONArray = new JSONArray(
			get(
				StringPool.BLANK,
				UriComponentsBuilder.fromUriString(
					_releasesURL
				).build(
				).toUri()));

		if (_log.isInfoEnabled()) {
			_log.info("Fetched " + releasesJSONArray.length() + " releases");
		}

		_waitUntilReady(getAuthorization());

		for (String productGroup : _productGroups) {
			_syncProductGroup(productGroup, releasesJSONArray);
		}
	}

	private List<ProductVersion> _getProductVersions(String filterString)
		throws Exception {

		return getAllItems(
			"/o/c/productversions", filterString, ProductVersion::new);
	}

	private boolean _isRetryable(Throwable throwable) {
		if (throwable instanceof WebClientResponseException) {
			WebClientResponseException webClientResponseException =
				(WebClientResponseException)throwable;

			int statusCode = webClientResponseException.getStatusCode(
			).value();

			if ((statusCode == 401) || (statusCode == 403)) {
				return false;
			}
		}

		return true;
	}

	private boolean _isSupported(JSONArray tagsJSONArray) {
		if (tagsJSONArray == null) {
			return false;
		}

		for (int i = 0; i < tagsJSONArray.length(); i++) {
			if (StringUtil.equals(tagsJSONArray.optString(i), "supported")) {
				return true;
			}
		}

		return false;
	}

	private void _syncProductGroup(
			String productGroup, JSONArray releasesJSONArray)
		throws Exception {

		Map<String, JSONObject> productVersionJSONObjects = new TreeMap<>();

		for (int i = 0; i < releasesJSONArray.length(); i++) {
			JSONObject releaseJSONObject = releasesJSONArray.getJSONObject(i);

			String productGroupVersion = releaseJSONObject.optString(
				"productGroupVersion");

			if (!StringUtil.equals(
					releaseJSONObject.optString("product"), productGroup) ||
				Validator.isNull(productGroupVersion)) {

				continue;
			}

			boolean supported = _isSupported(
				releaseJSONObject.optJSONArray("tags"));

			String version = releaseJSONObject.optString("productMajorVersion");

			if (Validator.isNull(version)) {
				version = StringBundler.concat(
					StringUtil.toUpperCase(productGroup), StringPool.SPACE,
					StringUtil.toUpperCase(productGroupVersion));
			}

			JSONObject productVersionJSONObject = productVersionJSONObjects.get(
				version);

			if (productVersionJSONObject != null) {
				if (supported) {
					productVersionJSONObject.put("supported", true);
				}

				continue;
			}

			productVersionJSONObjects.put(
				version,
				new JSONObject(
				).put(
					"externalReferenceCode", version
				).put(
					"productGroup", productGroup
				).put(
					"productVersion", version
				).put(
					"supported", supported
				));
		}

		if (_log.isInfoEnabled()) {
			_log.info(
				StringBundler.concat(
					"Syncing ", productVersionJSONObjects.size(),
					" product versions for product group ", productGroup));
		}

		String authorization = getAuthorization();

		if (Validator.isNull(authorization)) {
			_log.error(
				StringBundler.concat(
					"Unable to sync product versions for product group ",
					productGroup, ": no authorization token was obtained"));

			return;
		}

		int count = 0;

		for (JSONObject productVersionJSONObject :
				productVersionJSONObjects.values()) {

			String externalReferenceCode = productVersionJSONObject.getString(
				"externalReferenceCode");
			String version = productVersionJSONObject.getString(
				"productVersion");

			URI uri = UriComponentsBuilder.fromPath(
				"/o/c/productversions/by-external-reference-code/" +
					externalReferenceCode
			).build(
			).encode(
			).toUri();

			if (_log.isDebugEnabled()) {
				_log.debug(
					StringBundler.concat(
						"Syncing product version ", version, " to ", uri,
						" with payload ", productVersionJSONObject));
			}

			try {
				String response = put(
					authorization, productVersionJSONObject.toString(), uri);

				if ((response != null) &&
					!new JSONObject(
						response).isNull(
							"id"
						)) {

					count++;
				}
				else {
					_log.error(
						StringBundler.concat(
							"Unable to sync product version ", version, " to ",
							uri, ": unexpected response ", response));
				}
			}
			catch (WebClientResponseException webClientResponseException) {
				_log.error(
					StringBundler.concat(
						"Unable to sync product version ", version, " to ", uri,
						": ", webClientResponseException.getStatusCode(), " ",
						webClientResponseException.getResponseBodyAsString()));
			}
			catch (Exception exception) {
				_log.error(
					"Unable to sync product version " + version, exception);
			}
		}

		if (_log.isInfoEnabled()) {
			_log.info(
				StringBundler.concat(
					"Synced ", count, " product versions for product group ",
					productGroup));
		}
	}

	private void _waitUntilReady(String authorization) {
		if (_log.isInfoEnabled()) {
			_log.info(
				"Waiting for the product version object endpoint to be ready");
		}

		URI uri = UriComponentsBuilder.fromPath(
			"/o/c/productversions"
		).queryParam(
			"pageSize", 1
		).build(
		).toUri();

		Mono.fromCallable(
			() -> get(authorization, uri)
		).retryWhen(
			Retry.backoff(
				_READINESS_MAX_RETRIES, Duration.ofSeconds(2)
			).maxBackoff(
				Duration.ofSeconds(20)
			).scheduler(
				Schedulers.boundedElastic()
			).filter(
				this::_isRetryable
			).onRetryExhaustedThrow(
				(retryBackoffSpec, retrySignal) -> retrySignal.failure()
			)
		).block();
	}

	private static final long _READINESS_MAX_RETRIES = 6;

	private static final Log _log = LogFactory.getLog(
		ProductVersionService.class);

	@Value("${liferay.one.product.version.sync.product.groups}")
	private String[] _productGroups;

	@Value("${liferay.one.product.version.sync.releases.url}")
	private String _releasesURL;

}