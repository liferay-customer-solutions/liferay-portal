/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.one.model.Property;
import com.liferay.petra.string.StringBundler;

import java.util.List;

import org.json.JSONObject;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Felipe Veloso
 */
@Component
public class PropertyService extends OneBaseService {

	public Property addProperty(long accountId, String name, String value)
		throws Exception {

		JSONObject propertyJSONObject = new JSONObject(
		).put(
			"name", name
		).put(
			"r_accountEntryToProperty_accountEntryId", accountId
		).put(
			"value", value
		);

		String response = post(
			getAuthorization(), propertyJSONObject.toString(),
			UriComponentsBuilder.fromPath(
				"/o/c/properties"
			).build(
			).toUri());

		return new Property(new JSONObject(response));
	}

	public List<Property> getAccountProperties(long accountId)
		throws Exception {

		return getProperties(
			StringBundler.concat(
				"(r_accountEntryToProperty_accountEntryId eq '", accountId,
				"')"));
	}

	public List<Property> getAccountPropertiesByName(
			long accountId, String name)
		throws Exception {

		return getProperties(
			StringBundler.concat(
				"(r_accountEntryToProperty_accountEntryId eq '", accountId,
				"') and (name eq '", name, "')"));
	}

	public List<Property> getProperties(String filterString) throws Exception {
		return getAllItems("/o/c/properties", filterString, Property::new);
	}

	public String getPropertyValue(long accountId, String name)
		throws Exception {

		List<Property> properties = getAccountPropertiesByName(accountId, name);

		if (properties.isEmpty()) {
			return null;
		}

		Property property = properties.get(0);

		return property.getValue();
	}

}