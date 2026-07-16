/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.jira.converter;

import com.liferay.one.jira.constants.ExternalLinkConstants;
import com.liferay.one.jira.model.JiraAssetObject;
import com.liferay.one.model.Property;
import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringUtil;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Drew Brokke
 */
@Component
public class ExternalLinkConverter extends BaseAssetObjectConverter {

	@Override
	public String getObjectTypeName() {
		return ExternalLinkConstants.OBJECT_TYPE_NAME;
	}

	public JiraAssetObject toAssetObject(Property property) {
		JiraAssetObject jiraAssetObject = createJiraAssetObject();

		List<String> parts = StringUtil.split(
			property.getName(), CharPool.COLON);

		if (parts.size() != 2) {
			return null;
		}

		String domain = parts.get(0);
		String entityName = parts.get(1);

		jiraAssetObject.setAttributeValue(
			ExternalLinkConstants.ATTRIBUTE_NAME_NAME, property.getClassName());
		jiraAssetObject.setAttributeValue(
			ExternalLinkConstants.ATTRIBUTE_NAME_EXTERNAL_KEY,
			property.getExternalReferenceCode());
		jiraAssetObject.setAttributeValue(
			ExternalLinkConstants.ATTRIBUTE_NAME_DOMAIN, domain);
		jiraAssetObject.setAttributeValue(
			ExternalLinkConstants.ATTRIBUTE_NAME_ENTITY_ID,
			property.getValue());
		jiraAssetObject.setAttributeValue(
			ExternalLinkConstants.ATTRIBUTE_NAME_ENTITY_NAME, entityName);

		return jiraAssetObject;
	}

	@Override
	protected String getObjectSchemaName() {
		return _schemaName;
	}

	@Value("${liferay.one.jira.asset.schema.name}")
	private String _schemaName;

}