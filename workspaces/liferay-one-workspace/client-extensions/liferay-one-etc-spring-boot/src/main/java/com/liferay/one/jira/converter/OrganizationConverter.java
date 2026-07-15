/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.jira.converter;

import com.liferay.one.jira.constants.AccountConstants;
import com.liferay.one.jira.model.JiraAssetObject;
import com.liferay.one.jira.model.Organization;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Felipe Franca
 */
@Component
public class OrganizationConverter extends BaseAssetObjectConverter {

	@Override
	public String getObjectTypeName() {
		return AccountConstants.OBJECT_TYPE_NAME;
	}

	public Organization toOrganization(JSONObject assetObjectJSONObject) {
		JiraAssetObject jiraAssetObject = toJiraAssetObject(
			assetObjectJSONObject);

		return new Organization(
			jiraAssetObject.getAttributeValue(
				AccountConstants.ATTRIBUTE_NAME_EXTERNAL_KEY),
			jiraAssetObject.getObjectId(), jiraAssetObject.getObjectName());
	}

	@Override
	protected String getObjectSchemaName() {
		return _schemaName;
	}

	@Value("${liferay.one.jira.asset.schema.name}")
	private String _schemaName;

}