/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.model;

import org.json.JSONObject;

/**
 * @author Felipe Veloso
 */
public class ProjectMembership {

	public ProjectMembership(JSONObject jsonObject) {
		_accountId = jsonObject.optLong(
			"r_accountEntryToProjectMembership_accountEntryId");
		_externalReferenceCode = jsonObject.optString("externalReferenceCode");
		_projectExternalReferenceCode = jsonObject.optString(
			"r_projectToProjectMembership_c_projectERC");
		_userId = jsonObject.optLong("r_userToProjectMembership_userId");
	}

	public long getAccountId() {
		return _accountId;
	}

	public String getExternalReferenceCode() {
		return _externalReferenceCode;
	}

	public String getProjectExternalReferenceCode() {
		return _projectExternalReferenceCode;
	}

	public long getUserId() {
		return _userId;
	}

	private final long _accountId;
	private final String _externalReferenceCode;
	private final String _projectExternalReferenceCode;
	private final long _userId;

}