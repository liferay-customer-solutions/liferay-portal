/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.salesforce.model;

import org.json.JSONObject;

/**
 * @author Kyle Bischof
 */
public class Project {

	public Project(JSONObject jsonObject) {
		_accountId = jsonObject.optString("Account__c");
		_aiHubAccountName = jsonObject.optString("AI_Hub_Account_Name__c");
		_allowedEmailDomains = jsonObject.optString("Allowed_Email_Domains__c");
		_dataCenterLocation = jsonObject.optString("Data_Center_Location__c");
		_friendlyWorkspaceURL = jsonObject.optString(
			"Friendly_Workspace_URL__c");
		_id = jsonObject.optString("Id");
		_ldpWorkspaceName = jsonObject.optString("LDP_Workspace_Name__c");
		_liferayVersion = jsonObject.optString("Liferay_Version__c");
		_name = jsonObject.optString("Name");
		_securityContactEmailAddress = jsonObject.optString(
			"Security_Contact_Email_Address__c");
	}

	public String getAccountId() {
		return _accountId;
	}

	public String getAIHubAccountName() {
		return _aiHubAccountName;
	}

	public String getAllowedEmailDomains() {
		return _allowedEmailDomains;
	}

	public String getDataCenterLocation() {
		return _dataCenterLocation;
	}

	public String getFriendlyWorkspaceURL() {
		return _friendlyWorkspaceURL;
	}

	public String getId() {
		return _id;
	}

	public String getLDPWorkspaceName() {
		return _ldpWorkspaceName;
	}

	public String getLiferayVersion() {
		return _liferayVersion;
	}

	public String getName() {
		return _name;
	}

	public String getSecurityContactEmailAddress() {
		return _securityContactEmailAddress;
	}

	private final String _accountId;
	private final String _aiHubAccountName;
	private final String _allowedEmailDomains;
	private final String _dataCenterLocation;
	private final String _friendlyWorkspaceURL;
	private final String _id;
	private final String _ldpWorkspaceName;
	private final String _liferayVersion;
	private final String _name;
	private final String _securityContactEmailAddress;

}