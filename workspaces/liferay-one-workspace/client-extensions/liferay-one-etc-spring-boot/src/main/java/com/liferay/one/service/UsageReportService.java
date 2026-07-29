/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Generates monthly consumption based usage reports. On the first of every month
 * the datawarehouse is queried for the prior month's metered consumption, each
 * metered entitlement's usage is compared against its allotment, and every
 * overage is recorded as a UsageReport in the "Ready for Review" state. A
 * reviewer then approves the report (which triggers the overage order through
 * {@code ObjectActionUsageReportApprovedRestController}) or completes it when no
 * invoice is needed.
 *
 * @author Ryan Schuhler
 */
@Component
public class UsageReportService extends OneBaseService {

	public JSONObject fetchUsageReport(long usageReportId) throws Exception {
		String response = get(
			getAuthorization(),
			UriComponentsBuilder.fromPath(
				"/o/c/usagereports/{usageReportId}"
			).buildAndExpand(
				usageReportId
			).toUri());

		if (Validator.isNull(response)) {
			return null;
		}

		return new JSONObject(response);
	}

	public void generateUsageReports() throws Exception {
		YearMonth yearMonth = YearMonth.now(
			ZoneOffset.UTC
		).minusMonths(
			1
		);

		if (_log.isInfoEnabled()) {
			_log.info("Generating usage reports for " + yearMonth);
		}

		JSONArray usageRecordsJSONArray =
			_getDataWarehouseUsageRecordsJSONArray();

		int overageCount = 0;

		for (int i = 0; i < usageRecordsJSONArray.length(); i++) {
			JSONObject usageRecordJSONObject =
				usageRecordsJSONArray.getJSONObject(i);

			try {
				if (_generateUsageReport(usageRecordJSONObject, yearMonth)) {
					overageCount++;
				}
			}
			catch (Exception exception) {
				_log.error(
					"Unable to generate usage report for " +
						usageRecordJSONObject,
					exception);
			}
		}

		if (_log.isInfoEnabled()) {
			_log.info(
				StringBundler.concat(
					"Generated ", overageCount, " usage report(s) for ",
					yearMonth));
		}
	}

	@Scheduled(cron = "${liferay.one.usage.report.cron:0 0 2 1 * *}")
	public void scheduledGenerateUsageReports() {
		try {
			generateUsageReports();
		}
		catch (Exception exception) {
			_log.error("Unable to generate usage reports", exception);
		}
	}

	public void updateUsageReportCommerceOrderId(
			long commerceOrderId, long usageReportId)
		throws Exception {

		JSONObject jsonObject = new JSONObject();

		jsonObject.put("commerceOrderId", commerceOrderId);

		patch(
			getAuthorization(), jsonObject.toString(),
			UriComponentsBuilder.fromPath(
				"/o/c/usagereports/{usageReportId}"
			).buildAndExpand(
				usageReportId
			).toUri());
	}

	private JSONObject _fetchProject(String externalReferenceCode)
		throws Exception {

		String response = get(
			getAuthorization(),
			UriComponentsBuilder.fromPath(
				"/o/c/projects/by-external-reference-code/" +
					externalReferenceCode
			).build(
			).encode(
			).toUri());

		if (Validator.isNull(response)) {
			return null;
		}

		return new JSONObject(response);
	}

	private JSONObject _fetchUsageDefinition(String externalReferenceCode)
		throws Exception {

		String response = get(
			getAuthorization(),
			UriComponentsBuilder.fromPath(
				"/o/c/usagedefinitions/by-external-reference-code/" +
					externalReferenceCode
			).build(
			).encode(
			).toUri());

		if (Validator.isNull(response)) {
			return null;
		}

		return new JSONObject(response);
	}

	private boolean _generateUsageReport(
			JSONObject usageRecordJSONObject, YearMonth yearMonth)
		throws Exception {

		double aggregateQuantity = usageRecordJSONObject.getDouble(
			"usedQuantity");
		double entitledQuantity = usageRecordJSONObject.getDouble(
			"entitledQuantity");

		double overageQuantity = aggregateQuantity - entitledQuantity;

		if (overageQuantity <= 0) {
			return false;
		}

		String usageDefinitionExternalReferenceCode =
			usageRecordJSONObject.getString(
				"usageDefinitionExternalReferenceCode");

		JSONObject usageDefinitionJSONObject = _fetchUsageDefinition(
			usageDefinitionExternalReferenceCode);

		if (usageDefinitionJSONObject == null) {
			_log.error(
				"Unable to find usage definition " +
					usageDefinitionExternalReferenceCode);

			return false;
		}

		String projectExternalReferenceCode = usageRecordJSONObject.getString(
			"projectExternalReferenceCode");

		JSONObject projectJSONObject = _fetchProject(
			projectExternalReferenceCode);

		if (projectJSONObject == null) {
			_log.error(
				"Unable to find project " + projectExternalReferenceCode);

			return false;
		}

		String externalReferenceCode = StringBundler.concat(
			"C_USAGE_REPORT_",
			StringUtil.toUpperCase(
				StringUtil.replace(projectExternalReferenceCode, '-', '_')),
			"_", yearMonth.format(_ERC_MONTH_DATE_TIME_FORMATTER));

		// The report is created once and never overwritten, so a re-run never
		// resets a reviewer's approved or completed decision back to ready for
		// review.

		if (_usageReportExists(externalReferenceCode)) {
			if (_log.isInfoEnabled()) {
				_log.info(
					"Skipping usage report " + externalReferenceCode +
						" because it already exists");
			}

			return false;
		}

		double overageRate = usageDefinitionJSONObject.optDouble(
			"overageRate", 0);

		double overageAmount = overageQuantity * overageRate;

		LocalDate generatedDate = YearMonth.now(
			ZoneOffset.UTC
		).atDay(
			1
		);

		JSONObject usageReportJSONObject = new JSONObject();

		usageReportJSONObject.put(
			"accountExternalReferenceCode",
			usageRecordJSONObject.getString("accountExternalReferenceCode")
		).put(
			"aggregateQuantity", aggregateQuantity
		).put(
			"contractExternalReferenceCode",
			usageRecordJSONObject.getString("contractExternalReferenceCode")
		).put(
			"dateFrom", yearMonth.atDay(1) + "T00:00:00Z"
		).put(
			"dateTo", yearMonth.atEndOfMonth() + "T23:59:59Z"
		).put(
			"entitledQuantity", entitledQuantity
		).put(
			"externalReferenceCode", externalReferenceCode
		).put(
			"generatedAt", generatedDate + "T00:00:00Z"
		).put(
			"generatorClassName", UsageReportService.class.getName()
		).put(
			"overageAmount", overageAmount
		).put(
			"overageCurrency",
			usageDefinitionJSONObject.optString("overageCurrency", "USD")
		).put(
			"overageQuantity", overageQuantity
		).put(
			"r_projectToUsageReport_c_projectId",
			projectJSONObject.getLong("id")
		).put(
			"r_usageDefinitionToUsageReport_c_usageDefinitionId",
			usageDefinitionJSONObject.getLong("id")
		).put(
			"reviewStatus", "readyForReview"
		).put(
			"skuExternalReferenceCode",
			usageRecordJSONObject.getString("skuExternalReferenceCode")
		).put(
			"targetClassName",
			"com.liferay.object.model.ObjectDefinition#C_PROJECT"
		).put(
			"targetPK", projectJSONObject.getLong("id")
		).put(
			"targetType", "project"
		);

		put(
			getAuthorization(), usageReportJSONObject.toString(),
			UriComponentsBuilder.fromPath(
				"/o/c/usagereports/by-external-reference-code/" +
					externalReferenceCode
			).build(
			).encode(
			).toUri());

		if (_log.isInfoEnabled()) {
			_log.info(
				"Generated usage report " + externalReferenceCode +
					" ready for review");
		}

		return true;
	}

	/**
	 * Stands in for the datawarehouse query joined against the metered
	 * entitlements. Each record carries the identifying keys the overage order
	 * needs plus the prior month's allotted and consumed quantities; the overage
	 * rate and currency are read live from the matching UsageDefinition. This is
	 * the only mocked step of the flow.
	 */
	private JSONArray _getDataWarehouseUsageRecordsJSONArray() {
		JSONArray usageRecordsJSONArray = new JSONArray();

		usageRecordsJSONArray.put(
			new JSONObject(
			).put(
				"accountExternalReferenceCode", "ACCNT-026"
			).put(
				"contractExternalReferenceCode", "C_CONTRACT_AI_HUB"
			).put(
				"entitledQuantity", 50000000
			).put(
				"projectExternalReferenceCode", "PRJCT-026"
			).put(
				"skuExternalReferenceCode", "PRDCT-AI-HUB"
			).put(
				"usageDefinitionExternalReferenceCode", "ai-tokens-monthly"
			).put(
				"usedQuantity", 62500000
			));

		return usageRecordsJSONArray;
	}

	private boolean _usageReportExists(String externalReferenceCode)
		throws Exception {

		try {
			String response = get(
				getAuthorization(),
				UriComponentsBuilder.fromPath(
					"/o/c/usagereports/by-external-reference-code/" +
						externalReferenceCode
				).build(
				).encode(
				).toUri());

			return Validator.isNotNull(response);
		}
		catch (WebClientResponseException webClientResponseException) {
			int statusCode = webClientResponseException.getStatusCode(
			).value();

			if (statusCode == HttpStatus.NOT_FOUND.value()) {
				return false;
			}

			throw webClientResponseException;
		}
	}

	private static final DateTimeFormatter _ERC_MONTH_DATE_TIME_FORMATTER =
		DateTimeFormatter.ofPattern("yyyy_MM");

	private static final Log _log = LogFactory.getLog(UsageReportService.class);

}