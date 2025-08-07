/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.customer.model;

import com.liferay.customer.constants.JiraIssueConstants;

import org.json.JSONObject;

/**
 * @author Jenny Chen
 */
public class JiraIssue {

	public JiraIssue(JSONObject jsonObject, String jiraIssueURL) {
		_key = jsonObject.getString("key");
		_ticketURL = jiraIssueURL;

		JSONObject fieldsJSONObject = jsonObject.getJSONObject("fields");

		JSONObject statusJSONObject = fieldsJSONObject.optJSONObject("status");

		_status = statusJSONObject.getString("name");

		_summary = fieldsJSONObject.getString("summary");
	}

	public String getKey() {
		return _key;
	}

	public String getStatus() {
		return _status;
	}

	public String getSummary() {
		return _summary;
	}

	public boolean isClosed() {
		if (_status.equals(JiraIssueConstants.STATUS_CLOSED) ||
			_status.equals(JiraIssueConstants.STATUS_SOLUTION_ACCEPTED) ||
			_status.equals(JiraIssueConstants.STATUS_SOLUTION_PROPOSED)) {

			return true;
		}

		return false;
	}

	public JSONObject toJSONObject() {
		return new JSONObject(
		).put(
			"link", _ticketURL + "/" + _key
		).put(
			"status", _status
		).put(
			"subject", _summary
		).put(
			"ticketId", _key
		);
	}

	private final String _key;
	private final String _status;
	private final String _summary;
	private final String _ticketURL;

}