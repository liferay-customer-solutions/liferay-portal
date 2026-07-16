/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.jira.converter;

import com.liferay.headless.admin.user.client.dto.v1_0.PostalAddress;
import com.liferay.one.jira.constants.PostalAddressConstants;
import com.liferay.one.jira.model.JiraAssetObject;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Drew Brokke
 */
@Component
public class PostalAddressConverter extends BaseAssetObjectConverter {

	@Override
	public String getExternalKeyAttributeName() {
		return PostalAddressConstants.ATTRIBUTE_NAME_ID;
	}

	@Override
	public String getObjectTypeName() {
		return PostalAddressConstants.OBJECT_TYPE_NAME;
	}

	public JiraAssetObject toAssetObject(PostalAddress postalAddress) {
		JiraAssetObject jiraAssetObject = createJiraAssetObject();

		Long id = postalAddress.getId();

		if (id != null) {
			jiraAssetObject.setAttributeValue(
				PostalAddressConstants.ATTRIBUTE_NAME_ID, String.valueOf(id));
		}

		jiraAssetObject.setAttributeValue(
			PostalAddressConstants.ATTRIBUTE_NAME_NAME,
			_getName(postalAddress));
		jiraAssetObject.setAttributeValue(
			PostalAddressConstants.ATTRIBUTE_NAME_ADDRESS_COUNTRY,
			postalAddress.getAddressCountry());
		jiraAssetObject.setAttributeValue(
			PostalAddressConstants.ATTRIBUTE_NAME_ADDRESS_LOCALITY,
			postalAddress.getAddressLocality());
		jiraAssetObject.setAttributeValue(
			PostalAddressConstants.ATTRIBUTE_NAME_ADDRESS_REGION,
			postalAddress.getAddressRegion());
		jiraAssetObject.setAttributeValue(
			PostalAddressConstants.ATTRIBUTE_NAME_ADDRESS_TYPE,
			postalAddress.getAddressType());
		jiraAssetObject.setAttributeValue(
			PostalAddressConstants.ATTRIBUTE_NAME_POSTAL_CODE,
			postalAddress.getPostalCode());
		jiraAssetObject.setAttributeValue(
			PostalAddressConstants.ATTRIBUTE_NAME_PRIMARY,
			postalAddress.getPrimary());
		jiraAssetObject.setAttributeValue(
			PostalAddressConstants.ATTRIBUTE_NAME_STREET_ADDRESS_LINE_1,
			postalAddress.getStreetAddressLine1());
		jiraAssetObject.setAttributeValue(
			PostalAddressConstants.ATTRIBUTE_NAME_STREET_ADDRESS_LINE_2,
			postalAddress.getStreetAddressLine2());
		jiraAssetObject.setAttributeValue(
			PostalAddressConstants.ATTRIBUTE_NAME_STREET_ADDRESS_LINE_3,
			postalAddress.getStreetAddressLine3());

		return jiraAssetObject;
	}

	@Override
	protected String getObjectSchemaName() {
		return _schemaName;
	}

	private String _getName(PostalAddress postalAddress) {
		List<String> parts = new ArrayList<>();

		for (String part :
				new String[] {
					postalAddress.getStreetAddressLine1(),
					postalAddress.getStreetAddressLine2(),
					postalAddress.getStreetAddressLine3(),
					postalAddress.getAddressLocality(),
					postalAddress.getAddressRegion(),
					postalAddress.getAddressCountry(),
					postalAddress.getPostalCode()
				}) {

			if (Validator.isNotNull(part)) {
				parts.add(part);
			}
		}

		return StringUtil.merge(parts, ", ");
	}

	@Value("${liferay.one.jira.asset.schema.name}")
	private String _schemaName;

}