/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.model;

import org.json.JSONObject;

/**
 * @author Felipe Veloso
 */
public class Property {

	public Property(JSONObject jsonObject) {
		_accountEntryId = jsonObject.optLong(
			"r_accountEntryToProperty_accountEntryId");
		_name = jsonObject.optString("name");
		_propertyId = jsonObject.getLong("id");
		_value = jsonObject.optString("value");
	}

	public long getAccountEntryId() {
		return _accountEntryId;
	}

	public String getName() {
		return _name;
	}

	public long getPropertyId() {
		return _propertyId;
	}

	public String getValue() {
		return _value;
	}

	private final long _accountEntryId;
	private final String _name;
	private final long _propertyId;
	private final String _value;

}