/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.jira.converter;

import com.liferay.one.jira.constants.AccountContactRoleAssignmentConstants;
import com.liferay.one.jira.model.JiraAssetObject;
import com.liferay.petra.string.StringBundler;

import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Drew Brokke
 */
@Component
public class AccountContactRoleAssignmentConverter
	extends BaseJiraAssetObjectConverter {

	@Override
	public String getDeletedAttributeName() {
		return AccountContactRoleAssignmentConstants.ATTRIBUTE_NAME_DELETED;
	}

	@Override
	public String getExternalKeyAttributeName() {
		return AccountContactRoleAssignmentConstants.ATTRIBUTE_NAME_NAME;
	}

	public String getName(
		String accountExternalKey, String contactExternalKey,
		String contactRoleExternalKey) {

		return StringBundler.concat(
			contactRoleExternalKey, ";", contactExternalKey, ";",
			accountExternalKey);
	}

	@Override
	public String getObjectTypeName() {
		return AccountContactRoleAssignmentConstants.OBJECT_TYPE_NAME;
	}

	public JiraAssetObject toAssetObject(
		String contactRoleExternalKey, String contactExternalKey,
		String accountExternalKey, boolean deleted, Date externalUpdatedAt) {

		JiraAssetObject jiraAssetObject = createJiraAssetObject();

		jiraAssetObject.setAttributeValue(
			AccountContactRoleAssignmentConstants.ATTRIBUTE_NAME_NAME,
			getName(
				accountExternalKey, contactExternalKey,
				contactRoleExternalKey));
		jiraAssetObject.setAttributeValue(
			AccountContactRoleAssignmentConstants.
				ATTRIBUTE_NAME_CONTACT_ROLE_EXTERNAL_KEY,
			contactRoleExternalKey);
		jiraAssetObject.setAttributeValue(
			AccountContactRoleAssignmentConstants.
				ATTRIBUTE_NAME_CONTACT_EXTERNAL_KEY,
			contactExternalKey);
		jiraAssetObject.setAttributeValue(
			AccountContactRoleAssignmentConstants.
				ATTRIBUTE_NAME_ACCOUNT_EXTERNAL_KEY,
			accountExternalKey);
		jiraAssetObject.setAttributeValue(
			AccountContactRoleAssignmentConstants.ATTRIBUTE_NAME_DELETED,
			deleted);
		jiraAssetObject.setAttributeValue(
			AccountContactRoleAssignmentConstants.
				ATTRIBUTE_NAME_EXTERNAL_UPDATED_AT,
			formatDate(externalUpdatedAt));

		return jiraAssetObject;
	}

	@Override
	protected String getObjectSchemaName() {
		return _schemaName;
	}

	@Value("${liferay.one.jira.asset.schema.name}")
	private String _schemaName;

}