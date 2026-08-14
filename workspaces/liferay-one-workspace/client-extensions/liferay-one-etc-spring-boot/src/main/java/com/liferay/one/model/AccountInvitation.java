/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.model;

import com.liferay.portal.kernel.util.Validator;

import java.time.Instant;
import java.time.format.DateTimeParseException;

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
		_customExpirationDateInstant = _toInstant(
			jsonObject.optString("customExpirationDate"));
		_externalReferenceCode = jsonObject.optString("externalReferenceCode");
		_familyName = jsonObject.optString("familyName");
		_givenName = jsonObject.optString("givenName");
		_projectExternalReferenceCode = jsonObject.optString(
			"projectExternalReferenceCode");
		_projectRoleExternalReferenceCode = jsonObject.optString(
			"projectRoleExternalReferenceCode");
		_roleExternalReferenceCodes = _toRoleExternalReferenceCodes(
			jsonObject.optString("roleExternalReferenceCodes"));
		_token = jsonObject.optString("token");
	}

	public String getAccountExternalReferenceCode() {
		return _accountExternalReferenceCode;
	}

	public long getAccountInvitationId() {
		return _accountInvitationId;
	}

	public Instant getCustomExpirationDateInstant() {
		return _customExpirationDateInstant;
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

	public String getProjectExternalReferenceCode() {
		return _projectExternalReferenceCode;
	}

	public String getProjectRoleExternalReferenceCode() {
		return _projectRoleExternalReferenceCode;
	}

	public List<String> getRoleExternalReferenceCodes() {
		return _roleExternalReferenceCodes;
	}

	public String getToken() {
		return _token;
	}

	public boolean isAccepted() {
		return _accepted;
	}

	public boolean isExpired() {
		if (_customExpirationDateInstant == null) {
			return true;
		}

		return _customExpirationDateInstant.isBefore(Instant.now());
	}

	private Instant _toInstant(String value) {
		if (Validator.isNull(value)) {
			return null;
		}

		try {
			return Instant.parse(value);
		}
		catch (DateTimeParseException dateTimeParseException) {
			_log.error(
				"Unable to read the expiration date " + value,
				dateTimeParseException);

			return null;
		}
	}

	private List<String> _toRoleExternalReferenceCodes(String value) {
		List<String> roleExternalReferenceCodes = new ArrayList<>();

		if (Validator.isNull(value)) {
			return roleExternalReferenceCodes;
		}

		try {
			JSONArray jsonArray = new JSONArray(value);

			for (int i = 0; i < jsonArray.length(); i++) {
				String roleExternalReferenceCode = jsonArray.getString(i);

				if (Validator.isNotNull(roleExternalReferenceCode)) {
					roleExternalReferenceCodes.add(roleExternalReferenceCode);
				}
			}

			return roleExternalReferenceCodes;
		}
		catch (JSONException jsonException) {
			if (_log.isDebugEnabled()) {
				_log.debug(
					"Unable to read the role ERCs as a JSON array",
					jsonException);
			}
		}

		for (String roleExternalReferenceCode : value.split(",")) {
			String trimmedRoleExternalReferenceCode =
				roleExternalReferenceCode.trim();

			if (Validator.isNotNull(trimmedRoleExternalReferenceCode)) {
				roleExternalReferenceCodes.add(
					trimmedRoleExternalReferenceCode);
			}
		}

		return roleExternalReferenceCodes;
	}

	private static final Log _log = LogFactory.getLog(AccountInvitation.class);

	private final boolean _accepted;
	private final String _accountExternalReferenceCode;
	private final long _accountInvitationId;
	private final Instant _customExpirationDateInstant;
	private final String _emailAddress;
	private final String _externalReferenceCode;
	private final String _familyName;
	private final String _givenName;
	private final String _projectExternalReferenceCode;
	private final String _projectRoleExternalReferenceCode;
	private final List<String> _roleExternalReferenceCodes;
	private final String _token;

}