/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.salesforce.model;

import org.json.JSONObject;

/**
 * @author Kyle Bischof
 */
public class Opportunity {

	public Opportunity(JSONObject jsonObject) {
		_accountId = jsonObject.optString("AccountId");
		_amendedContractOpportunityId = jsonObject.optString(
			"SBQQ__AmendedContract__r.SBQQ__Opportunity__c");
		_firstLineSupport = jsonObject.optBoolean("First_Line_Support__c");
		_id = jsonObject.optString("Id");
		_name = jsonObject.optString("Name");
		_ownerEmailAddress = jsonObject.optString("Owner.Email");
		_ownerFirstName = jsonObject.optString("Owner.FirstName");
		_ownerLastName = jsonObject.optString("Owner.LastName");
		_productFamily = jsonObject.optString("Product_Family__c");
		_projectId = jsonObject.optString("Project__c");
		_renewal = jsonObject.optDouble("Has_Renewal__c", 0) > 0;
		_resellerName = jsonObject.optString("Reseller__r.Name");
		_soldBy = jsonObject.optString("Sold_By__c");
		_stageName = jsonObject.optString("StageName");
		_type = jsonObject.optString("Type");
	}

	public String getAccountId() {
		return _accountId;
	}

	public String getAmendedContractOpportunityId() {
		return _amendedContractOpportunityId;
	}

	public String getId() {
		return _id;
	}

	public String getName() {
		return _name;
	}

	public String getOwnerEmailAddress() {
		return _ownerEmailAddress;
	}

	public String getOwnerFirstName() {
		return _ownerFirstName;
	}

	public String getOwnerLastName() {
		return _ownerLastName;
	}

	public String getProductFamily() {
		return _productFamily;
	}

	public String getProjectId() {
		return _projectId;
	}

	public String getResellerName() {
		return _resellerName;
	}

	public String getSoldBy() {
		return _soldBy;
	}

	public String getStageName() {
		return _stageName;
	}

	public String getType() {
		return _type;
	}

	public boolean isFirstLineSupport() {
		return _firstLineSupport;
	}

	public boolean isRenewal() {
		return _renewal;
	}

	private final String _accountId;
	private final String _amendedContractOpportunityId;
	private final boolean _firstLineSupport;
	private final String _id;
	private final String _name;
	private final String _ownerEmailAddress;
	private final String _ownerFirstName;
	private final String _ownerLastName;
	private final String _productFamily;
	private final String _projectId;
	private final boolean _renewal;
	private final String _resellerName;
	private final String _soldBy;
	private final String _stageName;
	private final String _type;

}