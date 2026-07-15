/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.jira.service;

import com.liferay.one.jira.exception.JiraAssetSchemaException;
import com.liferay.portal.kernel.util.Validator;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * Caches and resolves JSM Assets schema metadata to attribute name-to-id and
 * name-to-options maps.
 *
 * @author Drew Brokke
 */
@Component
public class AssetSchemaLoader {

	@Cacheable("assetObjectTypeAttributeIds")
	public Map<String, String> getAttributeIds(String objectTypeId) {
		return _toNameIdMap(
			_jiraAssetService.getObjectTypeAttributes(objectTypeId));
	}

	@Cacheable("assetObjectTypeAttributeOptions")
	public Map<String, Set<String>> getAttributeOptions(String objectTypeId) {
		Map<String, Set<String>> attributeOptions = new LinkedHashMap<>();

		JSONArray attributesJSONArray =
			_jiraAssetService.getObjectTypeAttributes(objectTypeId);

		for (int i = 0; i < attributesJSONArray.length(); i++) {
			JSONObject attributeJSONObject = attributesJSONArray.getJSONObject(
				i);

			String name = attributeJSONObject.optString("name");
			String options = attributeJSONObject.optString("options");

			if (Validator.isNull(name) || Validator.isNull(options)) {
				continue;
			}

			Set<String> optionsSet = new LinkedHashSet<>();

			for (String option : options.split(",")) {
				optionsSet.add(option.trim());
			}

			attributeOptions.put(name, optionsSet);
		}

		return attributeOptions;
	}

	@Cacheable("assetObjectTypeIds")
	public Map<String, String> getObjectTypeIds(String schemaName) {
		String schemaId = _resolveSchemaId(schemaName);

		return _toNameIdMap(_jiraAssetService.getObjectTypes(schemaId));
	}

	private String _resolveSchemaId(String schemaName) {
		JSONArray objectSchemasJSONArray = _jiraAssetService.getObjectSchemas();

		for (int i = 0; i < objectSchemasJSONArray.length(); i++) {
			JSONObject schemaJSONObject = objectSchemasJSONArray.getJSONObject(
				i);

			if (schemaName.equals(schemaJSONObject.optString("name"))) {
				return schemaJSONObject.getString("id");
			}
		}

		throw new JiraAssetSchemaException(
			"Object schema \"" + schemaName + "\" not found");
	}

	private Map<String, String> _toNameIdMap(JSONArray attributesJSONArray) {
		Map<String, String> attributeIds = new LinkedHashMap<>();

		for (int i = 0; i < attributesJSONArray.length(); i++) {
			JSONObject attributeJSONObject = attributesJSONArray.getJSONObject(
				i);

			String name = attributeJSONObject.optString("name");
			String id = attributeJSONObject.optString("id");

			if (Validator.isNull(name) || Validator.isNull(id)) {
				continue;
			}

			attributeIds.put(name, id);
		}

		return attributeIds;
	}

	@Autowired
	private JiraAssetService _jiraAssetService;

}