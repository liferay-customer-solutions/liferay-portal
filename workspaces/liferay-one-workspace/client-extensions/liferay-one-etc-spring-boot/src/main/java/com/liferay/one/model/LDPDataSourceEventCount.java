/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.model;

import org.json.JSONObject;

/**
 * One element of the Liferay Data Platform Metrics API "eventSummary" array:
 * the events counted for a single data source.
 *
 * @author Drew Brokke
 */
public class LDPDataSourceEventCount {

	public LDPDataSourceEventCount(JSONObject jsonObject) {
		_dataSourceId = jsonObject.optString("dataSourceId");
		_dataSourceName = jsonObject.optString("dataSourceName");
		_eventsCount = jsonObject.optLong("eventsCount");
	}

	public String getDataSourceId() {
		return _dataSourceId;
	}

	public String getDataSourceName() {
		return _dataSourceName;
	}

	public long getEventsCount() {
		return _eventsCount;
	}

	private final String _dataSourceId;
	private final String _dataSourceName;
	private final long _eventsCount;

}