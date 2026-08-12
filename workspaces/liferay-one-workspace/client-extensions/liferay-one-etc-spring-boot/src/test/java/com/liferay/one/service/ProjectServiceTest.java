/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.one.constants.CommerceProductConstants;
import com.liferay.one.constants.PropertyConstants;
import com.liferay.one.exception.GoogleCloudFunctionUnavailableException;
import com.liferay.one.exception.InvalidUsageProductException;
import com.liferay.one.exception.ProjectNotFoundException;
import com.liferay.one.model.BaseUsageStrategy;
import com.liferay.one.model.Entitlement;
import com.liferay.one.model.EntitlementDefinition;
import com.liferay.one.model.ExperienceUsageStrategy;
import com.liferay.one.model.Project;
import com.liferay.one.model.SaaSUsageStrategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.json.JSONObject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Felipe Veloso
 */
public class ProjectServiceTest {

	@BeforeEach
	public void setUp() throws Exception {
		_projectService = Mockito.spy(new ProjectService());

		ReflectionTestUtils.setField(
			_projectService, "_commerceProductService",
			_commerceProductService);
		ReflectionTestUtils.setField(
			_projectService, "_googleCloudFunctionService",
			_googleCloudFunctionService);
		ReflectionTestUtils.setField(
			_projectService, "_entitlementDefinitionService",
			_entitlementDefinitionService);
		ReflectionTestUtils.setField(
			_projectService, "_entitlementService", _entitlementService);
		ReflectionTestUtils.setField(
			_projectService, "_propertyService", _propertyService);

		Mockito.doReturn(
			_createProject()
		).when(
			_projectService
		).fetchProject(
			_PROJECT_EXTERNAL_REFERENCE_CODE
		);

		Mockito.when(
			_entitlementDefinitionService.getEntitlementDefinitions(
				Mockito.anyList())
		).thenReturn(
			Collections.emptyList()
		);

		_setUpProductName(_PRODUCT_NAME_EXPERIENCE);
	}

	@Test
	public void testGetProjectUsageExcludesEntitlementsFromUnrelatedProducts()
		throws Exception {

		_setUpEntitlements(
			_createEntitlement(1, null, "sites", 5.0),
			_createEntitlement(2, null, "vcpu", 16.0));

		Mockito.when(
			_entitlementDefinitionService.getEntitlementDefinitions(
				Mockito.anyList())
		).thenReturn(
			List.of(
				_createEntitlementDefinition(1, null),
				_createEntitlementDefinition(99901, 2, null))
		);

		Mockito.when(
			_commerceProductService.fetchProductName(99901)
		).thenReturn(
			"Liferay PaaS Instance - Backup L"
		);

		_setUpCustomerUsage();

		JSONObject metricsJSONObject = _getMetricsJSONObject(
			_PRODUCT_NAME_SAAS_PLAN);

		JSONObject sitesJSONObject = metricsJSONObject.getJSONObject(
			SaaSUsageStrategy.METRIC_SITES);

		Assertions.assertEquals(
			5,
			sitesJSONObject.getBigDecimal(
				"maxCount"
			).intValue());

		JSONObject clientExtensionsCapacityCPUJSONObject =
			metricsJSONObject.getJSONObject(
				SaaSUsageStrategy.METRIC_CLIENT_EXTENSIONS_CAPACITY_CPU);

		Assertions.assertEquals(
			0,
			clientExtensionsCapacityCPUJSONObject.getBigDecimal(
				"maxCount"
			).intValue());
	}

	@Test
	public void testGetProjectUsageExperienceProfileReturnsExperienceMetrics()
		throws Exception {

		_setUpEntitlements(
			_createEntitlement(1, null, "extensions-vcpus", 3.0),
			_createEntitlement(2, null, "logs", 300.0));

		_setUpComposableUsage();

		JSONObject metricsJSONObject = _getMetricsJSONObject(
			_PRODUCT_NAME_EXPERIENCE);

		Assertions.assertEquals(
			Set.of(
				"clientExtensionsCPU", "clientExtensionsRAM", "databaseStorage",
				"documentLibraryAndBackupStorage", "logStorage",
				"networkTraffic"),
			metricsJSONObject.keySet());

		JSONObject logStorageJSONObject = metricsJSONObject.getJSONObject(
			ExperienceUsageStrategy.METRIC_LOG_STORAGE);

		Assertions.assertEquals(
			300,
			logStorageJSONObject.getBigDecimal(
				"maxCount"
			).intValue());
		Assertions.assertEquals(
			BaseUsageStrategy.UNIT_GIB,
			logStorageJSONObject.get("maxCountUnits"));
	}

	@Test
	public void testGetProjectUsageProfileSelectsMetricSetNotEntitlements()
		throws Exception {

		_setUpEntitlements(
			_createEntitlement(1, null, "extensions-vcpus", 3.0),
			_createEntitlement(2, null, "logs", 300.0));

		_setUpCustomerUsage();

		JSONObject metricsJSONObject = _getMetricsJSONObject(
			_PRODUCT_NAME_SAAS_PLAN);

		Assertions.assertEquals(
			Set.of(
				"anonymousPageViews", "clientExtensionsCapacityCPU",
				"clientExtensionsCapacityRAM", "monthlyActiveLoggedInUsers",
				"sites", "storageCapacityDocumentLibrary"),
			metricsJSONObject.keySet());
	}

	@Test
	public void testGetProjectUsagePropagatesGoogleCloudFunctionUnavailable()
		throws Exception {

		_setUpEntitlements(_createEntitlement(1, null, "logs", 300.0));

		Mockito.when(
			_googleCloudFunctionService.fetchComposableAccountUsage(
				Mockito.anyString(), Mockito.anyString())
		).thenThrow(
			new GoogleCloudFunctionUnavailableException()
		);

		Assertions.assertThrows(
			GoogleCloudFunctionUnavailableException.class,
			() -> _projectService.getProjectUsage(
				_PRODUCT_EXTERNAL_REFERENCE_CODE,
				_PROJECT_EXTERNAL_REFERENCE_CODE));
	}

	@Test
	public void testGetProjectUsageRejectsMissingProduct() throws Exception {
		Assertions.assertThrows(
			InvalidUsageProductException.class,
			() -> _projectService.getProjectUsage(
				null, _PROJECT_EXTERNAL_REFERENCE_CODE));
	}

	@Test
	public void testGetProjectUsageRejectsProductWithoutUsageDashboard()
		throws Exception {

		_setUpProductName("SaaS (Legacy)");

		Assertions.assertThrows(
			InvalidUsageProductException.class,
			() -> _projectService.getProjectUsage(
				_PRODUCT_EXTERNAL_REFERENCE_CODE,
				_PROJECT_EXTERNAL_REFERENCE_CODE));
	}

	@Test
	public void testGetProjectUsageRejectsUnknownProduct() throws Exception {
		_setUpProductName(null);

		Assertions.assertThrows(
			InvalidUsageProductException.class,
			() -> _projectService.getProjectUsage(
				_PRODUCT_EXTERNAL_REFERENCE_CODE,
				_PROJECT_EXTERNAL_REFERENCE_CODE));
	}

	@Test
	public void testGetProjectUsageRejectsUnknownProject() throws Exception {
		Mockito.doReturn(
			null
		).when(
			_projectService
		).fetchProject(
			_PROJECT_EXTERNAL_REFERENCE_CODE
		);

		Assertions.assertThrows(
			ProjectNotFoundException.class,
			() -> _projectService.getProjectUsage(
				_PRODUCT_EXTERNAL_REFERENCE_CODE,
				_PROJECT_EXTERNAL_REFERENCE_CODE));
	}

	@Test
	public void testGetProjectUsageRendersEmDashLimitsWithoutEntitlements()
		throws Exception {

		_setUpEntitlements();

		_setUpComposableUsage();

		JSONObject metricsJSONObject = _getMetricsJSONObject(
			_PRODUCT_NAME_EXPERIENCE);

		JSONObject logStorageJSONObject = metricsJSONObject.getJSONObject(
			ExperienceUsageStrategy.METRIC_LOG_STORAGE);

		Assertions.assertEquals(
			0,
			logStorageJSONObject.getBigDecimal(
				"maxCount"
			).intValue());
		Assertions.assertEquals("0", logStorageJSONObject.get("percentage"));

		Assertions.assertEquals(
			4,
			logStorageJSONObject.getBigDecimal(
				"usedCount"
			).intValue());
	}

	@Test
	public void testGetProjectUsageReportsPercentageAboveOneHundredOnOverage()
		throws Exception {

		_setUpEntitlements(_createEntitlement(1, null, "sites", 1.0));

		_setUpProductName(_PRODUCT_NAME_SAAS_PLAN);

		_setUpCustomerUsage();

		JSONObject jsonObject = _projectService.getProjectUsage(
			_PRODUCT_EXTERNAL_REFERENCE_CODE, _PROJECT_EXTERNAL_REFERENCE_CODE);

		JSONObject sitesJSONObject = jsonObject.getJSONObject(
			"metrics"
		).getJSONObject(
			SaaSUsageStrategy.METRIC_SITES
		);

		Assertions.assertEquals("300.0000", sitesJSONObject.get("percentage"));
	}

	@Test
	public void testGetProjectUsageResolvesAccountKeyFromExternalReferenceCode()
		throws Exception {

		_setUpEntitlements(_createEntitlement(1, null, "logs", 300.0));

		_projectService.getProjectUsage(
			_PRODUCT_EXTERNAL_REFERENCE_CODE, _PROJECT_EXTERNAL_REFERENCE_CODE);

		Mockito.verify(
			_googleCloudFunctionService
		).fetchComposableAccountUsage(
			Mockito.eq(_ACCOUNT_EXTERNAL_REFERENCE_CODE),
			Mockito.matches("\\d{4}-\\d{2}")
		);
	}

	@Test
	public void testGetProjectUsageResolvesAccountKeyFromOwnAccount()
		throws Exception {

		_setUpEntitlements(_createEntitlement(1, null, "logs", 300.0));

		Mockito.when(
			_propertyService.getPropertyValue(
				_ACCOUNT_ID, PropertyConstants.NAME_KORONEIKI_ACCOUNT_KEY)
		).thenReturn(
			_KORONEIKI_ACCOUNT_KEY
		);

		_projectService.getProjectUsage(
			_PRODUCT_EXTERNAL_REFERENCE_CODE, _PROJECT_EXTERNAL_REFERENCE_CODE);

		Mockito.verify(
			_googleCloudFunctionService
		).fetchComposableAccountUsage(
			Mockito.eq(_KORONEIKI_ACCOUNT_KEY), Mockito.matches("\\d{4}-\\d{2}")
		);
	}

	@Test
	public void testGetProjectUsageReturnsEmptyMetricsWhenComposableUsageIsAbsent()
		throws Exception {

		_setUpEntitlements(_createEntitlement(1, null, "logs", 300.0));

		Mockito.when(
			_googleCloudFunctionService.fetchComposableAccountUsage(
				Mockito.anyString(), Mockito.anyString())
		).thenReturn(
			new JSONObject(
			).toString()
		);

		JSONObject metricsJSONObject = _getMetricsJSONObject(
			_PRODUCT_NAME_EXPERIENCE);

		Assertions.assertTrue(metricsJSONObject.isEmpty());
	}

	@Test
	public void testGetProjectUsageReturnsEmptyMetricsWhenDataOpsReturnsNull()
		throws Exception {

		_setUpEntitlements(_createEntitlement(1, null, "logs", 300.0));

		JSONObject jsonObject = _projectService.getProjectUsage(
			_PRODUCT_EXTERNAL_REFERENCE_CODE, _PROJECT_EXTERNAL_REFERENCE_CODE);

		Assertions.assertTrue(
			jsonObject.getJSONObject(
				"metrics"
			).isEmpty());
	}

	@Test
	public void testGetProjectUsageReturnsEmptyMetricsWhenSaaSUsageIsNull()
		throws Exception {

		_setUpEntitlements(_createEntitlement(1, null, "sites", 15.0));

		JSONObject metricsJSONObject = _getMetricsJSONObject(
			_PRODUCT_NAME_SAAS_PLAN);

		Assertions.assertTrue(metricsJSONObject.isEmpty());
	}

	@Test
	public void testGetProjectUsageSumsContributingEntitlementNames()
		throws Exception {

		_setUpEntitlements(
			_createEntitlement(1, null, "extensions-vcpus", 3.0),
			_createEntitlement(2, null, "extensions-vcpu", 2.0));

		_setUpComposableUsage();

		JSONObject clientExtensionsCPUJSONObject = _getMetricsJSONObject(
			_PRODUCT_NAME_EXPERIENCE
		).getJSONObject(
			ExperienceUsageStrategy.METRIC_CLIENT_EXTENSIONS_CPU
		);

		Assertions.assertEquals(
			5,
			clientExtensionsCPUJSONObject.getBigDecimal(
				"maxCount"
			).intValue());
	}

	@Test
	public void testGetProjectUsageTreatsUnlimitedGrantTypeAsNegativeMaxCount()
		throws Exception {

		_setUpEntitlements(_createEntitlement(1, "unlimited", "sites", null));

		_setUpProductName(_PRODUCT_NAME_SAAS_PLAN);

		_setUpCustomerUsage();

		JSONObject jsonObject = _projectService.getProjectUsage(
			_PRODUCT_EXTERNAL_REFERENCE_CODE, _PROJECT_EXTERNAL_REFERENCE_CODE);

		JSONObject sitesJSONObject = jsonObject.getJSONObject(
			"metrics"
		).getJSONObject(
			SaaSUsageStrategy.METRIC_SITES
		);

		Assertions.assertEquals(
			-1,
			sitesJSONObject.getBigDecimal(
				"maxCount"
			).intValue());
		Assertions.assertEquals("0", sitesJSONObject.get("percentage"));
	}

	@Test
	public void testGetProjectUsageUpscalesTebibyteLimits() throws Exception {
		_setUpEntitlements(
			_createEntitlement(1, null, "storage", 1.0),
			_createEntitlement(2, null, "logs", 300.0));

		Mockito.when(
			_entitlementDefinitionService.getEntitlementDefinitions(
				Mockito.anyList())
		).thenReturn(
			List.of(
				_createEntitlementDefinition(1, BaseUsageStrategy.UNIT_TIB),
				_createEntitlementDefinition(2, BaseUsageStrategy.UNIT_GIB))
		);

		_setUpComposableUsage();

		JSONObject storageJSONObject = _getMetricsJSONObject(
			_PRODUCT_NAME_EXPERIENCE
		).getJSONObject(
			ExperienceUsageStrategy.METRIC_DOCUMENT_LIBRARY_AND_BACKUP_STORAGE
		);

		Assertions.assertEquals(
			1,
			storageJSONObject.getBigDecimal(
				"maxCount"
			).intValue());
		Assertions.assertEquals(
			BaseUsageStrategy.UNIT_TIB, storageJSONObject.get("maxCountUnits"));
	}

	private String _createComposableUsage() {
		return new JSONObject(
		).put(
			"usage",
			new JSONObject(
			).put(
				"logStorage", 4L * 1024L * 1024L * 1024L
			)
		).toString();
	}

	private Entitlement _createEntitlement(
		long entitlementDefinitionId, String grantType, String name,
		Double quantity) {

		JSONObject jsonObject = new JSONObject(
		).put(
			"id", entitlementDefinitionId
		).put(
			"name", name
		).put(
			"r_entitlementDefinitionToEntitlement_c_entitlementDefinitionId",
			entitlementDefinitionId
		);

		if (grantType != null) {
			jsonObject.put("grantType", grantType);
		}

		if (quantity != null) {
			jsonObject.put("quantity", quantity);
		}

		return new Entitlement(jsonObject);
	}

	private EntitlementDefinition _createEntitlementDefinition(
		long cProductId, long entitlementDefinitionId, String unit) {

		return new EntitlementDefinition(
			new JSONObject(
			).put(
				"id", entitlementDefinitionId
			).put(
				"r_commerceProductToEntitlementDefinition_CProductId",
				cProductId
			).put(
				"unit", unit
			));
	}

	private EntitlementDefinition _createEntitlementDefinition(
		long entitlementDefinitionId, String unit) {

		return _createEntitlementDefinition(
			_CPRODUCT_ID, entitlementDefinitionId, unit);
	}

	private Project _createProject() {
		return new Project(
			new JSONObject(
			).put(
				"externalReferenceCode", _PROJECT_EXTERNAL_REFERENCE_CODE
			).put(
				"r_accountEntryToProject_accountEntryERC",
				_ACCOUNT_EXTERNAL_REFERENCE_CODE
			).put(
				"r_accountEntryToProject_accountEntryId", _ACCOUNT_ID
			));
	}

	private JSONObject _getMetricsJSONObject(String productName)
		throws Exception {

		_setUpProductName(productName);

		JSONObject jsonObject = _projectService.getProjectUsage(
			_PRODUCT_EXTERNAL_REFERENCE_CODE, _PROJECT_EXTERNAL_REFERENCE_CODE);

		Assertions.assertFalse(jsonObject.has("variant"));

		return jsonObject.getJSONObject("metrics");
	}

	private void _setUpComposableUsage() throws Exception {
		Mockito.when(
			_googleCloudFunctionService.fetchComposableAccountUsage(
				Mockito.anyString(), Mockito.anyString())
		).thenReturn(
			_createComposableUsage()
		);
	}

	private void _setUpCustomerUsage() throws Exception {
		Mockito.when(
			_googleCloudFunctionService.fetchCustomerAccountUsage(
				Mockito.anyString())
		).thenReturn(
			new JSONObject(
			).put(
				"totalClientExtensionsCapacityRAM", 2
			).put(
				"totalSitesCount", 3
			).toString()
		);
	}

	private void _setUpEntitlements(Entitlement... entitlements)
		throws Exception {

		Mockito.when(
			_entitlementService.getActiveEntitlements(
				_PROJECT_EXTERNAL_REFERENCE_CODE)
		).thenReturn(
			Arrays.asList(entitlements)
		);

		List<EntitlementDefinition> entitlementDefinitions = new ArrayList<>();

		for (Entitlement entitlement : entitlements) {
			entitlementDefinitions.add(
				_createEntitlementDefinition(
					entitlement.getEntitlementDefinitionId(), null));
		}

		Mockito.when(
			_entitlementDefinitionService.getEntitlementDefinitions(
				Mockito.anyList())
		).thenReturn(
			entitlementDefinitions
		);
	}

	private void _setUpProductName(String productName) throws Exception {
		Mockito.when(
			_commerceProductService.fetchProductName(_CPRODUCT_ID)
		).thenReturn(
			productName
		);

		Mockito.when(
			_commerceProductService.fetchProductName(
				_PRODUCT_EXTERNAL_REFERENCE_CODE)
		).thenReturn(
			productName
		);
	}

	private static final String _ACCOUNT_EXTERNAL_REFERENCE_CODE =
		"0015Y00002ABCDEabc";

	private static final long _ACCOUNT_ID = 40001;

	private static final long _CPRODUCT_ID = 55501;

	private static final String _KORONEIKI_ACCOUNT_KEY = "abc-123-def";

	private static final String _PRODUCT_EXTERNAL_REFERENCE_CODE = "PRDCT-PAAS";

	private static final String _PRODUCT_NAME_EXPERIENCE =
		CommerceProductConstants.NAME_PAAS_EXPERIENCE;

	private static final String _PRODUCT_NAME_SAAS_PLAN =
		CommerceProductConstants.NAME_LIFERAY_SAAS_BUSINESS_PLAN;

	private static final String _PROJECT_EXTERNAL_REFERENCE_CODE = "PRJCT-004";

	private final CommerceProductService _commerceProductService = Mockito.mock(
		CommerceProductService.class);
	private final EntitlementDefinitionService _entitlementDefinitionService =
		Mockito.mock(EntitlementDefinitionService.class);
	private final EntitlementService _entitlementService = Mockito.mock(
		EntitlementService.class);
	private final GoogleCloudFunctionService _googleCloudFunctionService =
		Mockito.mock(GoogleCloudFunctionService.class);
	private ProjectService _projectService;
	private final PropertyService _propertyService = Mockito.mock(
		PropertyService.class);

}