/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.one.constants.EntitlementConstants;
import com.liferay.one.constants.UsageDefinitionConstants;
import com.liferay.one.exception.GoogleCloudFunctionUnavailableException;
import com.liferay.one.model.Contract;
import com.liferay.one.model.Entitlement;
import com.liferay.one.model.EntitlementDefinition;
import com.liferay.one.model.LDPEventAllotment;
import com.liferay.one.model.LDPEventSummary;
import com.liferay.one.model.Project;
import com.liferay.one.model.UsageDefinition;
import com.liferay.one.model.UsageReport;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Generates one usage report per Liferay Data Platform project for a calendar
 * month, comparing the events the data warehouse counted against the events
 * the project's entitlements allow.
 *
 * @author Drew Brokke
 */
@Component
public class LDPEventUsageReportService {

	public void generateUsageReports(YearMonth yearMonth) throws Exception {
		if (_log.isInfoEnabled()) {
			_log.info("Generating LDP event usage reports for " + yearMonth);
		}

		UsageDefinition usageDefinition =
			_usageDefinitionService.fetchUsageDefinition(
				UsageDefinitionConstants.
					EXTERNAL_REFERENCE_CODE_EVENTS_MONTHLY);

		if ((usageDefinition == null) ||
			(usageDefinition.getOverageRate() == null)) {

			_log.error(
				"Unable to find an overage rate for usage definition " +
					UsageDefinitionConstants.
						EXTERNAL_REFERENCE_CODE_EVENTS_MONTHLY);

			return;
		}

		Instant startInstant = yearMonth.atDay(
			1
		).atStartOfDay(
			ZoneOffset.UTC
		).toInstant();

		Instant endInstant = yearMonth.plusMonths(
			1
		).atDay(
			1
		).atStartOfDay(
			ZoneOffset.UTC
		).toInstant();

		Map<String, List<Entitlement>> entitlementsByProject =
			_getEntitlementsByProject(endInstant, startInstant);

		int generatedCount = 0;

		for (Map.Entry<String, List<Entitlement>> entry :
				entitlementsByProject.entrySet()) {

			String projectExternalReferenceCode = entry.getKey();

			try {
				if (_generateUsageReport(
						endInstant, entry.getValue(),
						projectExternalReferenceCode, startInstant,
						usageDefinition, yearMonth)) {

					generatedCount++;
				}
			}
			catch (Exception exception) {
				_log.error(
					StringBundler.concat(
						"Unable to generate LDP event usage report for ",
						"project ", projectExternalReferenceCode, " for ",
						yearMonth),
					exception);
			}
		}

		if (_log.isInfoEnabled()) {
			_log.info(
				StringBundler.concat(
					"Generated ", generatedCount, " of ",
					entitlementsByProject.size(),
					" LDP event usage reports for ", yearMonth));
		}
	}

	@Scheduled(
		cron = "${liferay.one.ldp.event.usage.report.cron}", zone = "UTC"
	)
	public void scheduledGenerateUsageReports() {
		YearMonth yearMonth = YearMonth.now(
			ZoneOffset.UTC
		).minusMonths(
			1
		);

		try {
			generateUsageReports(yearMonth);
		}
		catch (Exception exception) {
			_log.error(
				"Unable to generate LDP event usage reports for " + yearMonth,
				exception);
		}
	}

	private String _fetchContractExternalReferenceCode(
			List<Entitlement> entitlements)
		throws Exception {

		for (Entitlement entitlement : entitlements) {
			long contractId = entitlement.getContractId();

			if (contractId <= 0) {
				continue;
			}

			Contract contract = _contractService.fetchContract(contractId);

			if (contract != null) {
				return contract.getExternalReferenceCode();
			}
		}

		return null;
	}

	private LDPEventSummary _fetchLDPEventSummary(
			String projectExternalReferenceCode, YearMonth yearMonth)
		throws Exception {

		String response = null;

		try {
			response = _googleCloudFunctionService.fetchLDPProjectEventSummary(
				String.valueOf(yearMonth.atEndOfMonth()),
				projectExternalReferenceCode,
				String.valueOf(yearMonth.atDay(1)));
		}
		catch (GoogleCloudFunctionUnavailableException
					googleCloudFunctionUnavailableException) {

			_log.error(
				"Unable to read LDP event usage for project " +
					projectExternalReferenceCode,
				googleCloudFunctionUnavailableException);

			return null;
		}

		if (Validator.isNull(response)) {
			return null;
		}

		return new LDPEventSummary(new JSONObject(response));
	}

	private boolean _generateUsageReport(
			Instant endInstant, List<Entitlement> entitlements,
			String projectExternalReferenceCode, Instant startInstant,
			UsageDefinition usageDefinition, YearMonth yearMonth)
		throws Exception {

		LDPEventAllotment ldpEventAllotment = new LDPEventAllotment(
			entitlements);

		if (ldpEventAllotment.isUnlimited()) {
			if (_log.isInfoEnabled()) {
				_log.info(
					"Skipping project " + projectExternalReferenceCode +
						" because its LDP event allotment is unlimited");
			}

			return false;
		}

		String externalReferenceCode = _getExternalReferenceCode(
			projectExternalReferenceCode, yearMonth);

		UsageReport existingUsageReport = _usageReportService.fetchUsageReport(
			externalReferenceCode);

		if (existingUsageReport != null) {
			if (_log.isInfoEnabled()) {
				_log.info(
					"Skipping usage report " + externalReferenceCode +
						" because it already exists");
			}

			return false;
		}

		Project project = _projectService.fetchProject(
			projectExternalReferenceCode);

		if (project == null) {
			_log.error(
				"Unable to find project " + projectExternalReferenceCode);

			return false;
		}

		LDPEventSummary ldpEventSummary = _fetchLDPEventSummary(
			projectExternalReferenceCode, yearMonth);

		if (ldpEventSummary == null) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					StringBundler.concat(
						"Skipping project ", projectExternalReferenceCode,
						" because the data warehouse has no LDP event usage ",
						"for ", yearMonth));
			}

			return false;
		}

		UsageReport usageReport = _usageReportService.addUsageReport(
			ldpEventSummary.getTotalEventsCount(),
			_fetchContractExternalReferenceCode(entitlements), startInstant,
			endInstant.minusMillis(1), ldpEventAllotment.getEntitledQuantity(),
			externalReferenceCode, project,
			_getSkuExternalReferenceCode(entitlements), usageDefinition);

		if (_log.isInfoEnabled()) {
			_log.info(
				StringBundler.concat(
					"Generated usage report ", externalReferenceCode, " with ",
					usageReport.getOverageQuantity(), " overage events"));
		}

		return true;
	}

	private Map<String, List<Entitlement>> _getEntitlementsByProject(
			Instant endInstant, Instant startInstant)
		throws Exception {

		Map<String, List<Entitlement>> entitlementsByProject =
			new LinkedHashMap<>();

		for (Entitlement entitlement :
				_entitlementService.getEntitlements(
					endInstant, _entitlementNames, startInstant)) {

			String projectExternalReferenceCode =
				entitlement.getProjectExternalReferenceCode();

			if (Validator.isNull(projectExternalReferenceCode)) {
				continue;
			}

			List<Entitlement> entitlements =
				entitlementsByProject.computeIfAbsent(
					projectExternalReferenceCode, key -> new ArrayList<>());

			entitlements.add(entitlement);
		}

		return entitlementsByProject;
	}

	private String _getExternalReferenceCode(
		String projectExternalReferenceCode, YearMonth yearMonth) {

		return StringBundler.concat(
			_EXTERNAL_REFERENCE_CODE_PREFIX,
			StringUtil.toUpperCase(
				StringUtil.replace(projectExternalReferenceCode, '-', '_')),
			"_", yearMonth.format(_yearMonthDateTimeFormatter));
	}

	private String _getSkuExternalReferenceCode(
		List<Entitlement> entitlements) {

		for (Entitlement entitlement : entitlements) {
			EntitlementDefinition entitlementDefinition =
				entitlement.getEntitlementDefinition();

			if ((entitlementDefinition != null) &&
				Validator.isNotNull(
					entitlementDefinition.getSkuExternalReferenceCode())) {

				return entitlementDefinition.getSkuExternalReferenceCode();
			}
		}

		return null;
	}

	private static final String _EXTERNAL_REFERENCE_CODE_PREFIX =
		"C_USAGE_REPORT_";

	private static final Log _log = LogFactory.getLog(
		LDPEventUsageReportService.class);

	private static final List<String> _entitlementNames = Arrays.asList(
		EntitlementConstants.NAME_EVENTS,
		EntitlementConstants.NAME_EVENTS_ADD_ON_BUCKET);
	private static final DateTimeFormatter _yearMonthDateTimeFormatter =
		DateTimeFormatter.ofPattern("yyyy_MM");

	@Autowired
	private ContractService _contractService;

	@Autowired
	private EntitlementService _entitlementService;

	@Autowired
	private GoogleCloudFunctionService _googleCloudFunctionService;

	@Autowired
	private ProjectService _projectService;

	@Autowired
	private UsageDefinitionService _usageDefinitionService;

	@Autowired
	private UsageReportService _usageReportService;

}