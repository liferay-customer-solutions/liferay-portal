/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.one.model.Property;
import com.liferay.petra.string.StringBundler;

import java.util.List;

import org.springframework.stereotype.Component;

/**
 * @author Felipe Veloso
 */
@Component
public class PropertyService extends OneBaseService {

	public String getPropertyValue(long accountId, String name)
		throws Exception {

		List<Property> properties = getAllItems(
			"/o/c/properties",
			StringBundler.concat(
				"(r_accountEntryToProperty_accountEntryId eq '", accountId,
				"') and (name eq '", name, "')"),
			Property::new);

		if (properties.isEmpty()) {
			return null;
		}

		Property property = properties.get(0);

		return property.getValue();
	}

}