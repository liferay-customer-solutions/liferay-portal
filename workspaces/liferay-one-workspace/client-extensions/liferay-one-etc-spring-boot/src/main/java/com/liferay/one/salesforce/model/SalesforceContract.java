/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.salesforce.model;

import org.json.JSONObject;

/**
 * @author Kyle Bischof
 */
public class SalesforceContract {

	public SalesforceContract(JSONObject jsonObject) {
		_accountId = jsonObject.optString("AccountId");
		_endDate = jsonObject.optString("EndDate");
		_id = jsonObject.optString("Id");
		_opportunityId = jsonObject.optString("SBQQ__Opportunity__c");
		_renewalOpportunityId = jsonObject.optString(
			"SBQQ__RenewalOpportunity__c");
		_startDate = jsonObject.optString("StartDate");

		if (jsonObject.isNull("ContractTerm")) {
			_contractTerm = null;
		}
		else {
			_contractTerm = jsonObject.optInt("ContractTerm");
		}
	}

	public String getAccountId() {
		return _accountId;
	}

	public Integer getContractTerm() {
		return _contractTerm;
	}

	public String getEndDate() {
		return _endDate;
	}

	public String getId() {
		return _id;
	}

	public String getOpportunityId() {
		return _opportunityId;
	}

	public String getRenewalOpportunityId() {
		return _renewalOpportunityId;
	}

	public String getStartDate() {
		return _startDate;
	}

	private final String _accountId;
	private final Integer _contractTerm;
	private final String _endDate;
	private final String _id;
	private final String _opportunityId;
	private final String _renewalOpportunityId;
	private final String _startDate;

}