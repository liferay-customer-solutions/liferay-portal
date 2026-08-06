/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.client.extension.util.spring.boot3.client.LiferayOAuth2AccessTokenManager;
import com.liferay.client.extension.util.spring.boot3.service.BaseService;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
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

	protected boolean isNotFound(String status) {
		return Objects.equals(HttpStatus.NOT_FOUND.name(), status);
	}

	private static final int _PAGE_SIZE = 500;

	@Autowired
	private LiferayOAuth2AccessTokenManager _liferayOAuth2AccessTokenManager;

}