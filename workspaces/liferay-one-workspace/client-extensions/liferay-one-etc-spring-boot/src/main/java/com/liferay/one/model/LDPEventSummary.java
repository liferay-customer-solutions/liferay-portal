/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.model;

import com.liferay.portal.kernel.util.Validator;

import java.time.LocalDate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * The response of the Liferay Data Platform Metrics API
 * <code>GET /projects/{salesforceProjectId}/ldp/usage/event-summary</code>
 * endpoint: the events counted per data source for a project within a date
 * range.
 *
 * @author Drew Brokke
 */
public class LDPEventSummary {

	public LDPEventSummary(JSONObject jsonObject) {
		_dataSourceEventCounts = _parseDataSourceEventCounts(
			jsonObject.optJSONArray("eventSummary"));
		_endLocalDate = _parseLocalDate(jsonObject.optString("endDate"));
		_projectName = jsonObject.optString("projectName");
		_salesforceProjectId = jsonObject.optString("salesforceProjectId");
		_startLocalDate = _parseLocalDate(jsonObject.optString("startDate"));
		_weDeployKey = jsonObject.optString("weDeployKey");
	}

	public List<LDPDataSourceEventCount> getDataSourceEventCounts() {
		return _dataSourceEventCounts;
	}

	public LocalDate getEndLocalDate() {
		return _endLocalDate;
	}

	public String getProjectName() {
		return _projectName;
	}

	public String getSalesforceProjectId() {
		return _salesforceProjectId;
	}

	public LocalDate getStartLocalDate() {
		return _startLocalDate;
	}

	public long getTotalEventsCount() {
		long totalEventsCount = 0;

		for (LDPDataSourceEventCount dataSourceEventCount :
				_dataSourceEventCounts) {

			totalEventsCount += dataSourceEventCount.getEventsCount();
		}

		return totalEventsCount;
	}

	public String getWeDeployKey() {
		return _weDeployKey;
	}

	private List<LDPDataSourceEventCount> _parseDataSourceEventCounts(
		JSONArray jsonArray) {

		if (jsonArray == null) {
			return Collections.emptyList();
		}

		List<LDPDataSourceEventCount> dataSourceEventCounts = new ArrayList<>(
			jsonArray.length());

		for (int i = 0; i < jsonArray.length(); i++) {
			dataSourceEventCounts.add(
				new LDPDataSourceEventCount(jsonArray.getJSONObject(i)));
		}

		return Collections.unmodifiableList(dataSourceEventCounts);
	}

	private LocalDate _parseLocalDate(String value) {
		if (Validator.isNull(value)) {
			return null;
		}

		return LocalDate.parse(value);
	}

	private final List<LDPDataSourceEventCount> _dataSourceEventCounts;
	private final LocalDate _endLocalDate;
	private final String _projectName;
	private final String _salesforceProjectId;
	private final LocalDate _startLocalDate;
	private final String _weDeployKey;

}