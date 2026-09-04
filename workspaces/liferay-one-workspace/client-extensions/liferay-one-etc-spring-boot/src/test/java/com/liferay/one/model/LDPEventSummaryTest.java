/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.model;

import java.time.LocalDate;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Drew Brokke
 */
public class LDPEventSummaryTest {

	@Test
	public void testParsesMetricsAPIContract() {
		LDPEventSummary ldpEventSummary = new LDPEventSummary(
			new JSONObject(
			).put(
				"endDate", "2026-08-31"
			).put(
				"eventSummary",
				new JSONArray(
				).put(
					_createDataSourceEventCountJSONObject(
						"ds-1", "Website", 750000)
				).put(
					_createDataSourceEventCountJSONObject(
						"ds-2", "Mobile App", 500000)
				)
			).put(
				"projectName", "Acme"
			).put(
				"salesforceProjectId", "a0B0g00000eABCD123"
			).put(
				"startDate", "2026-08-01"
			).put(
				"weDeployKey", "acme-prod"
			));

		Assertions.assertEquals(
			LocalDate.of(2026, 8, 31), ldpEventSummary.getEndLocalDate());
		Assertions.assertEquals("Acme", ldpEventSummary.getProjectName());
		Assertions.assertEquals(
			"a0B0g00000eABCD123", ldpEventSummary.getSalesforceProjectId());
		Assertions.assertEquals(
			LocalDate.of(2026, 8, 1), ldpEventSummary.getStartLocalDate());
		Assertions.assertEquals("acme-prod", ldpEventSummary.getWeDeployKey());

		List<LDPDataSourceEventCount> dataSourceEventCounts =
			ldpEventSummary.getDataSourceEventCounts();

		Assertions.assertEquals(2, dataSourceEventCounts.size());

		LDPDataSourceEventCount dataSourceEventCount =
			dataSourceEventCounts.get(0);

		Assertions.assertEquals("ds-1", dataSourceEventCount.getDataSourceId());
		Assertions.assertEquals(
			"Website", dataSourceEventCount.getDataSourceName());
		Assertions.assertEquals(750000, dataSourceEventCount.getEventsCount());

		Assertions.assertEquals(1250000, ldpEventSummary.getTotalEventsCount());
	}

	@Test
	public void testTotalIsZeroWithoutEventSummary() {
		LDPEventSummary ldpEventSummary = new LDPEventSummary(
			new JSONObject(
			).put(
				"salesforceProjectId", "a0B0g00000eABCD123"
			));

		Assertions.assertTrue(
			ldpEventSummary.getDataSourceEventCounts(
			).isEmpty());
		Assertions.assertNull(ldpEventSummary.getEndLocalDate());
		Assertions.assertNull(ldpEventSummary.getStartLocalDate());
		Assertions.assertEquals(0, ldpEventSummary.getTotalEventsCount());
	}

	private JSONObject _createDataSourceEventCountJSONObject(
		String dataSourceId, String dataSourceName, long eventsCount) {

		return new JSONObject(
		).put(
			"dataSourceId", dataSourceId
		).put(
			"dataSourceName", dataSourceName
		).put(
			"eventsCount", eventsCount
		);
	}

}