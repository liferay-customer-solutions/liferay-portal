/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.model;

import com.liferay.one.constants.JiraIssueConstants;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.util.ArrayUtil;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author Jenny Chen
 */
public class JiraSupportIssue {

	public JiraSupportIssue(
		JiraOrganization jiraOrganization, JSONObject jsonObject) {

		this(jsonObject);

		_jiraOrganization = jiraOrganization;
	}

	public JiraSupportIssue(JSONObject jsonObject) {
		_key = jsonObject.getString("key");

		JSONObject fieldsJSONObject = jsonObject.getJSONObject("fields");

		JSONArray labelsJSONArray = fieldsJSONObject.optJSONArray(
			"labels", new JSONArray());

		List<String> labels = new ArrayList<>();

		for (int i = 0; i < labelsJSONArray.length(); i++) {
			labels.add(labelsJSONArray.getString(i));
		}

		_labels = labels.toArray(new String[0]);

		JSONObject statusJSONObject = fieldsJSONObject.optJSONObject(
			"status", new JSONObject());

		_status = statusJSONObject.optString("name");

		_summary = fieldsJSONObject.optString("summary");
	}

	public JiraSupportIssue(JSONObject jsonObject, String ticketURL) {
		this(jsonObject);

		_ticketURL = ticketURL;
	}

	public JiraOrganization getJiraOrganization() {
		return _jiraOrganization;
	}

	public String getKey() {
		return _key;
	}

	public String[] getLabels() {
		return _labels;
	}

	public String getStatus() {
		return _status;
	}

	public String getSummary() {
		return _summary;
	}

	public String getTicketURL() {
		return _ticketURL;
	}

	public boolean isClosed() {
		return ArrayUtil.contains(JiraIssueConstants.STATUSES_CLOSED, _status);
	}

	public JSONObject toJSONObject() {
		return new JSONObject(
		).put(
			"link", getTicketURL()
		).put(
			"status", getStatus()
		).put(
			"subject", getSummary()
		).put(
			"ticketId", getKey()
		);
	}

	private JiraOrganization _jiraOrganization;
	private final String _key;
	private final String[] _labels;
	private final String _status;
	private final String _summary;
	private String _ticketURL = StringPool.BLANK;

}