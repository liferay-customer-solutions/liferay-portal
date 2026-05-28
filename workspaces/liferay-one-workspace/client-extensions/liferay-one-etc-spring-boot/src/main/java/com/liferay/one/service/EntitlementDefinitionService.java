/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.client.extension.util.spring.boot3.client.LiferayOAuth2AccessTokenManager;
import com.liferay.client.extension.util.spring.boot3.service.BaseService;
import com.liferay.one.model.EntitlementDefinition;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Felipe Veloso
 */
@Component
public class EntitlementDefinitionService extends BaseService {

	public List<EntitlementDefinition> getEntitlementDefinitions(
			String filterString)
		throws Exception {

		UriComponentsBuilder uriComponentsBuilder =
			UriComponentsBuilder.fromPath(
				"/o/c/entitlementdefinitions"
			).queryParam(
				"pageSize", "500"
			);

		if (filterString != null) {
			uriComponentsBuilder.queryParam("filter", filterString);
		}

		String response = get(
			_getAuthorization(),
			uriComponentsBuilder.build(
			).toUri());

		List<EntitlementDefinition> entitlementDefinitions = new ArrayList<>();

		if (Validator.isNull(response)) {
			return entitlementDefinitions;
		}

		try {
			JSONObject jsonObject = new JSONObject(response);

			JSONArray jsonArray = jsonObject.getJSONArray("items");

			for (int i = 0; i < jsonArray.length(); i++) {
				entitlementDefinitions.add(
					new EntitlementDefinition(jsonArray.getJSONObject(i)));
			}

			return entitlementDefinitions;
		}
		catch (Exception exception) {
			_log.error("Unable to parse JSON: " + response, exception);

			return entitlementDefinitions;
		}
	}

	private String _getAuthorization() {
		return _liferayOAuth2AccessTokenManager.getAuthorization(
			"liferay-one-etc-spring-boot-oahs");
	}

	private static final Log _log = LogFactory.getLog(
		EntitlementDefinitionService.class);

	@Autowired
	private LiferayOAuth2AccessTokenManager _liferayOAuth2AccessTokenManager;

}