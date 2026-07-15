/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.jira.service;

import com.liferay.one.jira.exception.JiraAssetSchemaException;
import com.liferay.petra.string.StringBundler;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Drew Brokke
 */
@Component
public class AssetSchemaService {

	public Map<String, String> getAttributeIds(
		String schemaName, String objectTypeName) {

		return _assetSchemaLoader.getAttributeIds(
			getObjectTypeId(schemaName, objectTypeName));
	}

	public Map<String, Set<String>> getAttributeOptions(
		String schemaName, String objectTypeName) {

		return _assetSchemaLoader.getAttributeOptions(
			getObjectTypeId(schemaName, objectTypeName));
	}

	public String getObjectTypeId(String schemaName, String objectTypeName) {
		Map<String, String> objectTypeIds = _assetSchemaLoader.getObjectTypeIds(
			schemaName);

		String objectTypeId = objectTypeIds.get(objectTypeName);

		if (objectTypeId == null) {
			throw new JiraAssetSchemaException(
				StringBundler.concat(
					"Object type \"", objectTypeName,
					"\" not found in schema \"", schemaName,
					"\". Available object types: ",
					new TreeSet<>(objectTypeIds.keySet())));
		}

		return objectTypeId;
	}

	@Autowired
	private AssetSchemaLoader _assetSchemaLoader;

}