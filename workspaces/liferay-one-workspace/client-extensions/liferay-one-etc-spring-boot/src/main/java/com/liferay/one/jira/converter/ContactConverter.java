/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.jira.converter;

import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.one.jira.constants.ContactConstants;
import com.liferay.one.jira.model.JiraAssetObject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Drew Brokke
 */
@Component
public class ContactConverter extends BaseAssetObjectConverter {

	@Override
	public String getObjectTypeName() {
		return ContactConstants.OBJECT_TYPE_NAME;
	}

	public JiraAssetObject toAssetObject(UserAccount userAccount) {
		JiraAssetObject jiraAssetObject = createJiraAssetObject();

		jiraAssetObject.setAttributeValue(
			ContactConstants.ATTRIBUTE_NAME_EXTERNAL_KEY,
			userAccount.getExternalReferenceCode());
		jiraAssetObject.setAttributeValue(
			ContactConstants.ATTRIBUTE_NAME_NAME, userAccount.getName());
		jiraAssetObject.setAttributeValue(
			ContactConstants.ATTRIBUTE_NAME_FIRST_NAME,
			userAccount.getGivenName());
		jiraAssetObject.setAttributeValue(
			ContactConstants.ATTRIBUTE_NAME_MIDDLE_NAME,
			userAccount.getAdditionalName());
		jiraAssetObject.setAttributeValue(
			ContactConstants.ATTRIBUTE_NAME_LAST_NAME,
			userAccount.getFamilyName());
		jiraAssetObject.setAttributeValue(
			ContactConstants.ATTRIBUTE_NAME_EMAIL_ADDRESS,
			userAccount.getEmailAddress());
		jiraAssetObject.setAttributeValue(
			ContactConstants.ATTRIBUTE_NAME_LANGUAGE_ID,
			userAccount.getLanguageId());
		jiraAssetObject.setAttributeValue(
			ContactConstants.ATTRIBUTE_NAME_EXTERNAL_CREATED_AT,
			formatDate(userAccount.getDateCreated()));
		jiraAssetObject.setAttributeValue(
			ContactConstants.ATTRIBUTE_NAME_EXTERNAL_UPDATED_AT,
			formatDate(userAccount.getDateModified()));

		return jiraAssetObject;
	}

	@Override
	protected String getObjectSchemaName() {
		return _schemaName;
	}

	@Value("${liferay.one.jira.asset.schema.name}")
	private String _schemaName;

}