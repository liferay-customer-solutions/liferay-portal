/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.IdTokenCredentials;
import com.google.auth.oauth2.IdTokenProvider;

import com.liferay.client.extension.util.spring.boot3.service.BaseService;
import com.liferay.one.exception.GoogleCloudFunctionUnavailableException;

import java.net.URI;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Felipe Veloso
 */
@Component
public class GoogleCloudFunctionService extends BaseService {

	@Cacheable("composableAccountUsage")
	public String fetchComposableAccountUsage(String accountKey, String month)
		throws Exception {

		return _handleRequest(
			accountKey, _gcfBaseURL + _FUNCTION_PATH_COMPOSABLE_USAGE_API,
			UriComponentsBuilder.fromUriString(
				_gcfBaseURL
			).path(
				_FUNCTION_PATH_COMPOSABLE_USAGE_API +
					"/api/v1/accounts/{accountKey}/usage/month/{month}"
			).buildAndExpand(
				accountKey, month
			).toUri());
	}

	@Cacheable("customerAccountUsage")
	public String fetchCustomerAccountUsage(String accountKey)
		throws Exception {

		return _handleRequest(
			accountKey, _gcfBaseURL + _FUNCTION_PATH_CUSTOMER_USAGE_API,
			UriComponentsBuilder.fromUriString(
				_gcfBaseURL
			).path(
				_FUNCTION_PATH_CUSTOMER_USAGE_API +
					"/api/v1/customer/usage/accounts/{accountKey}"
			).buildAndExpand(
				accountKey
			).toUri());
	}

	private String _getAuthorization(String audience) throws Exception {
		IdTokenCredentials idTokenCredentials = _getIdTokenCredentials(
			audience);

		idTokenCredentials.refreshIfExpired();

		AccessToken accessToken = idTokenCredentials.getAccessToken();

		if (accessToken == null) {
			throw new Exception(
				"Unable to get access token for audience " + audience);
		}

		return "Bearer " + accessToken.getTokenValue();
	}

	private IdTokenCredentials _getIdTokenCredentials(String audience)
		throws Exception {

		IdTokenCredentials idTokenCredentials = _idTokenCredentials.get(
			audience);

		if (idTokenCredentials != null) {
			return idTokenCredentials;
		}

		idTokenCredentials = IdTokenCredentials.newBuilder(
		).setIdTokenProvider(
			(IdTokenProvider)GoogleCredentials.getApplicationDefault()
		).setTargetAudience(
			audience
		).build();

		IdTokenCredentials previousIdTokenCredentials =
			_idTokenCredentials.putIfAbsent(audience, idTokenCredentials);

		if (previousIdTokenCredentials != null) {
			return previousIdTokenCredentials;
		}

		return idTokenCredentials;
	}

	private String _handleRequest(String accountKey, String audience, URI uri)
		throws Exception {

		String authorization = null;

		try {
			authorization = _getAuthorization(audience);
		}
		catch (Exception exception) {
			throw new GoogleCloudFunctionUnavailableException(
				"Unable to authenticate to DataOps for account " + accountKey,
				exception);
		}

		try {
			return get(authorization, uri);
		}
		catch (WebClientResponseException webClientResponseException) {
			if (webClientResponseException.getStatusCode() ==
					HttpStatus.NOT_FOUND) {

				if (_log.isInfoEnabled()) {
					_log.info(
						"No DataOps usage data for account " + accountKey);
				}

				return null;
			}

			throw new GoogleCloudFunctionUnavailableException(
				"Unable to read DataOps usage for account " + accountKey,
				webClientResponseException);
		}
		catch (WebClientException webClientException) {
			throw new GoogleCloudFunctionUnavailableException(
				"Unable to reach DataOps for account " + accountKey,
				webClientException);
		}
	}

	private static final String _FUNCTION_PATH_COMPOSABLE_USAGE_API =
		"/composable_usage_api";

	private static final String _FUNCTION_PATH_CUSTOMER_USAGE_API =
		"/customer_usage_api";

	private static final Log _log = LogFactory.getLog(
		GoogleCloudFunctionService.class);

	@Value("${liferay.one.gcf.base.url}")
	private String _gcfBaseURL;

	private final Map<String, IdTokenCredentials> _idTokenCredentials =
		new ConcurrentHashMap<>();

}