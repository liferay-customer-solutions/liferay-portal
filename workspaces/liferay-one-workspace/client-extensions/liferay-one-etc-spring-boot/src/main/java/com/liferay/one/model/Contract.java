/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.model;

import com.liferay.portal.kernel.util.Validator;

import java.time.Instant;

import org.json.JSONObject;

/**
 * @author Kyle Bischof
 */
public class Contract {

	public Contract(JSONObject jsonObject) {
		_externalReferenceCode = jsonObject.optString("externalReferenceCode");
		_id = jsonObject.optLong("id");
		_opportunityId = jsonObject.optString("opportunityId");
		_originalContractExternalReferenceCode = jsonObject.optString(
			"r_originalContractToContract_c_contractERC");
		_projectExternalReferenceCode = jsonObject.optString(
			"r_projectToContract_c_projectERC");
		_renewalOpportunityId = jsonObject.optString("renewalOpportunityId");

		JSONObject contractTypeJSONObject = jsonObject.optJSONObject(
			"contractType");

		if (contractTypeJSONObject == null) {
			_contractType = null;
		}
		else {
			_contractType = contractTypeJSONObject.optString("key", null);
		}

		String endDate = jsonObject.optString("endDate");

		if (Validator.isNull(endDate)) {
			_endDateInstant = null;
		}
		else {
			_endDateInstant = Instant.parse(endDate);
		}
	}

	public String getContractType() {
		return _contractType;
	}

	public Instant getEndDateInstant() {
		return _endDateInstant;
	}

	public String getExternalReferenceCode() {
		return _externalReferenceCode;
	}

	public long getId() {
		return _id;
	}

	public String getOpportunityId() {
		return _opportunityId;
	}

	public String getOriginalContractExternalReferenceCode() {
		return _originalContractExternalReferenceCode;
	}

	public String getProjectExternalReferenceCode() {
		return _projectExternalReferenceCode;
	}

	public String getRenewalOpportunityId() {
		return _renewalOpportunityId;
	}

	private final String _contractType;
	private final Instant _endDateInstant;
	private final String _externalReferenceCode;
	private final long _id;
	private final String _opportunityId;
	private final String _originalContractExternalReferenceCode;
	private final String _projectExternalReferenceCode;
	private final String _renewalOpportunityId;

}