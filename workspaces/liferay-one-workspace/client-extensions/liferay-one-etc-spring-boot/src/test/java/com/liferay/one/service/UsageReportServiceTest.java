/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.one.model.Project;
import com.liferay.one.model.UsageDefinition;

import java.time.Instant;

import org.json.JSONObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;

/**
 * @author Drew Brokke
 */
public class UsageReportServiceTest {

	@BeforeEach
	public void setUp() throws Exception {
		_usageReportService = Mockito.spy(new UsageReportService());

		Mockito.doReturn(
			null
		).when(
			_usageReportService
		).addUsageReport(
			Mockito.anyString(), Mockito.anyDouble(), Mockito.any(),
			Mockito.any(), Mockito.any(), Mockito.anyDouble(),
			Mockito.anyString(), Mockito.anyDouble(), Mockito.anyDouble(),
			Mockito.any(), Mockito.anyString(), Mockito.anyDouble(),
			Mockito.anyLong(), Mockito.anyString(), Mockito.anyString(),
			Mockito.anyLong()
		);
	}

	@Test
	public void testDerivesCompletedReportWithoutOverage() throws Exception {
		_usageReportService.addUsageReport(
			999999, _CONTRACT_EXTERNAL_REFERENCE_CODE, _DATE_FROM_INSTANT,
			_DATE_TO_INSTANT, 1000000, _EXTERNAL_REFERENCE_CODE, _project,
			_SKU_EXTERNAL_REFERENCE_CODE, _usageDefinition);

		Mockito.verify(
			_usageReportService
		).addUsageReport(
			_ACCOUNT_EXTERNAL_REFERENCE_CODE, 999999,
			_CONTRACT_EXTERNAL_REFERENCE_CODE, _DATE_FROM_INSTANT,
			_DATE_TO_INSTANT, 1000000, _EXTERNAL_REFERENCE_CODE, 0, 0,
			_OVERAGE_BUCKET_SIZE, "USD", 0, _PROJECT_ID,
			UsageReportService.REVIEW_STATUS_COMPLETED,
			_SKU_EXTERNAL_REFERENCE_CODE, _USAGE_DEFINITION_ID
		);
	}

	@Test
	public void testPricesWholeOverageBuckets() throws Exception {
		_usageReportService.addUsageReport(
			1400000, _CONTRACT_EXTERNAL_REFERENCE_CODE, _DATE_FROM_INSTANT,
			_DATE_TO_INSTANT, 1000000, _EXTERNAL_REFERENCE_CODE, _project,
			_SKU_EXTERNAL_REFERENCE_CODE, _usageDefinition);

		Mockito.verify(
			_usageReportService
		).addUsageReport(
			_ACCOUNT_EXTERNAL_REFERENCE_CODE, 1400000,
			_CONTRACT_EXTERNAL_REFERENCE_CODE, _DATE_FROM_INSTANT,
			_DATE_TO_INSTANT, 1000000, _EXTERNAL_REFERENCE_CODE,
			2 * _OVERAGE_RATE, 2, _OVERAGE_BUCKET_SIZE, "USD", 400000,
			_PROJECT_ID, UsageReportService.REVIEW_STATUS_READY_FOR_REVIEW,
			_SKU_EXTERNAL_REFERENCE_CODE, _USAGE_DEFINITION_ID
		);
	}

	/**
	 * A bucket is the smallest quantity that can go on an order, so an overage
	 * of one and a half buckets bills as two.
	 */
	@Test
	public void testRoundsPartialOverageBucketUp() throws Exception {
		_usageReportService.addUsageReport(
			1300000, _CONTRACT_EXTERNAL_REFERENCE_CODE, _DATE_FROM_INSTANT,
			_DATE_TO_INSTANT, 1000000, _EXTERNAL_REFERENCE_CODE, _project,
			_SKU_EXTERNAL_REFERENCE_CODE, _usageDefinition);

		Mockito.verify(
			_usageReportService
		).addUsageReport(
			_ACCOUNT_EXTERNAL_REFERENCE_CODE, 1300000,
			_CONTRACT_EXTERNAL_REFERENCE_CODE, _DATE_FROM_INSTANT,
			_DATE_TO_INSTANT, 1000000, _EXTERNAL_REFERENCE_CODE,
			2 * _OVERAGE_RATE, 2, _OVERAGE_BUCKET_SIZE, "USD", 300000,
			_PROJECT_ID, UsageReportService.REVIEW_STATUS_READY_FOR_REVIEW,
			_SKU_EXTERNAL_REFERENCE_CODE, _USAGE_DEFINITION_ID
		);
	}

	/**
	 * A single event over the allotment still buys a whole bucket.
	 */
	@Test
	public void testRoundsSingleUnitOverageUpToOneBucket() throws Exception {
		_usageReportService.addUsageReport(
			1000001, _CONTRACT_EXTERNAL_REFERENCE_CODE, _DATE_FROM_INSTANT,
			_DATE_TO_INSTANT, 1000000, _EXTERNAL_REFERENCE_CODE, _project,
			_SKU_EXTERNAL_REFERENCE_CODE, _usageDefinition);

		Mockito.verify(
			_usageReportService
		).addUsageReport(
			_ACCOUNT_EXTERNAL_REFERENCE_CODE, 1000001,
			_CONTRACT_EXTERNAL_REFERENCE_CODE, _DATE_FROM_INSTANT,
			_DATE_TO_INSTANT, 1000000, _EXTERNAL_REFERENCE_CODE, _OVERAGE_RATE,
			1, _OVERAGE_BUCKET_SIZE, "USD", 1, _PROJECT_ID,
			UsageReportService.REVIEW_STATUS_READY_FOR_REVIEW,
			_SKU_EXTERNAL_REFERENCE_CODE, _USAGE_DEFINITION_ID
		);
	}

	private static final String _ACCOUNT_EXTERNAL_REFERENCE_CODE = "ACCNT-001";

	private static final String _CONTRACT_EXTERNAL_REFERENCE_CODE =
		"C_CONTRACT_001";

	private static final Instant _DATE_FROM_INSTANT = Instant.parse(
		"2026-08-01T00:00:00Z");

	private static final Instant _DATE_TO_INSTANT = Instant.parse(
		"2026-08-31T23:59:59.999Z");

	private static final String _EXTERNAL_REFERENCE_CODE =
		"C_USAGE_REPORT_PRJCT_001_2026_08";

	private static final double _OVERAGE_BUCKET_SIZE = 200000;

	private static final double _OVERAGE_RATE = 20;

	private static final long _PROJECT_ID = 22;

	private static final String _SKU_EXTERNAL_REFERENCE_CODE =
		"PRDCT-DATA-PLATFORM";

	private static final long _USAGE_DEFINITION_ID = 33;

	private final Project _project = new Project(
		new JSONObject(
		).put(
			"externalReferenceCode", "PRJCT-001"
		).put(
			"id", _PROJECT_ID
		).put(
			"r_accountEntryToProject_accountEntryERC",
			_ACCOUNT_EXTERNAL_REFERENCE_CODE
		));
	private final UsageDefinition _usageDefinition = new UsageDefinition(
		new JSONObject(
		).put(
			"id", _USAGE_DEFINITION_ID
		).put(
			"overageBucketSize", _OVERAGE_BUCKET_SIZE
		).put(
			"overageCurrency", "USD"
		).put(
			"overageRate", _OVERAGE_RATE
		));
	private UsageReportService _usageReportService;

}