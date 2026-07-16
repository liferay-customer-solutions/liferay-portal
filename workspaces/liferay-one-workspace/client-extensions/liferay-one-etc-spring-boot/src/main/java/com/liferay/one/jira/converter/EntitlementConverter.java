/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.jira.converter;

import com.liferay.one.jira.constants.EntitlementConstants;
import com.liferay.one.jira.model.JiraAssetObject;
import com.liferay.one.model.EntitlementDefinition;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Drew Brokke
 */
@Component
public class EntitlementConverter extends BaseAssetObjectConverter {

	@Override
	public String getExternalKeyAttributeName() {
		return EntitlementConstants.ATTRIBUTE_NAME_NAME;
	}

	@Override
	public String getObjectTypeName() {
		return EntitlementConstants.OBJECT_TYPE_NAME;
	}

	public JiraAssetObject toAssetObject(
		EntitlementDefinition entitlementDefinition) {

		JiraAssetObject jiraAssetObject = createJiraAssetObject();

		jiraAssetObject.setAttributeValue(
			EntitlementConstants.ATTRIBUTE_NAME_NAME,
			entitlementDefinition.getDisplayName());
		jiraAssetObject.setAttributeValue(
			EntitlementConstants.ATTRIBUTE_NAME_ENTITLEMENT_DEFINITION_KEY,
			entitlementDefinition.getExternalReferenceCode());

		return jiraAssetObject;
	}

	@Override
	protected String getObjectSchemaName() {
		return _schemaName;
	}

	@Value("${liferay.one.jira.asset.schema.name}")
	private String _schemaName;

}