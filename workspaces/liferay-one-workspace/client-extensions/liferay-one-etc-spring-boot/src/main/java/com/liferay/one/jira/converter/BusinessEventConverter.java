/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.jira.converter;

import com.liferay.one.jira.constants.BusinessEventConstants;
import com.liferay.one.jira.model.BusinessEvent;
import com.liferay.one.jira.model.JiraAssetObject;
import com.liferay.petra.string.StringPool;

import org.json.JSONObject;

import org.springframework.stereotype.Component;

/**
 * @author Amos Fong
 */
@Component
public class BusinessEventConverter extends BaseAssetObjectConverter {

	@Override
	public String getObjectTypeName() {
		return BusinessEventConstants.OBJECT_TYPE_BUSINESS_EVENT;
	}

	public JiraAssetObject toAssetObject(
		String accountObjectKey, BusinessEvent businessEvent) {

		JiraAssetObject jiraAssetObject = createJiraAssetObject();

		jiraAssetObject.setAttributeValue(
			BusinessEventConstants.ATTRIBUTE_NAME_ACTUAL_EVENT_DATE,
			businessEvent.getActualEventDate());
		jiraAssetObject.setAttributeValue(
			BusinessEventConstants.ATTRIBUTE_NAME_ASSOCIATED_TICKETS,
			businessEvent.getAssociatedTickets());
		jiraAssetObject.setAttributeValue(
			BusinessEventConstants.ATTRIBUTE_NAME_CURRENT_VERSION,
			businessEvent.getCurrentLiferayVersionKey());
		jiraAssetObject.setAttributeValue(
			BusinessEventConstants.ATTRIBUTE_NAME_DESCRIPTION,
			businessEvent.getDescription());
		jiraAssetObject.setAttributeValue(
			BusinessEventConstants.ATTRIBUTE_NAME_EVENT_STATUS,
			businessEvent.getEventStatusName());
		jiraAssetObject.setAttributeValue(
			BusinessEventConstants.ATTRIBUTE_NAME_EVENT_TYPE,
			businessEvent.getEventTypeName());
		jiraAssetObject.setAttributeValue(
			BusinessEventConstants.ATTRIBUTE_NAME_LAST_COMMENT,
			businessEvent.getLastComment());
		jiraAssetObject.setAttributeValue(
			BusinessEventConstants.ATTRIBUTE_NAME_LAST_UPDATED_AUTHOR,
			businessEvent.getLastUpdatedAuthorEmailAddress());
		jiraAssetObject.setAttributeValue(
			BusinessEventConstants.ATTRIBUTE_NAME_NAME,
			businessEvent.getName());
		jiraAssetObject.setAttributeValue(
			BusinessEventConstants.ATTRIBUTE_NAME_NEW_VERSION,
			businessEvent.getNewLiferayVersionKey());
		jiraAssetObject.setAttributeValue(
			BusinessEventConstants.ATTRIBUTE_NAME_PLANNED_EVENT_DATE,
			businessEvent.getPlannedEventDate());
		jiraAssetObject.setAttributeValue(
			BusinessEventConstants.ATTRIBUTE_NAME_TIME_ZONE,
			businessEvent.getTimeZoneName());

		if (accountObjectKey != null) {
			jiraAssetObject.setAttributeValue(
				BusinessEventConstants.ATTRIBUTE_NAME_ACCOUNT,
				accountObjectKey);
			jiraAssetObject.setAttributeValue(
				BusinessEventConstants.ATTRIBUTE_NAME_AUTHOR,
				businessEvent.getAuthorEmailAddress());
		}

		return jiraAssetObject;
	}

	public BusinessEvent toBusinessEvent(
		JSONObject jiraAssetObjectJSONObject,
		String projectExternalReferenceCode) {

		JiraAssetObject jiraAssetObject = toJiraAssetObject(
			jiraAssetObjectJSONObject);

		return new BusinessEvent(
			jiraAssetObject.getAttributeValue(
				BusinessEventConstants.ATTRIBUTE_NAME_ACTUAL_EVENT_DATE),
			jiraAssetObject.getAttributeDisplayValue(
				BusinessEventConstants.ATTRIBUTE_NAME_ASSOCIATED_TICKETS),
			jiraAssetObject.getAttributeDisplayValue(
				BusinessEventConstants.ATTRIBUTE_NAME_AUTHOR),
			jiraAssetObject.getObjectId(),
			jiraAssetObject.getAttributeValue(
				BusinessEventConstants.ATTRIBUTE_NAME_CURRENT_VERSION),
			jiraAssetObject.getAttributeDisplayValue(
				BusinessEventConstants.ATTRIBUTE_NAME_CURRENT_VERSION),
			jiraAssetObject.getAttributeDisplayValue(
				BusinessEventConstants.ATTRIBUTE_NAME_DESCRIPTION),
			jiraAssetObject.getAttributeDisplayValue(
				BusinessEventConstants.ATTRIBUTE_NAME_EVENT_STATUS),
			jiraAssetObject.getAttributeDisplayValue(
				BusinessEventConstants.ATTRIBUTE_NAME_EVENT_TYPE),
			jiraAssetObject.getAttributeDisplayValue(
				BusinessEventConstants.ATTRIBUTE_NAME_LAST_COMMENT),
			jiraAssetObject.getAttributeDisplayValue(
				BusinessEventConstants.ATTRIBUTE_NAME_LAST_UPDATED_AUTHOR),
			jiraAssetObject.getAttributeDisplayValue(
				BusinessEventConstants.ATTRIBUTE_NAME_NAME),
			jiraAssetObject.getAttributeValue(
				BusinessEventConstants.ATTRIBUTE_NAME_NEW_VERSION),
			jiraAssetObject.getAttributeDisplayValue(
				BusinessEventConstants.ATTRIBUTE_NAME_NEW_VERSION),
			jiraAssetObject.getAttributeValue(
				BusinessEventConstants.ATTRIBUTE_NAME_PLANNED_EVENT_DATE),
			projectExternalReferenceCode,
			jiraAssetObject.getAttributeDisplayValue(
				BusinessEventConstants.ATTRIBUTE_NAME_TIME_ZONE));
	}

	public BusinessEvent toBusinessEvent(
		String attributesJSON, String authorEmailAddress,
		String projectExternalReferenceCode) {

		JSONObject attributesJSONObject = new JSONObject(attributesJSON);

		return new BusinessEvent(
			attributesJSONObject.optString("actualEventDate"),
			attributesJSONObject.optString("associatedTickets"),
			authorEmailAddress, StringPool.BLANK,
			attributesJSONObject.optString("currentLiferayVersion"),
			StringPool.BLANK, attributesJSONObject.optString("description"),
			attributesJSONObject.optString("eventStatus"),
			attributesJSONObject.optString("eventType"),
			attributesJSONObject.optString("lastComment"), authorEmailAddress,
			attributesJSONObject.optString("name"),
			attributesJSONObject.optString("newLiferayVersion"),
			StringPool.BLANK,
			attributesJSONObject.optString("plannedEventDate"),
			projectExternalReferenceCode,
			attributesJSONObject.optString("timeZone"));
	}

	@Override
	protected String getObjectSchemaName() {
		return BusinessEventConstants.OBJECT_SCHEMA_BUSINESS_EVENTS;
	}

}