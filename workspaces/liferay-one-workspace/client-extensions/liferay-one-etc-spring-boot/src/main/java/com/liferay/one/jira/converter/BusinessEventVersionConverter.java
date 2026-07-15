/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.jira.converter;

import com.liferay.one.jira.constants.BusinessEventConstants;
import com.liferay.one.jira.model.BusinessEventVersion;
import com.liferay.one.jira.model.JiraAssetObject;

import org.json.JSONObject;

import org.springframework.stereotype.Component;

/**
 * @author Amos Fong
 */
@Component
public class BusinessEventVersionConverter extends BaseAssetObjectConverter {

	@Override
	public String getObjectTypeName() {
		return BusinessEventConstants.OBJECT_TYPE_BUSINESS_EVENT_VERSION;
	}

	public BusinessEventVersion toBusinessEventVersion(
		JSONObject jiraAssetObjectJSONObject) {

		JiraAssetObject jiraAssetObject = toJiraAssetObject(
			jiraAssetObjectJSONObject);

		return new BusinessEventVersion(
			jiraAssetObject.getAttributeDisplayValue(
				BusinessEventConstants.ATTRIBUTE_NAME_AUTHOR),
			jiraAssetObject.getAttributeDisplayValue(
				BusinessEventConstants.ATTRIBUTE_NAME_CHANGE),
			jiraAssetObject.getAttributeDisplayValue(
				BusinessEventConstants.ATTRIBUTE_NAME_COMMENT),
			jiraAssetObject.getAttributeValue(
				BusinessEventConstants.ATTRIBUTE_NAME_CREATED));
	}

	@Override
	protected String getObjectSchemaName() {
		return BusinessEventConstants.OBJECT_SCHEMA_BUSINESS_EVENTS;
	}

}