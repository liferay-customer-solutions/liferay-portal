/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.model;

import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Pedro Oliveira
 */
public class AccountInvitation {

	public AccountInvitation(JSONObject jsonObject) {
		_accepted = jsonObject.optBoolean("accepted");
		_accountExternalReferenceCode = jsonObject.optString(
			"accountExternalReferenceCode");
		_accountInvitationId = jsonObject.getLong("id");
		_emailAddress = jsonObject.optString("emailAddress");
		_externalReferenceCode = jsonObject.optString("externalReferenceCode");
		_familyName = jsonObject.optString("familyName");
		_givenName = jsonObject.optString("givenName");
		_roleNames = _toRoleNames(jsonObject.optString("roleNames"));
		_token = jsonObject.optString("token");
	}

	public String getAccountExternalReferenceCode() {
		return _accountExternalReferenceCode;
	}

	public long getAccountInvitationId() {
		return _accountInvitationId;
	}

	public String getEmailAddress() {
		return _emailAddress;
	}

	public String getExternalReferenceCode() {
		return _externalReferenceCode;
	}

	public String getFamilyName() {
		return _familyName;
	}

	public String getGivenName() {
		return _givenName;
	}

	public List<String> getRoleNames() {
		return _roleNames;
	}

	public String getToken() {
		return _token;
	}

	public boolean isAccepted() {
		return _accepted;
	}

	private List<String> _toRoleNames(String value) {
		List<String> roleNames = new ArrayList<>();

		if (Validator.isNull(value)) {
			return roleNames;
		}

		try {
			JSONArray roleNamesJSONArray = new JSONArray(value);

			for (int i = 0; i < roleNamesJSONArray.length(); i++) {
				String roleName = roleNamesJSONArray.getString(i);

				if (Validator.isNotNull(roleName)) {
					roleNames.add(roleName);
				}
			}

			return roleNames;
		}
		catch (JSONException jsonException) {
			if (_log.isDebugEnabled()) {
				_log.debug(
					"Unable to read the role names as a JSON array",
					jsonException);
			}
		}

		for (String roleName : value.split(",")) {
			String trimmedRoleName = roleName.trim();

			if (Validator.isNotNull(trimmedRoleName)) {
				roleNames.add(trimmedRoleName);
			}
		}

		return roleNames;
	}

	private static final Log _log = LogFactory.getLog(AccountInvitation.class);

	private final boolean _accepted;
	private final String _accountExternalReferenceCode;
	private final long _accountInvitationId;
	private final String _emailAddress;
	private final String _externalReferenceCode;
	private final String _familyName;
	private final String _givenName;
	private final List<String> _roleNames;
	private final String _token;

}