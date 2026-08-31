/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.liferay.headless.admin.user.client.dto.v1_0.Account;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.one.constants.CommerceProductConstants;
import com.liferay.one.constants.PropertyConstants;
import com.liferay.one.exception.GoogleCloudFunctionUnavailableException;
import com.liferay.one.exception.InvalidUsageParameterException;
import com.liferay.one.exception.InvalidUsageProductException;
import com.liferay.one.exception.ProjectNotFoundException;
import com.liferay.one.jira.synchronizer.AccountSynchronizer;
import com.liferay.one.jira.synchronizer.AccountUserAccountRoleSynchronizer;
import com.liferay.one.jira.synchronizer.UserAccountSynchronizer;
import com.liferay.one.model.BaseUsageStrategy;
import com.liferay.one.model.Entitlement;
import com.liferay.one.model.ExperienceUsageStrategy;
import com.liferay.one.model.LDPEventUsageStrategy;
import com.liferay.one.model.LDPUsageStrategy;
import com.liferay.one.model.Project;
import com.liferay.one.permission.BusinessEventPermission;
import com.liferay.one.service.AccountService;
import com.liferay.one.service.CommerceProductService;
import com.liferay.one.service.CommerceSkuService;
import com.liferay.one.service.EntitlementService;
import com.liferay.one.service.GoogleCloudFunctionService;
import com.liferay.one.service.ProjectMembershipService;
import com.liferay.one.service.ProjectService;
import com.liferay.one.service.PropertyService;
import com.liferay.one.service.UserAccountService;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.security.permission.ActionKeys;

import java.util.Arrays;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.InOrder;
import org.mockito.Mockito;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Felipe Veloso
 */
public class ProjectRestControllerTest {

	@BeforeEach
	public void setUp() throws Exception {
		_projectRestController = new ProjectRestController();

		ReflectionTestUtils.setField(
			_projectRestController, "_accountService", _accountService);
		ReflectionTestUtils.setField(
			_projectRestController, "_accountSynchronizer",
			_accountSynchronizer);
		ReflectionTestUtils.setField(
			_projectRestController, "_accountUserAccountRoleSynchronizer",
			_accountUserAccountRoleSynchronizer);
		ReflectionTestUtils.setField(
			_projectRestController, "_businessEventPermission",
			_businessEventPermission);
		ReflectionTestUtils.setField(
			_projectRestController, "_commerceProductService",
			_commerceProductService);
		ReflectionTestUtils.setField(
			_projectRestController, "_commerceSkuService", _commerceSkuService);
		ReflectionTestUtils.setField(
			_projectRestController, "_entitlementService", _entitlementService);
		ReflectionTestUtils.setField(
			_projectRestController, "_googleCloudFunctionService",
			_googleCloudFunctionService);
		ReflectionTestUtils.setField(
			_projectRestController, "_projectMembershipService",
			_projectMembershipService);
		ReflectionTestUtils.setField(
			_projectRestController, "_projectService", _projectService);
		ReflectionTestUtils.setField(
			_projectRestController, "_propertyService", _propertyService);
		ReflectionTestUtils.setField(
			_projectRestController, "_userAccountService", _userAccountService);
		ReflectionTestUtils.setField(
			_projectRestController, "_userAccountSynchronizer",
			_userAccountSynchronizer);

		Mockito.when(
			_projectService.fetchProject(_PROJECT_EXTERNAL_REFERENCE_CODE)
		).thenReturn(
			_createProject()
		);

		_setUpProductName(_PRODUCT_NAME_EXPERIENCE);
	}

	@Test
	public void testDeleteProjectMembershipsDoesNotSyncWhenNothingDeleted()
		throws Exception {

		Mockito.when(
			_projectMembershipService.deleteProjectMembership(
				null, _PROJECT_EXTERNAL_REFERENCE_CODE,
				_ACCOUNT_ROLE_EXTERNAL_REFERENCE_CODE, _USER_ID)
		).thenReturn(
			false
		);

		_deleteProjectMemberships();

		_assertNoSync();
	}

	@Test
	public void testDeleteProjectMembershipsSyncsWhenDeleted()
		throws Exception {

		Mockito.when(
			_projectMembershipService.deleteProjectMembership(
				null, _PROJECT_EXTERNAL_REFERENCE_CODE,
				_ACCOUNT_ROLE_EXTERNAL_REFERENCE_CODE, _USER_ID)
		).thenReturn(
			true
		);

		_whenSyncDependencies();

		_deleteProjectMemberships();

		Mockito.verify(
			_accountSynchronizer
		).syncProjectUserAccounts(
			Mockito.any()
		);

		Mockito.verify(
			_accountSynchronizer
		).syncAccountUserAccounts(
			_account
		);

		Mockito.verify(
			_userAccountSynchronizer
		).syncUserAccountAccounts(
			Mockito.any()
		);

		Mockito.verify(
			_accountUserAccountRoleSynchronizer
		).syncUnassignRole(
			_ACCOUNT_ROLE_EXTERNAL_REFERENCE_CODE,
			_USER_EXTERNAL_REFERENCE_CODE, _PROJECT_EXTERNAL_REFERENCE_CODE
		);
	}

	@Test
	public void testGetUsageChecksPermissionBeforeReadingUsage()
		throws Exception {

		_setUpEntitlements();

		_setUpComposableUsage();

		_getUsage();

		InOrder inOrder = Mockito.inOrder(
			_businessEventPermission, _projectService);

		inOrder.verify(
			_businessEventPermission
		).check(
			ActionKeys.VIEW, null, _PROJECT_EXTERNAL_REFERENCE_CODE
		);

		inOrder.verify(
			_projectService
		).fetchProject(
			_PROJECT_EXTERNAL_REFERENCE_CODE
		);
	}

	@Test
	public void testGetUsageDoesNotReadUsageWhenPermissionIsDenied()
		throws Exception {

		Mockito.doThrow(
			new PrincipalException()
		).when(
			_businessEventPermission
		).check(
			ActionKeys.VIEW, null, _PROJECT_EXTERNAL_REFERENCE_CODE
		);

		Assertions.assertThrows(PrincipalException.class, this::_getUsage);

		Mockito.verifyNoInteractions(_googleCloudFunctionService);
	}

	@Test
	public void testGetUsageEventHistoryRejectsInvalidGranularity()
		throws Exception {

		Assertions.assertThrows(
			InvalidUsageParameterException.class,
			() -> _getUsageEventHistory(
				_END_DATE, "week", _START_DATE_PREVIOUS_MONTH));

		Mockito.verifyNoInteractions(_googleCloudFunctionService);
	}

	@Test
	public void testGetUsageEventHistoryRejectsRangeAboveMaximum()
		throws Exception {

		Assertions.assertThrows(
			InvalidUsageParameterException.class,
			() -> _getUsageEventHistory("2027-07-28", "day", "2026-06-01"));

		Assertions.assertThrows(
			InvalidUsageParameterException.class,
			() -> _getUsageEventHistory("2036-07-28", "month", "2026-06-01"));

		Mockito.verifyNoInteractions(_googleCloudFunctionService);
	}

	@Test
	public void testGetUsageEventHistoryRejectsUnknownProject()
		throws Exception {

		Assertions.assertThrows(
			ProjectNotFoundException.class,
			() -> _projectRestController.getUsageEventHistory(
				null, _PROJECT_EXTERNAL_REFERENCE_CODE_UNKNOWN, _END_DATE,
				"month", _START_DATE_PREVIOUS_MONTH));

		Mockito.verifyNoInteractions(_googleCloudFunctionService);
	}

	@Test
	public void testGetUsageEventHistoryReturnsHistoryWithTotals()
		throws Exception {

		_setUpProductName(_PRODUCT_NAME_LDP);

		_setUpEntitlements(_createEntitlement(1, null, "events", 1000000.0));

		Mockito.when(
			_googleCloudFunctionService.fetchLDPProjectEventHistory(
				_END_DATE, "month", _PROJECT_EXTERNAL_REFERENCE_CODE,
				_START_DATE_PREVIOUS_MONTH)
		).thenReturn(
			_createLDPEventHistory()
		);

		ResponseEntity<String> responseEntity = _getUsageEventHistory(
			_END_DATE, "month", _START_DATE_PREVIOUS_MONTH);

		JSONObject jsonObject = new JSONObject(responseEntity.getBody());

		Assertions.assertEquals(
			1158,
			jsonObject.getBigDecimal(
				"usedCount"
			).intValue());
		Assertions.assertEquals(
			1000000,
			jsonObject.getBigDecimal(
				"maxCount"
			).intValue());

		JSONArray eventHistoryJSONArray = jsonObject.getJSONArray(
			LDPEventUsageStrategy.FIELD_EVENT_HISTORY);

		Assertions.assertEquals(2, eventHistoryJSONArray.length());

		Assertions.assertFalse(
			jsonObject.has(LDPEventUsageStrategy.FIELD_EVENT_SUMMARY));
	}

	@Test
	public void testGetUsageEventSummaryChecksPermissionBeforeReadingUsage()
		throws Exception {

		Mockito.doThrow(
			new PrincipalException()
		).when(
			_businessEventPermission
		).check(
			ActionKeys.VIEW, null, _PROJECT_EXTERNAL_REFERENCE_CODE
		);

		Assertions.assertThrows(
			PrincipalException.class,
			() -> _getUsageEventSummary(_END_DATE, _START_DATE_PREVIOUS_MONTH));

		Mockito.verifyNoInteractions(_googleCloudFunctionService);
	}

	@Test
	public void testGetUsageEventSummaryRejectsInvalidDate() throws Exception {
		Assertions.assertThrows(
			InvalidUsageParameterException.class,
			() -> _getUsageEventSummary(_END_DATE, "06/01/2026"));

		Mockito.verifyNoInteractions(_googleCloudFunctionService);
	}

	@Test
	public void testGetUsageEventSummaryRejectsRangeAboveMaximum()
		throws Exception {

		Assertions.assertThrows(
			InvalidUsageParameterException.class,
			() -> _getUsageEventSummary("2036-07-28", "2026-06-01"));

		Mockito.verifyNoInteractions(_googleCloudFunctionService);
	}

	@Test
	public void testGetUsageEventSummaryRejectsStartDateAfterEndDate()
		throws Exception {

		Assertions.assertThrows(
			InvalidUsageParameterException.class,
			() -> _getUsageEventSummary(_START_DATE_PREVIOUS_MONTH, _END_DATE));

		Mockito.verifyNoInteractions(_googleCloudFunctionService);
	}

	@Test
	public void testGetUsageEventSummaryRejectsUnknownProject()
		throws Exception {

		Assertions.assertThrows(
			ProjectNotFoundException.class,
			() -> _projectRestController.getUsageEventSummary(
				null, _PROJECT_EXTERNAL_REFERENCE_CODE_UNKNOWN, _END_DATE,
				_START_DATE_PREVIOUS_MONTH));

		Mockito.verifyNoInteractions(_googleCloudFunctionService);
	}

	@Test
	public void testGetUsageEventSummaryReturnsEntitlementsWhenDataOpsIsUnavailable()
		throws Exception {

		_setUpProductName(_PRODUCT_NAME_LDP);

		_setUpEntitlements(_createEntitlement(1, null, "events", 1000000.0));

		Mockito.when(
			_googleCloudFunctionService.fetchLDPProjectEventSummary(
				_END_DATE, _PROJECT_EXTERNAL_REFERENCE_CODE,
				_START_DATE_PREVIOUS_MONTH)
		).thenThrow(
			new GoogleCloudFunctionUnavailableException()
		);

		ResponseEntity<String> responseEntity = _getUsageEventSummary(
			_END_DATE, _START_DATE_PREVIOUS_MONTH);

		Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

		JSONObject jsonObject = new JSONObject(responseEntity.getBody());

		Assertions.assertEquals(
			1000000,
			jsonObject.getBigDecimal(
				"maxCount"
			).intValue());
		Assertions.assertFalse(jsonObject.has("usedCount"));
		Assertions.assertFalse(jsonObject.getBoolean("usageDataAvailable"));
	}

	@Test
	public void testGetUsageEventSummaryReturnsEntitlementsWhenDataOpsReturnsNull()
		throws Exception {

		_setUpProductName(_PRODUCT_NAME_LDP);

		_setUpEntitlements(_createEntitlement(1, null, "events", 1000000.0));

		ResponseEntity<String> responseEntity = _getUsageEventSummary(
			_END_DATE, _START_DATE_PREVIOUS_MONTH);

		JSONObject jsonObject = new JSONObject(responseEntity.getBody());

		Assertions.assertEquals(
			1000000,
			jsonObject.getBigDecimal(
				"maxCount"
			).intValue());
		Assertions.assertFalse(jsonObject.has("usedCount"));
		Assertions.assertFalse(
			jsonObject.has(LDPEventUsageStrategy.FIELD_EVENT_SUMMARY));
		Assertions.assertFalse(jsonObject.getBoolean("usageDataAvailable"));
	}

	@Test
	public void testGetUsageEventSummaryReturnsSummaryWithAddOnBuckets()
		throws Exception {

		_setUpProductName(_PRODUCT_NAME_LDP);

		_setUpEntitlements(
			_createEntitlement(1, null, "events", 1000000.0),
			_createEntitlement(2, null, "events-add-on-bucket", 2.0));

		_setUpLDPEventSummary();

		ResponseEntity<String> responseEntity = _getUsageEventSummary(
			_END_DATE, _START_DATE_PREVIOUS_MONTH);

		JSONObject jsonObject = new JSONObject(responseEntity.getBody());

		Assertions.assertEquals(
			2,
			jsonObject.getBigDecimal(
				"addOnBucketCount"
			).intValue());
		Assertions.assertEquals(
			1000000,
			jsonObject.getBigDecimal(
				"baseAllotment"
			).intValue());
		Assertions.assertEquals(
			1400000,
			jsonObject.getBigDecimal(
				"maxCount"
			).intValue());
		Assertions.assertEquals(
			579,
			jsonObject.getBigDecimal(
				"usedCount"
			).intValue());

		JSONArray eventSummaryJSONArray = jsonObject.getJSONArray(
			LDPEventUsageStrategy.FIELD_EVENT_SUMMARY);

		Assertions.assertEquals(2, eventSummaryJSONArray.length());

		JSONObject eventSummaryJSONObject = eventSummaryJSONArray.getJSONObject(
			0);

		Assertions.assertEquals(
			"Liferay", eventSummaryJSONObject.getString("dataSourceName"));
	}

	@Test
	public void testGetUsageEventSummaryTreatsUnlimitedBucketsAsNegativeMaxCount()
		throws Exception {

		_setUpProductName(_PRODUCT_NAME_LDP);

		_setUpEntitlements(
			_createEntitlement(1, null, "events", 1000000.0),
			_createEntitlement(2, "unlimited", "events-add-on-bucket", null));

		_setUpLDPEventSummary();

		ResponseEntity<String> responseEntity = _getUsageEventSummary(
			_END_DATE, _START_DATE_PREVIOUS_MONTH);

		JSONObject jsonObject = new JSONObject(responseEntity.getBody());

		Assertions.assertEquals(
			-1,
			jsonObject.getBigDecimal(
				"maxCount"
			).intValue());
	}

	@Test
	public void testGetUsageEventSummaryTreatsUnlimitedEventsAsNegativeMaxCount()
		throws Exception {

		_setUpProductName(_PRODUCT_NAME_LDP);

		_setUpEntitlements(
			_createEntitlement(1, "unlimited", "events", null),
			_createEntitlement(2, null, "events-add-on-bucket", 2.0));

		_setUpLDPEventSummary();

		ResponseEntity<String> responseEntity = _getUsageEventSummary(
			_END_DATE, _START_DATE_PREVIOUS_MONTH);

		JSONObject jsonObject = new JSONObject(responseEntity.getBody());

		Assertions.assertEquals(
			-1,
			jsonObject.getBigDecimal(
				"maxCount"
			).intValue());
	}

	@Test
	public void testGetUsageExcludesEntitlementsFromUnrelatedProducts()
		throws Exception {

		_setUpEntitlements(
			_createEntitlement(1, null, "sites", 5.0),
			_createEntitlement(
				2, null, "vcpu", 16.0, _SKU_EXTERNAL_REFERENCE_CODE_UNRELATED,
				null));

		_setUpUnrelatedProduct();

		Mockito.when(
			_commerceProductService.fetchProductName(_CPRODUCT_ID_UNRELATED)
		).thenReturn(
			"Liferay PaaS Instance - Backup L"
		);

		_setUpCustomerUsage();

		JSONObject metricsJSONObject = _getMetricsJSONObject(
			_PRODUCT_NAME_SAAS_PLAN);

		JSONObject sitesJSONObject = metricsJSONObject.getJSONObject("sites");

		Assertions.assertEquals(
			5,
			sitesJSONObject.getBigDecimal(
				"maxCount"
			).intValue());

		JSONObject clientExtensionsCapacityCPUJSONObject =
			metricsJSONObject.getJSONObject("clientExtensionsCapacityCPU");

		Assertions.assertEquals(
			0,
			clientExtensionsCapacityCPUJSONObject.getBigDecimal(
				"maxCount"
			).intValue());
	}

	@Test
	public void testGetUsageExperienceProfileReturnsExperienceMetrics()
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
	public void testGetUsageLDPProfileExcludesEntitlementsFromUnrelatedProducts()
		throws Exception {

		_setUpEntitlements(
			_createEntitlement(1, null, "api-requests", 500000.0),
			_createEntitlement(
				2, null, "connectors", 10.0,
				_SKU_EXTERNAL_REFERENCE_CODE_UNRELATED, null));

		_setUpUnrelatedProduct();

		Mockito.when(
			_commerceProductService.fetchProductName(_CPRODUCT_ID_UNRELATED)
		).thenReturn(
			CommerceProductConstants.NAME_LIFERAY_SAAS_PRO_PLAN
		);

		_setUpLDPUsage();

		JSONObject metricsJSONObject = _getMetricsJSONObject(_PRODUCT_NAME_LDP);

		JSONObject apiRequestsJSONObject = metricsJSONObject.getJSONObject(
			LDPUsageStrategy.METRIC_API_REQUESTS);

		Assertions.assertEquals(
			500000,
			apiRequestsJSONObject.getBigDecimal(
				"maxCount"
			).intValue());

		JSONObject connectorsJSONObject = metricsJSONObject.getJSONObject(
			LDPUsageStrategy.METRIC_CONNECTORS);

		Assertions.assertEquals(
			0,
			connectorsJSONObject.getBigDecimal(
				"maxCount"
			).intValue());
	}

	@Test
	public void testGetUsageLDPProfileReadsUsageByProject() throws Exception {
		_setUpEntitlements();

		_setUpLDPUsage();

		_getMetricsJSONObject(_PRODUCT_NAME_LDP);

		Mockito.verify(
			_googleCloudFunctionService
		).fetchLDPProjectUsage(
			_PROJECT_EXTERNAL_REFERENCE_CODE
		);
	}

	@Test
	public void testGetUsageLDPProfileReturnsLDPMetrics() throws Exception {
		_setUpEntitlements(
			_createEntitlement(1, null, "api-requests", 500000.0),
			_createEntitlement(2, null, "connectors", 10.0));

		_setUpLDPUsage();

		JSONObject metricsJSONObject = _getMetricsJSONObject(_PRODUCT_NAME_LDP);

		Assertions.assertEquals(
			Set.of(
				"activeBatchSegments", "activeRealTimeSegments", "apiRequests",
				"connectors"),
			metricsJSONObject.keySet());

		JSONObject apiRequestsJSONObject = metricsJSONObject.getJSONObject(
			LDPUsageStrategy.METRIC_API_REQUESTS);

		Assertions.assertEquals(
			213123,
			apiRequestsJSONObject.getBigDecimal(
				"usedCount"
			).intValue());
		Assertions.assertEquals(
			500000,
			apiRequestsJSONObject.getBigDecimal(
				"maxCount"
			).intValue());

		JSONObject activeBatchSegmentsJSONObject =
			metricsJSONObject.getJSONObject(
				LDPUsageStrategy.METRIC_ACTIVE_BATCH_SEGMENTS);

		Assertions.assertEquals(
			23423,
			activeBatchSegmentsJSONObject.getBigDecimal(
				"usedCount"
			).intValue());
	}

	@Test
	public void testGetUsageProfileSelectsMetricSetNotEntitlements()
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
	public void testGetUsageRejectsMissingProduct() throws Exception {
		Assertions.assertThrows(
			InvalidUsageProductException.class,
			() -> _projectRestController.getUsage(
				null, _PROJECT_EXTERNAL_REFERENCE_CODE, null));
	}

	@Test
	public void testGetUsageRejectsProductWithoutUsageDashboard()
		throws Exception {

		_setUpProductName("SaaS (Legacy)");

		Assertions.assertThrows(
			InvalidUsageProductException.class, this::_getUsage);
	}

	@Test
	public void testGetUsageRejectsUnknownProduct() throws Exception {
		_setUpProductName(null);

		Assertions.assertThrows(
			InvalidUsageProductException.class, this::_getUsage);
	}

	@Test
	public void testGetUsageRejectsUnknownProject() throws Exception {
		Mockito.when(
			_projectService.fetchProject(_PROJECT_EXTERNAL_REFERENCE_CODE)
		).thenReturn(
			null
		);

		Assertions.assertThrows(
			ProjectNotFoundException.class, this::_getUsage);
	}

	@Test
	public void testGetUsageRendersEmDashLimitsWithoutEntitlements()
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
	public void testGetUsageReportsPercentageAboveOneHundredOnOverage()
		throws Exception {

		_setUpEntitlements(_createEntitlement(1, null, "sites", 1.0));

		_setUpProductName(_PRODUCT_NAME_SAAS_PLAN);

		_setUpCustomerUsage();

		JSONObject sitesJSONObject = _getMetricsJSONObject().getJSONObject(
			"sites");

		Assertions.assertEquals("300.0000", sitesJSONObject.get("percentage"));
	}

	@Test
	public void testGetUsageResolvesAccountKeyFromExternalReferenceCode()
		throws Exception {

		_setUpEntitlements(_createEntitlement(1, null, "logs", 300.0));

		_getUsage();

		Mockito.verify(
			_googleCloudFunctionService
		).fetchComposableAccountUsage(
			Mockito.eq(_ACCOUNT_EXTERNAL_REFERENCE_CODE),
			Mockito.matches("\\d{4}-\\d{2}")
		);
	}

	@Test
	public void testGetUsageResolvesAccountKeyFromOwnAccount()
		throws Exception {

		_setUpEntitlements(_createEntitlement(1, null, "logs", 300.0));

		Mockito.when(
			_propertyService.getPropertyValue(
				_ACCOUNT_ID, PropertyConstants.NAME_KORONEIKI_ACCOUNT_KEY)
		).thenReturn(
			_KORONEIKI_ACCOUNT_KEY
		);

		_getUsage();

		Mockito.verify(
			_googleCloudFunctionService
		).fetchComposableAccountUsage(
			Mockito.eq(_KORONEIKI_ACCOUNT_KEY), Mockito.matches("\\d{4}-\\d{2}")
		);
	}

	@Test
	public void testGetUsageReturnsEntitlementsWhenComposableUsageIsAbsent()
		throws Exception {

		_setUpEntitlements(_createEntitlement(1, null, "logs", 300.0));

		Mockito.when(
			_googleCloudFunctionService.fetchComposableAccountUsage(
				Mockito.anyString(), Mockito.anyString())
		).thenReturn(
			new JSONObject(
			).toString()
		);

		JSONObject logStorageJSONObject = _getMetricsJSONObject(
			_PRODUCT_NAME_EXPERIENCE
		).getJSONObject(
			ExperienceUsageStrategy.METRIC_LOG_STORAGE
		);

		Assertions.assertEquals(
			300,
			logStorageJSONObject.getBigDecimal(
				"maxCount"
			).intValue());
		Assertions.assertEquals(
			BaseUsageStrategy.UNIT_GIB,
			logStorageJSONObject.get("maxCountUnits"));
		Assertions.assertEquals("0", logStorageJSONObject.get("percentage"));
		Assertions.assertFalse(logStorageJSONObject.has("usedCount"));
	}

	@Test
	public void testGetUsageReturnsEntitlementsWhenDataOpsReturnsNull()
		throws Exception {

		_setUpEntitlements(_createEntitlement(1, null, "logs", 300.0));

		JSONObject metricsJSONObject = _getMetricsJSONObject();

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
		Assertions.assertFalse(logStorageJSONObject.has("usedCount"));
	}

	@Test
	public void testGetUsageReturnsEntitlementsWhenGoogleCloudFunctionIsUnavailable()
		throws Exception {

		_setUpEntitlements(_createEntitlement(1, null, "logs", 300.0));

		Mockito.when(
			_googleCloudFunctionService.fetchComposableAccountUsage(
				Mockito.anyString(), Mockito.anyString())
		).thenThrow(
			new GoogleCloudFunctionUnavailableException()
		);

		ResponseEntity<String> responseEntity = _getUsage();

		Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

		JSONObject jsonObject = new JSONObject(responseEntity.getBody());

		Assertions.assertFalse(jsonObject.getBoolean("usageDataAvailable"));

		JSONObject logStorageJSONObject = jsonObject.getJSONObject(
			"metrics"
		).getJSONObject(
			ExperienceUsageStrategy.METRIC_LOG_STORAGE
		);

		Assertions.assertEquals(
			300,
			logStorageJSONObject.getBigDecimal(
				"maxCount"
			).intValue());
		Assertions.assertFalse(logStorageJSONObject.has("usedCount"));
	}

	@Test
	public void testGetUsageReturnsEntitlementsWhenSaaSUsageIsNull()
		throws Exception {

		_setUpEntitlements(_createEntitlement(1, null, "sites", 15.0));

		JSONObject sitesJSONObject = _getMetricsJSONObject(
			_PRODUCT_NAME_SAAS_PLAN
		).getJSONObject(
			"sites"
		);

		Assertions.assertEquals(
			15,
			sitesJSONObject.getBigDecimal(
				"maxCount"
			).intValue());
		Assertions.assertFalse(sitesJSONObject.has("usedCount"));
	}

	@Test
	public void testGetUsageReturnsOKWithMetrics() throws Exception {
		_setUpEntitlements();

		_setUpComposableUsage();

		ResponseEntity<String> responseEntity = _getUsage();

		Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

		JSONObject jsonObject = new JSONObject(responseEntity.getBody());

		Assertions.assertFalse(jsonObject.has("variant"));
		Assertions.assertTrue(jsonObject.has("metrics"));
	}

	@Test
	public void testGetUsageSumsContributingEntitlementNames()
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
	public void testGetUsageTreatsUnlimitedGrantTypeAsNegativeMaxCount()
		throws Exception {

		_setUpEntitlements(_createEntitlement(1, "unlimited", "sites", null));

		_setUpProductName(_PRODUCT_NAME_SAAS_PLAN);

		_setUpCustomerUsage();

		JSONObject sitesJSONObject = _getMetricsJSONObject().getJSONObject(
			"sites");

		Assertions.assertEquals(
			-1,
			sitesJSONObject.getBigDecimal(
				"maxCount"
			).intValue());
		Assertions.assertEquals("0", sitesJSONObject.get("percentage"));
	}

	@Test
	public void testGetUsageUpscalesTebibyteLimits() throws Exception {
		_setUpEntitlements(
			_createEntitlement(
				1, null, "storage", 1.0, _SKU_EXTERNAL_REFERENCE_CODE,
				BaseUsageStrategy.UNIT_TIB),
			_createEntitlement(
				2, null, "logs", 300.0, _SKU_EXTERNAL_REFERENCE_CODE,
				BaseUsageStrategy.UNIT_GIB));

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

	@Test
	public void testHandleExceptionMapsInvalidParameterToBadRequest() {
		ResponseEntity<ProblemDetail> responseEntity =
			_projectRestController.handleException(
				new InvalidUsageParameterException(
					"The granularity must be day or month"));

		Assertions.assertEquals(
			HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());

		ProblemDetail problemDetail = responseEntity.getBody();

		Assertions.assertEquals(
			HttpStatus.BAD_REQUEST.value(), problemDetail.getStatus());
		Assertions.assertEquals(
			"The granularity must be day or month", problemDetail.getDetail());
	}

	@Test
	public void testHandleExceptionMapsInvalidProductToBadRequest() {
		ResponseEntity<ProblemDetail> responseEntity =
			_projectRestController.handleException(
				new InvalidUsageProductException("Product is required"));

		Assertions.assertEquals(
			HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());

		ProblemDetail problemDetail = responseEntity.getBody();

		Assertions.assertEquals(
			HttpStatus.BAD_REQUEST.value(), problemDetail.getStatus());
		Assertions.assertEquals(
			"Product is required", problemDetail.getDetail());
	}

	@Test
	public void testPostProjectMembershipsDoesNotSyncWhenNothingAdded()
		throws Exception {

		Mockito.when(
			_projectMembershipService.addProjectMembership(
				null, _PROJECT_EXTERNAL_REFERENCE_CODE,
				_ACCOUNT_ROLE_EXTERNAL_REFERENCE_CODE, _USER_ID)
		).thenReturn(
			false
		);

		_postProjectMemberships();

		_assertNoSync();
	}

	@Test
	public void testPostProjectMembershipsSyncsWhenAdded() throws Exception {
		Mockito.when(
			_projectMembershipService.addProjectMembership(
				null, _PROJECT_EXTERNAL_REFERENCE_CODE,
				_ACCOUNT_ROLE_EXTERNAL_REFERENCE_CODE, _USER_ID)
		).thenReturn(
			true
		);

		_whenSyncDependencies();

		_postProjectMemberships();

		Mockito.verify(
			_accountSynchronizer
		).syncProjectUserAccounts(
			Mockito.any()
		);

		Mockito.verify(
			_accountSynchronizer
		).syncAccountUserAccounts(
			_account
		);

		Mockito.verify(
			_userAccountSynchronizer
		).syncUserAccountAccounts(
			Mockito.any()
		);

		Mockito.verify(
			_accountUserAccountRoleSynchronizer
		).syncAssignRole(
			_ACCOUNT_ROLE_EXTERNAL_REFERENCE_CODE,
			_USER_EXTERNAL_REFERENCE_CODE, _PROJECT_EXTERNAL_REFERENCE_CODE
		);
	}

	private void _assertNoSync() throws Exception {
		Mockito.verifyNoInteractions(_accountService);
		Mockito.verifyNoInteractions(_accountSynchronizer);
		Mockito.verifyNoInteractions(_accountUserAccountRoleSynchronizer);
		Mockito.verifyNoInteractions(_userAccountService);
		Mockito.verifyNoInteractions(_userAccountSynchronizer);
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

		return _createEntitlement(
			entitlementDefinitionId, grantType, name, quantity,
			_SKU_EXTERNAL_REFERENCE_CODE, null);
	}

	private Entitlement _createEntitlement(
		long entitlementDefinitionId, String grantType, String name,
		Double quantity, String skuExternalReferenceCode, String unit) {

		JSONObject jsonObject = new JSONObject(
		).put(
			"entitlementDefinitionToEntitlement",
			new JSONObject(
			).put(
				"id", entitlementDefinitionId
			).put(
				"skuExternalReferenceCode", skuExternalReferenceCode
			).put(
				"unit", unit
			)
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

	private String _createLDPEventHistory() {
		return new JSONObject(
		).put(
			"endDate", _END_DATE
		).put(
			"eventHistory",
			new JSONArray(
			).put(
				new JSONObject(
				).put(
					"date", "2026-06-01"
				).put(
					"eventSummary", _createLDPEventSummaryJSONArray()
				)
			).put(
				new JSONObject(
				).put(
					"date", "2026-07-01"
				).put(
					"eventSummary", _createLDPEventSummaryJSONArray()
				)
			)
		).put(
			"granularity", "month"
		).put(
			"salesforceProjectId", _PROJECT_EXTERNAL_REFERENCE_CODE
		).put(
			"startDate", _START_DATE_PREVIOUS_MONTH
		).toString();
	}

	private String _createLDPEventSummary() {
		return new JSONObject(
		).put(
			"endDate", _END_DATE
		).put(
			"eventSummary", _createLDPEventSummaryJSONArray()
		).put(
			"salesforceProjectId", _PROJECT_EXTERNAL_REFERENCE_CODE
		).put(
			"startDate", _START_DATE_PREVIOUS_MONTH
		).toString();
	}

	private JSONArray _createLDPEventSummaryJSONArray() {
		return new JSONArray(
		).put(
			new JSONObject(
			).put(
				"dataSourceId", "101"
			).put(
				"dataSourceName", "Liferay"
			).put(
				"eventsCount", 123
			)
		).put(
			new JSONObject(
			).put(
				"dataSourceId", "102"
			).put(
				"dataSourceName", "Salesforce"
			).put(
				"eventsCount", 456
			)
		);
	}

	private String _createLDPUsage() {
		return new JSONObject(
		).put(
			"activeBatchSegmentsCount", 23423
		).put(
			"activeRealTimeSegmentsCount", 123
		).put(
			"apiRequestsCount", 213123
		).put(
			"connectorsCount", 123
		).put(
			"month", "2026-07"
		).put(
			"salesforceProjectId", _PROJECT_EXTERNAL_REFERENCE_CODE
		).toString();
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

	private void _deleteProjectMemberships() throws Exception {
		_projectRestController.deleteProjectMemberships(
			null, _PROJECT_EXTERNAL_REFERENCE_CODE, _USER_ID,
			_ACCOUNT_ROLE_EXTERNAL_REFERENCE_CODE);
	}

	private JSONObject _getMetricsJSONObject() throws Exception {
		ResponseEntity<String> responseEntity = _getUsage();

		JSONObject jsonObject = new JSONObject(responseEntity.getBody());

		Assertions.assertFalse(jsonObject.has("variant"));

		return jsonObject.getJSONObject("metrics");
	}

	private JSONObject _getMetricsJSONObject(String productName)
		throws Exception {

		_setUpProductName(productName);

		return _getMetricsJSONObject();
	}

	private ResponseEntity<String> _getUsage() throws Exception {
		return _projectRestController.getUsage(
			null, _PROJECT_EXTERNAL_REFERENCE_CODE,
			_PRODUCT_EXTERNAL_REFERENCE_CODE);
	}

	private ResponseEntity<String> _getUsageEventHistory(
			String endDate, String granularity, String startDate)
		throws Exception {

		return _projectRestController.getUsageEventHistory(
			null, _PROJECT_EXTERNAL_REFERENCE_CODE, endDate, granularity,
			startDate);
	}

	private ResponseEntity<String> _getUsageEventSummary(
			String endDate, String startDate)
		throws Exception {

		return _projectRestController.getUsageEventSummary(
			null, _PROJECT_EXTERNAL_REFERENCE_CODE, endDate, startDate);
	}

	private void _postProjectMemberships() throws Exception {
		_projectRestController.postProjectMemberships(
			null, _PROJECT_EXTERNAL_REFERENCE_CODE, _USER_ID,
			_ACCOUNT_ROLE_EXTERNAL_REFERENCE_CODE);
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
	}

	private void _setUpLDPEventSummary() throws Exception {
		Mockito.when(
			_googleCloudFunctionService.fetchLDPProjectEventSummary(
				_END_DATE, _PROJECT_EXTERNAL_REFERENCE_CODE,
				_START_DATE_PREVIOUS_MONTH)
		).thenReturn(
			_createLDPEventSummary()
		);
	}

	private void _setUpLDPUsage() throws Exception {
		Mockito.when(
			_googleCloudFunctionService.fetchLDPProjectUsage(
				_PROJECT_EXTERNAL_REFERENCE_CODE)
		).thenReturn(
			_createLDPUsage()
		);
	}

	private void _setUpProductName(String productName) throws Exception {
		Mockito.when(
			_commerceSkuService.fetchProductId(_SKU_EXTERNAL_REFERENCE_CODE)
		).thenReturn(
			_CPRODUCT_ID
		);

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

	private void _setUpUnrelatedProduct() throws Exception {
		Mockito.when(
			_commerceSkuService.fetchProductId(
				_SKU_EXTERNAL_REFERENCE_CODE_UNRELATED)
		).thenReturn(
			_CPRODUCT_ID_UNRELATED
		);
	}

	private void _whenSyncDependencies() throws Exception {
		Mockito.when(
			_accountService.getAccount(_ACCOUNT_EXTERNAL_REFERENCE_CODE)
		).thenReturn(
			_account
		);

		Mockito.when(
			_projectService.getProject(_PROJECT_EXTERNAL_REFERENCE_CODE)
		).thenReturn(
			_createProject()
		);

		UserAccount userAccount = new UserAccount();

		userAccount.setExternalReferenceCode(_USER_EXTERNAL_REFERENCE_CODE);
		userAccount.setId(_USER_ID);

		Mockito.when(
			_userAccountService.getUserAccount(_USER_ID)
		).thenReturn(
			userAccount
		);
	}

	private static final String _ACCOUNT_EXTERNAL_REFERENCE_CODE =
		"0015Y00002ABCDEabc";

	private static final long _ACCOUNT_ID = 40001;

	private static final String _ACCOUNT_ROLE_EXTERNAL_REFERENCE_CODE =
		"ACCT-ROLE-001";

	private static final long _CPRODUCT_ID = 55501;

	private static final long _CPRODUCT_ID_UNRELATED = 99901;

	private static final String _END_DATE = "2026-07-28";

	private static final String _KORONEIKI_ACCOUNT_KEY = "abc-123-def";

	private static final String _PRODUCT_EXTERNAL_REFERENCE_CODE = "PRDCT-PAAS";

	private static final String _PRODUCT_NAME_EXPERIENCE =
		CommerceProductConstants.NAME_PAAS_EXPERIENCE;

	private static final String _PRODUCT_NAME_LDP =
		CommerceProductConstants.NAME_LIFERAY_DATA_PLATFORM;

	private static final String _PRODUCT_NAME_SAAS_PLAN =
		CommerceProductConstants.NAME_LIFERAY_SAAS_BUSINESS_PLAN;

	private static final String _PROJECT_EXTERNAL_REFERENCE_CODE = "PRJCT-004";

	private static final String _PROJECT_EXTERNAL_REFERENCE_CODE_UNKNOWN =
		"PRJCT-999";

	private static final String _SKU_EXTERNAL_REFERENCE_CODE = "SKU-001";

	private static final String _SKU_EXTERNAL_REFERENCE_CODE_UNRELATED =
		"SKU-999";

	private static final String _START_DATE_PREVIOUS_MONTH = "2026-06-01";

	private static final String _USER_EXTERNAL_REFERENCE_CODE = "USER-001";

	private static final long _USER_ID = 1L;

	private final Account _account = new Account();
	private final AccountService _accountService = Mockito.mock(
		AccountService.class);
	private final AccountSynchronizer _accountSynchronizer = Mockito.mock(
		AccountSynchronizer.class);
	private final AccountUserAccountRoleSynchronizer
		_accountUserAccountRoleSynchronizer = Mockito.mock(
			AccountUserAccountRoleSynchronizer.class);
	private final BusinessEventPermission _businessEventPermission =
		Mockito.mock(BusinessEventPermission.class);
	private final CommerceProductService _commerceProductService = Mockito.mock(
		CommerceProductService.class);
	private final CommerceSkuService _commerceSkuService = Mockito.mock(
		CommerceSkuService.class);
	private final EntitlementService _entitlementService = Mockito.mock(
		EntitlementService.class);
	private final GoogleCloudFunctionService _googleCloudFunctionService =
		Mockito.mock(GoogleCloudFunctionService.class);
	private final ProjectMembershipService _projectMembershipService =
		Mockito.mock(ProjectMembershipService.class);
	private ProjectRestController _projectRestController;
	private final ProjectService _projectService = Mockito.mock(
		ProjectService.class);
	private final PropertyService _propertyService = Mockito.mock(
		PropertyService.class);
	private final UserAccountService _userAccountService = Mockito.mock(
		UserAccountService.class);
	private final UserAccountSynchronizer _userAccountSynchronizer =
		Mockito.mock(UserAccountSynchronizer.class);

}