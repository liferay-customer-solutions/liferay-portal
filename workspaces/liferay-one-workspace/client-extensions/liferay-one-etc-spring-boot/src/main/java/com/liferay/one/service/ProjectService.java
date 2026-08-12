/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.one.constants.CommerceProductConstants;
import com.liferay.one.constants.PropertyConstants;
import com.liferay.one.exception.InvalidUsageProductException;
import com.liferay.one.exception.ProjectNotFoundException;
import com.liferay.one.model.BaseUsageStrategy;
import com.liferay.one.model.Entitlement;
import com.liferay.one.model.EntitlementDefinition;
import com.liferay.one.model.ExperienceUsageStrategy;
import com.liferay.one.model.Project;
import com.liferay.one.model.SaaSUsageStrategy;
import com.liferay.one.salesforce.model.SalesforceProject;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.Validator;

import java.net.URI;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Felipe Veloso
 */
@Component
public class ProjectService extends OneBaseService {

	public Project fetchProject(String externalReferenceCode) throws Exception {
		return _fetchProject(getAuthorization(), externalReferenceCode);
	}

	public Project fetchProject(String externalReferenceCode, Jwt jwt)
		throws Exception {

		return _fetchProject(getAuthorization(jwt), externalReferenceCode);
	}

	public Project getProject(String externalReferenceCode) throws Exception {
		String response = get(
			getAuthorization(),
			UriComponentsBuilder.fromPath(
				"/o/c/projects/by-external-reference-code" +
					"/{externalReferenceCode}"
			).buildAndExpand(
				externalReferenceCode
			).toUri());

		return new Project(new JSONObject(response));
	}

	public List<Project> getProjects(long accountId) throws Exception {
		return getAllItems(
			"/o/c/projects",
			"r_accountEntryToProject_accountEntryId eq '" + accountId + "'",
			Project::new);
	}

	public JSONObject getProjectUsage(
			String productExternalReferenceCode,
			String projectExternalReferenceCode)
		throws Exception {

		if (Validator.isNull(productExternalReferenceCode)) {
			throw new InvalidUsageProductException(
				"Product external reference code is required");
		}

		String productName = _commerceProductService.fetchProductName(
			productExternalReferenceCode);

		if (Validator.isNull(productName)) {
			throw new InvalidUsageProductException(
				"Unable to find product " + productExternalReferenceCode);
		}

		if (!ArrayUtil.contains(
				CommerceProductConstants.NAMES_EXPERIENCE_PRODUCTS,
				productName) &&
			!ArrayUtil.contains(
				CommerceProductConstants.NAMES_SAAS_PLAN_PRODUCTS,
				productName)) {

			throw new InvalidUsageProductException(
				StringBundler.concat(
					"Product ", productExternalReferenceCode,
					" has no usage dashboard: ", productName));
		}

		Project project = fetchProject(projectExternalReferenceCode);

		if (project == null) {
			throw new ProjectNotFoundException(
				"Unable to find project " + projectExternalReferenceCode);
		}

		List<Entitlement> entitlements =
			_entitlementService.getActiveEntitlements(
				projectExternalReferenceCode);

		if (entitlements.isEmpty() && _log.isWarnEnabled()) {
			_log.warn(
				"Unable to find active entitlements for project " +
					projectExternalReferenceCode);
		}

		boolean experienceProduct = ArrayUtil.contains(
			CommerceProductConstants.NAMES_EXPERIENCE_PRODUCTS, productName);

		List<EntitlementDefinition> entitlementDefinitions =
			_getDashboardEntitlementDefinitions(
				_entitlementDefinitionService.getEntitlementDefinitions(
					entitlements),
				experienceProduct);

		entitlements = _getDashboardEntitlements(
			entitlementDefinitions, entitlements);

		BaseUsageStrategy usageStrategy = null;

		if (experienceProduct) {
			LocalDate localDate = LocalDate.now(ZoneOffset.UTC);

			usageStrategy = new ExperienceUsageStrategy(
				entitlementDefinitions, entitlements,
				_googleCloudFunctionService.fetchComposableAccountUsage(
					_getAccountKey(project),
					localDate.format(_BILLING_PERIOD_DATE_TIME_FORMATTER)));
		}
		else {
			usageStrategy = new SaaSUsageStrategy(
				entitlementDefinitions, entitlements,
				_googleCloudFunctionService.fetchCustomerAccountUsage(
					_getAccountKey(project)));
		}

		JSONObject metricsJSONObject = new JSONObject();

		if (usageStrategy.hasUsage()) {
			metricsJSONObject = usageStrategy.toJSONObject();
		}
		else if (_log.isInfoEnabled()) {
			_log.info(
				"Unable to find DataOps usage data for project " +
					projectExternalReferenceCode);
		}

		return new JSONObject(
		).put(
			"metrics", metricsJSONObject
		);
	}

	public void upsertProject(SalesforceProject salesforceProject)
		throws Exception {

		upsertProject(null, salesforceProject);
	}

	public void upsertProject(
			String accountExternalReferenceCode,
			SalesforceProject salesforceProject)
		throws Exception {

		String accountEntryERC = salesforceProject.getAccountId();

		if (Validator.isNull(accountEntryERC)) {
			accountEntryERC = accountExternalReferenceCode;
		}

		if (Validator.isNull(salesforceProject.getId()) ||
			Validator.isNull(salesforceProject.getName()) ||
			Validator.isNull(accountEntryERC)) {

			if (_log.isWarnEnabled()) {
				_log.warn(
					"Unable to upsert project " + salesforceProject.getId() +
						" without an ID, name, and account");
			}

			return;
		}

		JSONObject jsonObject = new JSONObject();

		if (Validator.isNotNull(salesforceProject.getAIHubAccountName())) {
			jsonObject.put(
				"aiHubAccountName", salesforceProject.getAIHubAccountName());
		}

		if (Validator.isNotNull(salesforceProject.getAllowedEmailDomains())) {
			jsonObject.put(
				"allowedEmailDomains",
				salesforceProject.getAllowedEmailDomains());
		}

		if (Validator.isNotNull(salesforceProject.getDataCenterLocation())) {
			jsonObject.put(
				"dataCenterLocation",
				salesforceProject.getDataCenterLocation());
		}

		jsonObject.put("externalReferenceCode", salesforceProject.getId());

		if (Validator.isNotNull(salesforceProject.getFriendlyWorkspaceURL())) {
			jsonObject.put(
				"friendlyWorkspaceURL",
				salesforceProject.getFriendlyWorkspaceURL());
		}

		if (Validator.isNotNull(salesforceProject.getLiferayVersion())) {
			jsonObject.put(
				"liferayVersion", salesforceProject.getLiferayVersion());
		}

		jsonObject.put(
			"name", salesforceProject.getName()
		).put(
			"r_accountEntryToProject_accountEntryERC", accountEntryERC
		);

		if (Validator.isNotNull(
				salesforceProject.getSecurityContactEmailAddress())) {

			jsonObject.put(
				"securityContactEmailAddress",
				salesforceProject.getSecurityContactEmailAddress());
		}

		URI uri = UriComponentsBuilder.fromPath(
			"/o/c/projects/by-external-reference-code/" +
				salesforceProject.getId()
		).build(
		).toUri();

		try {
			patch(getAuthorization(), jsonObject.toString(), uri);
		}
		catch (WebClientResponseException webClientResponseException) {
			int statusCode = webClientResponseException.getStatusCode(
			).value();

			if (statusCode != HttpStatus.NOT_FOUND.value()) {
				throw webClientResponseException;
			}

			put(getAuthorization(), jsonObject.toString(), uri);
		}
	}

	private Project _fetchProject(
			String authorization, String externalReferenceCode)
		throws Exception {

		String response = null;

		try {
			response = get(
				authorization,
				UriComponentsBuilder.fromPath(
					"/o/c/projects/by-external-reference-code" +
						"/{externalReferenceCode}"
				).buildAndExpand(
					externalReferenceCode
				).toUri());
		}
		catch (WebClientResponseException webClientResponseException) {
			int statusCode = webClientResponseException.getStatusCode(
			).value();

			if (statusCode != HttpStatus.NOT_FOUND.value()) {
				throw webClientResponseException;
			}
		}

		if (Validator.isNull(response)) {
			return null;
		}

		return new Project(new JSONObject(response));
	}

	private String _getAccountKey(Project project) throws Exception {
		long accountId = project.getAccountId();

		if (accountId > 0) {
			String value = _propertyService.getPropertyValue(
				accountId, PropertyConstants.NAME_KORONEIKI_ACCOUNT_KEY);

			if (Validator.isNotNull(value)) {
				return value;
			}
		}

		return project.getAccountExternalReferenceCode();
	}

	private List<EntitlementDefinition> _getDashboardEntitlementDefinitions(
			List<EntitlementDefinition> entitlementDefinitions,
			boolean experienceProduct)
		throws Exception {

		List<EntitlementDefinition> dashboardEntitlementDefinitions =
			new ArrayList<>();

		for (EntitlementDefinition entitlementDefinition :
				entitlementDefinitions) {

			String productName = _commerceProductService.fetchProductName(
				entitlementDefinition.getCProductId());

			if (Validator.isNull(productName)) {
				continue;
			}

			if (experienceProduct) {
				if (ArrayUtil.contains(
						CommerceProductConstants.
							NAMES_EXPERIENCE_ENTITLEMENT_PRODUCTS,
						productName)) {

					dashboardEntitlementDefinitions.add(entitlementDefinition);
				}
			}
			else if (ArrayUtil.contains(
						CommerceProductConstants.
							NAMES_SAAS_PLAN_ENTITLEMENT_PRODUCTS,
						productName) ||
					 productName.startsWith(
						 CommerceProductConstants.
							 NAME_PREFIX_LIFERAY_SAAS_ENTITLEMENTS)) {

				dashboardEntitlementDefinitions.add(entitlementDefinition);
			}
		}

		return dashboardEntitlementDefinitions;
	}

	private List<Entitlement> _getDashboardEntitlements(
		List<EntitlementDefinition> entitlementDefinitions,
		List<Entitlement> entitlements) {

		Set<Long> entitlementDefinitionIds = new HashSet<>();

		for (EntitlementDefinition entitlementDefinition :
				entitlementDefinitions) {

			entitlementDefinitionIds.add(
				entitlementDefinition.getEntitlementDefinitionId());
		}

		List<Entitlement> dashboardEntitlements = new ArrayList<>();

		for (Entitlement entitlement : entitlements) {
			if (entitlementDefinitionIds.contains(
					entitlement.getEntitlementDefinitionId())) {

				dashboardEntitlements.add(entitlement);
			}
		}

		return dashboardEntitlements;
	}

	private static final DateTimeFormatter _BILLING_PERIOD_DATE_TIME_FORMATTER =
		DateTimeFormatter.ofPattern("yyyy-MM");

	private static final Log _log = LogFactory.getLog(ProjectService.class);

	@Autowired
	private CommerceProductService _commerceProductService;

	@Autowired
	private EntitlementDefinitionService _entitlementDefinitionService;

	@Autowired
	private EntitlementService _entitlementService;

	@Autowired
	private GoogleCloudFunctionService _googleCloudFunctionService;

	@Autowired
	private PropertyService _propertyService;

}