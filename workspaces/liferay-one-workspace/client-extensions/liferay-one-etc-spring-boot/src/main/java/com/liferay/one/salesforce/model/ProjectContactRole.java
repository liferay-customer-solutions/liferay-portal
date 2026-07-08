/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.salesforce.model;

import org.json.JSONObject;

/**
 * @author Kyle Bischof
 */
public class ProjectContactRole {

	public ProjectContactRole(JSONObject jsonObject) {
		_contactRole = jsonObject.optString("Contact_Role__c");
		_emailAddress = jsonObject.optString("Contact__r.Email");
		_firstName = jsonObject.optString("Contact__r.FirstName");
		_lastName = jsonObject.optString("Contact__r.LastName");
		_projectId = jsonObject.optString("Project__c");
	}

	public String getContactRole() {
		return _contactRole;
	}

	public String getEmailAddress() {
		return _emailAddress;
	}

	public String getFirstName() {
		return _firstName;
	}

	public String getLastName() {
		return _lastName;
	}

	public String getProjectId() {
		return _projectId;
	}

	private final String _contactRole;
	private final String _emailAddress;
	private final String _firstName;
	private final String _lastName;
	private final String _projectId;

}