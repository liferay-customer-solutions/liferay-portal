/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.model;

import org.json.JSONObject;

/**
 * @author Kyle Bischof
 */
public class Contract {

	public Contract(JSONObject jsonObject) {
		_dateCreated = jsonObject.optString("dateCreated");
		_endDate = jsonObject.optString("endDate");
		_externalReferenceCode = jsonObject.optString("externalReferenceCode");
		_id = jsonObject.optLong("id");
		_projectExternalReferenceCode = jsonObject.optString(
			"r_projectToContract_c_projectERC");
	}

	public String getDateCreated() {
		return _dateCreated;
	}

	public String getEndDate() {
		return _endDate;
	}

	public String getExternalReferenceCode() {
		return _externalReferenceCode;
	}

	public long getId() {
		return _id;
	}

	public String getProjectExternalReferenceCode() {
		return _projectExternalReferenceCode;
	}

	private final String _dateCreated;
	private final String _endDate;
	private final String _externalReferenceCode;
	private final long _id;
	private final String _projectExternalReferenceCode;

}