/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.client.extension.util.spring.boot3.client.LiferayOAuth2AccessTokenManager;
import com.liferay.client.extension.util.spring.boot3.service.BaseService;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.net.URI;

import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Amos Fong
 */
public abstract class OneBaseService extends BaseService {

	protected <T> List<T> getAllItems(
			String path, String filterString, Function<JSONObject, T> function)
		throws Exception {

		return getAllItems(path, filterString, function, null);
	}

	protected <T> List<T> getAllItems(
			String path, String filterString, Function<JSONObject, T> function,
			Jwt jwt)
		throws Exception {

		List<T> items = new ArrayList<>();

		int page = 1;

		while (true) {
			UriComponentsBuilder uriComponentsBuilder =
				UriComponentsBuilder.fromPath(
					path
				).queryParam(
					"page", page
				).queryParam(
					"pageSize", _PAGE_SIZE
				);

			if (filterString != null) {
				uriComponentsBuilder.queryParam("filter", filterString);
			}

			String response = get(
				getAuthorization(jwt),
				uriComponentsBuilder.build(
				).toUri());

			if (Validator.isNull(response)) {
				return items;
			}

			JSONObject jsonObject = new JSONObject(response);

			JSONArray jsonArray = jsonObject.getJSONArray("items");

			for (int i = 0; i < jsonArray.length(); i++) {
				items.add(function.apply(jsonArray.getJSONObject(i)));
			}

			if (jsonArray.length() < _PAGE_SIZE) {
				return items;
			}

			page++;
		}
	}

	protected String getAuthorization() {
		return _liferayOAuth2AccessTokenManager.getAuthorization(
			"liferay-one-etc-spring-boot-oahs");
	}

	protected String getAuthorization(Jwt jwt) {
		if (jwt == null) {
			return getAuthorization();
		}

		return "Bearer " + jwt.getTokenValue();
	}

	protected String getDXPEndpointAddress() {
		if (lxcDXPMainDomain.contains(":")) {
			return lxcDXPMainDomain;
		}

		int port = 80;

		if (StringUtil.equals(lxcDXPServerProtocol, "https")) {
			port = 443;
		}

		return lxcDXPMainDomain + ":" + port;
	}

	@Override
	protected ExchangeFilterFunction getWebClientExchangeFilterFunction() {
		return super.getWebClientExchangeFilterFunction(
		).andThen(
			(clientRequest, exchangeFunction) -> exchangeFunction.exchange(
				clientRequest
			).doOnNext(
				clientResponse -> {
					int statusCode = clientResponse.statusCode(
					).value();

					if (statusCode == 403) {
						_logForbidden(clientRequest);
					}
				}
			)
		);
	}

	protected boolean isNotFound(String status) {
		return Objects.equals(HttpStatus.NOT_FOUND.name(), status);
	}

	private String _getObjectScope(URI uri) {
		String path = uri.getPath();

		int index = path.indexOf("/o/c/");

		if (index < 0) {
			return null;
		}

		String objectName = path.substring(index + 5);

		int slashIndex = objectName.indexOf('/');

		if (slashIndex >= 0) {
			objectName = objectName.substring(0, slashIndex);
		}

		if (objectName.isEmpty()) {
			return null;
		}

		if (objectName.endsWith("s")) {
			objectName = objectName.substring(0, objectName.length() - 1);
		}

		return "c_" + objectName;
	}

	private String _getTokenScope(String authorization) {
		if ((authorization == null) || !authorization.startsWith("Bearer ")) {
			return "";
		}

		try {
			String token = authorization.substring(7);

			int firstIndex = token.indexOf('.');

			int secondIndex = token.indexOf('.', firstIndex + 1);

			if ((firstIndex < 0) || (secondIndex < 0)) {
				return "";
			}

			String payload = new String(
				Base64.getUrlDecoder(
				).decode(
					token.substring(firstIndex + 1, secondIndex)
				),
				StandardCharsets.UTF_8);

			JSONObject jsonObject = new JSONObject(payload);

			return jsonObject.optString("scope");
		}
		catch (RuntimeException runtimeException) {
			if (_log.isDebugEnabled()) {
				_log.debug("Unable to read the token scope", runtimeException);
			}

			return "";
		}
	}

	private void _logForbidden(ClientRequest clientRequest) {
		if (!_log.isWarnEnabled()) {
			return;
		}

		URI uri = clientRequest.url();

		String tokenScope = _getTokenScope(
			clientRequest.headers(
			).getFirst(
				HttpHeaders.AUTHORIZATION
			));

		String objectScope = _getObjectScope(uri);

		if ((objectScope != null) && !tokenScope.contains(objectScope)) {
			_log.warn(
				StringBundler.concat(
					"Received a 403 from ", uri,
					". The service account token is missing the ", objectScope,
					" scope. The token scopes are ", tokenScope));
		}
		else {
			_log.warn(
				StringBundler.concat(
					"Received a 403 from ", uri, ". The token scopes are ",
					tokenScope));
		}
	}

	private static final int _PAGE_SIZE = 500;

	private static final Log _log = LogFactory.getLog(OneBaseService.class);

	@Autowired
	private LiferayOAuth2AccessTokenManager _liferayOAuth2AccessTokenManager;

}