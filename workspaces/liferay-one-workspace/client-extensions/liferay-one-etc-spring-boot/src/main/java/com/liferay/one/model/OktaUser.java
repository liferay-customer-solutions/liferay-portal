/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONObject;

/**
 * @author Karoline Silva
 */
public class OktaUser {

	public OktaUser(JSONObject jsonObject) {
		JSONObject profileJSONObject = jsonObject.optJSONObject("profile");

		if (profileJSONObject == null) {
			profileJSONObject = new JSONObject();
		}

		_email = profileJSONObject.optString("email");
		_emailAddressVerified = _statusesVerified.contains(
			jsonObject.optString("status"));
		_firstName = profileJSONObject.optString("firstName");
		_lastName = profileJSONObject.optString("lastName");
		_middleName = profileJSONObject.optString("middleName");
		_uuid = profileJSONObject.optString("uuid");
	}

	public String getEmail() {
		return _email;
	}

	public String getFirstName() {
		return _firstName;
	}

	public String getLastName() {
		return _lastName;
	}

	public String getMiddleName() {
		return _middleName;
	}

	public String getUuid() {
		return _uuid;
	}

	public boolean isEmailAddressVerified() {
		return _emailAddressVerified;
	}

	private static final Set<String> _statusesVerified = new HashSet<>(
		Arrays.asList(
			"ACTIVE", "LOCKED_OUT", "PASSWORD_EXPIRED", "RECOVERY",
			"SUSPENDED"));

	private final String _email;
	private final boolean _emailAddressVerified;
	private final String _firstName;
	private final String _lastName;
	private final String _middleName;
	private final String _uuid;

}