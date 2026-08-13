/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.jira.converter;

import com.liferay.one.jira.constants.AccountTeamRoleAssignmentConstants;
import com.liferay.one.jira.model.JiraAssetObject;
import com.liferay.petra.string.StringBundler;

import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Drew Brokke
 */
@Component
public class AccountTeamRoleAssignmentConverter
	extends BaseJiraAssetObjectConverter {

	@Override
	public String getDeletedAttributeName() {
		return AccountTeamRoleAssignmentConstants.ATTRIBUTE_NAME_DELETED;
	}

	@Override
	public String getExternalKeyAttributeName() {
		return AccountTeamRoleAssignmentConstants.ATTRIBUTE_NAME_NAME;
	}

	@Override
	public String getObjectTypeName() {
		return AccountTeamRoleAssignmentConstants.OBJECT_TYPE_NAME;
	}

	public JiraAssetObject toAssetObject(
		String teamRoleExternalKey, String teamExternalKey,
		String accountExternalKey, boolean deleted, Date externalUpdatedAt) {

		JiraAssetObject jiraAssetObject = createJiraAssetObject();

		jiraAssetObject.setAttributeValue(
			AccountTeamRoleAssignmentConstants.ATTRIBUTE_NAME_NAME,
			_getName(accountExternalKey, teamExternalKey, teamRoleExternalKey));
		jiraAssetObject.setAttributeValue(
			AccountTeamRoleAssignmentConstants.
				ATTRIBUTE_NAME_TEAM_ROLE_EXTERNAL_KEY,
			teamRoleExternalKey);
		jiraAssetObject.setAttributeValue(
			AccountTeamRoleAssignmentConstants.ATTRIBUTE_NAME_TEAM_EXTERNAL_KEY,
			teamExternalKey);
		jiraAssetObject.setAttributeValue(
			AccountTeamRoleAssignmentConstants.
				ATTRIBUTE_NAME_ACCOUNT_EXTERNAL_KEY,
			accountExternalKey);
		jiraAssetObject.setAttributeValue(
			AccountTeamRoleAssignmentConstants.ATTRIBUTE_NAME_DELETED, deleted);
		jiraAssetObject.setAttributeValue(
			AccountTeamRoleAssignmentConstants.
				ATTRIBUTE_NAME_EXTERNAL_UPDATED_AT,
			formatDate(externalUpdatedAt));

		return jiraAssetObject;
	}

	@Override
	protected String getObjectSchemaName() {
		return _schemaName;
	}

	private String _getName(
		String accountExternalKey, String teamExternalKey,
		String teamRoleExternalKey) {

		return StringBundler.concat(
			teamRoleExternalKey, ";", teamExternalKey, ";", accountExternalKey);
	}

	@Value("${liferay.one.jira.asset.schema.name}")
	private String _schemaName;

}