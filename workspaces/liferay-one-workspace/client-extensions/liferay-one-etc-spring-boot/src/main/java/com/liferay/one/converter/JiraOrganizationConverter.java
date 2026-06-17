/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.converter;

import com.liferay.one.model.JiraOrganization;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Felipe Franca
 */
@Component
public class JiraOrganizationConverter extends JiraAssetObjectConverter {

	public JiraOrganization toJiraOrganization(
		JSONObject jiraAssetObjectJSONObject) {

		return new JiraOrganization(
			getAttributeValue(
				_externalKeyAttributeId, jiraAssetObjectJSONObject),
			jiraAssetObjectJSONObject.optString("id"),
			jiraAssetObjectJSONObject.optString("name"));
	}

	@Value(
		"${liferay.one.jira.organization.asset.object.type.attribute.external.key}"
	)
	private String _externalKeyAttributeId;

}