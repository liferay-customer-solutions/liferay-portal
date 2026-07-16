/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.jira.converter;

import com.liferay.headless.admin.user.client.dto.v1_0.Organization;
import com.liferay.one.jira.constants.TeamConstants;
import com.liferay.one.jira.model.JiraAssetObject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Drew Brokke
 */
@Component
public class TeamConverter extends BaseAssetObjectConverter {

	@Override
	public String getObjectTypeName() {
		return TeamConstants.OBJECT_TYPE_NAME;
	}

	public JiraAssetObject toAssetObject(Organization organization) {
		JiraAssetObject jiraAssetObject = createJiraAssetObject();

		jiraAssetObject.setAttributeValue(
			TeamConstants.ATTRIBUTE_NAME_EXTERNAL_KEY,
			organization.getExternalReferenceCode());
		jiraAssetObject.setAttributeValue(
			TeamConstants.ATTRIBUTE_NAME_NAME, organization.getName());
		jiraAssetObject.setAttributeValue(
			TeamConstants.ATTRIBUTE_NAME_EXTERNAL_CREATED_AT,
			formatDate(organization.getDateCreated()));
		jiraAssetObject.setAttributeValue(
			TeamConstants.ATTRIBUTE_NAME_EXTERNAL_UPDATED_AT,
			formatDate(organization.getDateModified()));

		return jiraAssetObject;
	}

	@Override
	protected String getObjectSchemaName() {
		return _schemaName;
	}

	@Value("${liferay.one.jira.asset.schema.name}")
	private String _schemaName;

}