/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.one.constants.EntitlementConstants;
import com.liferay.one.constants.EnvironmentConstants;
import com.liferay.one.exception.DisasterRecoveryEntitlementException;
import com.liferay.one.exception.EnvironmentActivationAlreadyRequestedException;
import com.liferay.one.exception.InvalidEnvironmentAdminsException;
import com.liferay.one.model.Environment;
import com.liferay.one.util.KeyedLock;
import com.liferay.petra.string.StringBundler;

import java.util.Map;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Felipe Franca
 */
public class CloudActivationRequestServiceTest {

	@BeforeEach
	public void setUp() throws Exception {
		_cloudActivationRequestService = new CloudActivationRequestService();

		_entitlementService = Mockito.mock(EntitlementService.class);
		_environmentAdminService = Mockito.mock(EnvironmentAdminService.class);
		_environmentService = Mockito.mock(EnvironmentService.class);
		_notificationQueueEntryService = Mockito.mock(
			NotificationQueueEntryService.class);
		_notificationTemplateService = Mockito.mock(
			NotificationTemplateService.class);

		Environment activationEnvironment = Mockito.mock(Environment.class);

		Mockito.when(
			activationEnvironment.getId()
		).thenReturn(
			_ENVIRONMENT_ID
		);

		Mockito.when(
			_environmentService.addActivationEnvironment(
				Mockito.anyLong(), Mockito.anyLong(), Mockito.any(),
				Mockito.anyString(), Mockito.anyString())
		).thenReturn(
			activationEnvironment
		);

		Mockito.when(
			_entitlementService.hasActiveEntitlement(
				_PROJECT_ERC, EntitlementConstants.NAME_DISASTER_RECOVERY)
		).thenReturn(
			true
		);

		ReflectionTestUtils.setField(
			_cloudActivationRequestService, "_entitlementService",
			_entitlementService);
		ReflectionTestUtils.setField(
			_cloudActivationRequestService, "_environmentAdminService",
			_environmentAdminService);
		ReflectionTestUtils.setField(
			_cloudActivationRequestService, "_environmentService",
			_environmentService);
		ReflectionTestUtils.setField(
			_cloudActivationRequestService, "_keyedLock", new KeyedLock());
		ReflectionTestUtils.setField(
			_cloudActivationRequestService, "_notificationQueueEntryService",
			_notificationQueueEntryService);
		ReflectionTestUtils.setField(
			_cloudActivationRequestService, "_notificationTemplateService",
			_notificationTemplateService);
	}

	@Test
	public void testAddActivationRequestAllowsAnalyticsCloudActivationWithExistingSaaSEnvironment()
		throws Exception {

		Environment existingSaaSEnvironment = Mockito.mock(Environment.class);

		Mockito.when(
			existingSaaSEnvironment.getExternalReferenceCode()
		).thenReturn(
			"ENV-SAAS"
		);

		Mockito.when(
			_environmentService.fetchActivationEnvironment(
				_ACCOUNT_ENTRY_ID,
				EnvironmentConstants.OFFERING_ANALYTICS_CLOUD, _PROJECT_ERC)
		).thenReturn(
			null
		);

		Mockito.when(
			_environmentService.fetchActivationEnvironment(
				_ACCOUNT_ENTRY_ID, EnvironmentConstants.OFFERING_SAAS,
				_PROJECT_ERC)
		).thenReturn(
			existingSaaSEnvironment
		);

		Assertions.assertDoesNotThrow(
			() -> _cloudActivationRequestService.addActivationRequest(
				_ACCOUNT_ENTRY_ID, _ACCOUNT_ERC, _CONTRACT_ID,
				"analytics-cloud", _createAnalyticsCloudFieldsJSONObject(),
				_PROJECT_ERC));

		Mockito.verify(
			_environmentService
		).addActivationEnvironment(
			Mockito.eq(_ACCOUNT_ENTRY_ID), Mockito.eq(_CONTRACT_ID),
			Mockito.any(),
			Mockito.eq(EnvironmentConstants.OFFERING_ANALYTICS_CLOUD),
			Mockito.eq(_PROJECT_ERC)
		);
	}

	@Test
	public void testAddActivationRequestAllowsSaaSActivationWithExistingAnalyticsCloudEnvironment()
		throws Exception {

		Environment existingAnalyticsCloudEnvironment = Mockito.mock(
			Environment.class);

		Mockito.when(
			existingAnalyticsCloudEnvironment.getExternalReferenceCode()
		).thenReturn(
			"ENV-ANALYTICS"
		);

		Mockito.when(
			_environmentService.fetchActivationEnvironment(
				_ACCOUNT_ENTRY_ID, EnvironmentConstants.OFFERING_SAAS,
				_PROJECT_ERC)
		).thenReturn(
			null
		);

		Mockito.when(
			_environmentService.fetchActivationEnvironment(
				_ACCOUNT_ENTRY_ID,
				EnvironmentConstants.OFFERING_ANALYTICS_CLOUD, _PROJECT_ERC)
		).thenReturn(
			existingAnalyticsCloudEnvironment
		);

		Assertions.assertDoesNotThrow(
			() -> _cloudActivationRequestService.addActivationRequest(
				_ACCOUNT_ENTRY_ID, _ACCOUNT_ERC, _CONTRACT_ID, "saas",
				_createSaaSFieldsJSONObject(), _PROJECT_ERC));

		Mockito.verify(
			_environmentService
		).addActivationEnvironment(
			Mockito.eq(_ACCOUNT_ENTRY_ID), Mockito.eq(_CONTRACT_ID),
			Mockito.any(), Mockito.eq(EnvironmentConstants.OFFERING_SAAS),
			Mockito.eq(_PROJECT_ERC)
		);
	}

	@Test
	public void testAddActivationRequestAllowsUnentitledSubmissionWithoutDisasterRecoveryRegion()
		throws Exception {

		JSONObject fieldsJSONObject = _createPaaSFieldsJSONObject();

		fieldsJSONObject.remove("disasterRecoveryRegion");

		Mockito.when(
			_environmentService.fetchActivationEnvironment(
				_ACCOUNT_ENTRY_ID, EnvironmentConstants.OFFERING_PAAS,
				_PROJECT_ERC)
		).thenReturn(
			null
		);

		Mockito.when(
			_entitlementService.hasActiveEntitlement(
				_PROJECT_ERC, EntitlementConstants.NAME_DISASTER_RECOVERY)
		).thenReturn(
			false
		);

		_cloudActivationRequestService.addActivationRequest(
			_ACCOUNT_ENTRY_ID, _ACCOUNT_ERC, _CONTRACT_ID, "paas",
			fieldsJSONObject, _PROJECT_ERC);

		Mockito.verify(
			_environmentService
		).addActivationEnvironment(
			_ACCOUNT_ENTRY_ID, _CONTRACT_ID, fieldsJSONObject,
			EnvironmentConstants.OFFERING_PAAS, _PROJECT_ERC
		);

		Mockito.verify(
			_entitlementService, Mockito.never()
		).hasActiveEntitlement(
			_PROJECT_ERC, EntitlementConstants.NAME_DISASTER_RECOVERY
		);
	}

	@Test
	public void testAddActivationRequestCreatesEnvironmentWhenEntitledWithDisasterRecoveryRegion()
		throws Exception {

		JSONObject fieldsJSONObject = _createPaaSFieldsJSONObject();

		Mockito.when(
			_environmentService.fetchActivationEnvironment(
				_ACCOUNT_ENTRY_ID, EnvironmentConstants.OFFERING_PAAS,
				_PROJECT_ERC)
		).thenReturn(
			null
		);

		_cloudActivationRequestService.addActivationRequest(
			_ACCOUNT_ENTRY_ID, _ACCOUNT_ERC, _CONTRACT_ID, "paas",
			fieldsJSONObject, _PROJECT_ERC);

		Mockito.verify(
			_entitlementService
		).hasActiveEntitlement(
			_PROJECT_ERC, EntitlementConstants.NAME_DISASTER_RECOVERY
		);

		Mockito.verify(
			_environmentService
		).addActivationEnvironment(
			_ACCOUNT_ENTRY_ID, _CONTRACT_ID, fieldsJSONObject,
			EnvironmentConstants.OFFERING_PAAS, _PROJECT_ERC
		);
	}

	@Test
	public void testAddActivationRequestCreatesEnvironmentWhenNotificationFails()
		throws Exception {

		JSONObject fieldsJSONObject = _createPaaSFieldsJSONObject();

		Mockito.when(
			_environmentService.fetchActivationEnvironment(
				_ACCOUNT_ENTRY_ID, EnvironmentConstants.OFFERING_PAAS,
				_PROJECT_ERC)
		).thenReturn(
			null
		);

		Mockito.when(
			_notificationTemplateService.getAndProcessTemplateJSONObject(
				Mockito.anyString(), Mockito.anyString(), Mockito.anyMap())
		).thenThrow(
			new RuntimeException("Unable to reach the notification service")
		);

		_cloudActivationRequestService.addActivationRequest(
			_ACCOUNT_ENTRY_ID, _ACCOUNT_ERC, _CONTRACT_ID, "paas",
			fieldsJSONObject, _PROJECT_ERC);

		Mockito.verify(
			_environmentService
		).addActivationEnvironment(
			_ACCOUNT_ENTRY_ID, _CONTRACT_ID, fieldsJSONObject,
			EnvironmentConstants.OFFERING_PAAS, _PROJECT_ERC
		);

		Mockito.verifyNoInteractions(_notificationQueueEntryService);
	}

	@Test
	public void testAddActivationRequestCreatesMultiplePaaSEnvironmentAdmins()
		throws Exception {

		JSONObject fieldsJSONObject = _createPaaSFieldsJSONObject().put(
			"admins",
			new JSONArray(
			).put(
				_createPaaSAdminJSONObject(
					"first@liferay.com", "First", "firstadmin", "Admin")
			).put(
				_createPaaSAdminJSONObject(
					"second@liferay.com", "Second", "secondadmin", "Admin")
			));

		Mockito.when(
			_environmentService.fetchActivationEnvironment(
				_ACCOUNT_ENTRY_ID, EnvironmentConstants.OFFERING_PAAS,
				_PROJECT_ERC)
		).thenReturn(
			null
		);

		Mockito.when(
			_notificationTemplateService.getAndProcessTemplateJSONObject(
				Mockito.eq("SETUP-PAAS-ENVIRONMENT"), Mockito.eq("en_US"),
				Mockito.anyMap())
		).thenReturn(
			_createProcessedTemplateJSONObject()
		);

		_cloudActivationRequestService.addActivationRequest(
			_ACCOUNT_ENTRY_ID, _ACCOUNT_ERC, _CONTRACT_ID, "paas",
			fieldsJSONObject, _PROJECT_ERC);

		Assertions.assertEquals(
			"first@liferay.com",
			fieldsJSONObject.getString("adminEmailAddress"));
		Assertions.assertEquals(
			"First", fieldsJSONObject.getString("adminFirstName"));
		Assertions.assertEquals(
			"Admin", fieldsJSONObject.getString("adminLastName"));
		Assertions.assertEquals(
			"firstadmin", fieldsJSONObject.getString("githubUsername"));

		ArgumentCaptor<JSONArray> adminsArgumentCaptor =
			ArgumentCaptor.forClass(JSONArray.class);

		Mockito.verify(
			_environmentAdminService
		).addEnvironmentAdmins(
			adminsArgumentCaptor.capture(), Mockito.eq(_ENVIRONMENT_ID)
		);

		JSONArray adminsJSONArray = adminsArgumentCaptor.getValue();

		Assertions.assertEquals(2, adminsJSONArray.length());

		JSONObject firstAdminJSONObject = adminsJSONArray.getJSONObject(0);

		Assertions.assertEquals(
			"first@liferay.com",
			firstAdminJSONObject.getString("emailAddress"));

		JSONObject secondAdminJSONObject = adminsJSONArray.getJSONObject(1);

		Assertions.assertEquals(
			"second@liferay.com",
			secondAdminJSONObject.getString("emailAddress"));

		ArgumentCaptor<Map<String, String>> placeholdersArgumentCaptor =
			ArgumentCaptor.forClass(Map.class);

		Mockito.verify(
			_notificationTemplateService
		).getAndProcessTemplateJSONObject(
			Mockito.eq("SETUP-PAAS-ENVIRONMENT"), Mockito.eq("en_US"),
			placeholdersArgumentCaptor.capture()
		);

		Map<String, String> placeholders =
			placeholdersArgumentCaptor.getValue();

		String firstProjectAdmin = _getPaaSProjectAdmin(
			"first@liferay.com", "First", "firstadmin", "Admin");
		String secondProjectAdmin = _getPaaSProjectAdmin(
			"second@liferay.com", "Second", "secondadmin", "Admin");

		Assertions.assertEquals(
			firstProjectAdmin + secondProjectAdmin,
			placeholders.get("PROJECT_ADMIN"));
	}

	@Test
	public void testAddActivationRequestCreatesMultipleSaaSEnvironmentAdmins()
		throws Exception {

		JSONObject fieldsJSONObject = _createSaaSFieldsJSONObject().put(
			"admins",
			new JSONArray(
			).put(
				_createSaaSAdminJSONObject(
					"first@liferay.com", "First", "Admin")
			).put(
				_createSaaSAdminJSONObject(
					"second@liferay.com", "Second", "Admin")
			));

		Mockito.when(
			_environmentService.fetchActivationEnvironment(
				_ACCOUNT_ENTRY_ID, EnvironmentConstants.OFFERING_SAAS,
				_PROJECT_ERC)
		).thenReturn(
			null
		);

		Mockito.when(
			_notificationTemplateService.getAndProcessTemplateJSONObject(
				Mockito.eq("SETUP-SAAS-ENVIRONMENT"), Mockito.eq("en_US"),
				Mockito.anyMap())
		).thenReturn(
			_createProcessedTemplateJSONObject()
		);

		_cloudActivationRequestService.addActivationRequest(
			_ACCOUNT_ENTRY_ID, _ACCOUNT_ERC, _CONTRACT_ID, "saas",
			fieldsJSONObject, _PROJECT_ERC);

		Assertions.assertFalse(fieldsJSONObject.has("githubUsername"));

		ArgumentCaptor<JSONArray> adminsArgumentCaptor =
			ArgumentCaptor.forClass(JSONArray.class);

		Mockito.verify(
			_environmentAdminService
		).addEnvironmentAdmins(
			adminsArgumentCaptor.capture(), Mockito.eq(_ENVIRONMENT_ID)
		);

		Assertions.assertEquals(
			2,
			adminsArgumentCaptor.getValue(
			).length());

		ArgumentCaptor<Map<String, String>> placeholdersArgumentCaptor =
			ArgumentCaptor.forClass(Map.class);

		Mockito.verify(
			_notificationTemplateService
		).getAndProcessTemplateJSONObject(
			Mockito.eq("SETUP-SAAS-ENVIRONMENT"), Mockito.eq("en_US"),
			placeholdersArgumentCaptor.capture()
		);

		Map<String, String> placeholders =
			placeholdersArgumentCaptor.getValue();

		String firstProjectAdmin = _getSaaSProjectAdmin(
			"first@liferay.com", "First", "Admin");
		String secondProjectAdmin = _getSaaSProjectAdmin(
			"second@liferay.com", "Second", "Admin");

		Assertions.assertEquals(
			firstProjectAdmin + secondProjectAdmin,
			placeholders.get("PROJECT_ADMIN"));
	}

	@Test
	public void testAddActivationRequestEscapesHtmlInPlaceholders()
		throws Exception {

		JSONObject fieldsJSONObject = _createPaaSFieldsJSONObject(
		).put(
			"admins",
			new JSONArray(
			).put(
				_createPaaSAdminJSONObject(
					"admin@liferay.com",
					"<a href=\"http://evil\">Click here</a>", "janedoe", "Doe")
			)
		).put(
			"disasterRecoveryRegion", "<script>alert(1)</script>"
		);

		Mockito.when(
			_environmentService.fetchActivationEnvironment(
				_ACCOUNT_ENTRY_ID, EnvironmentConstants.OFFERING_PAAS,
				_PROJECT_ERC)
		).thenReturn(
			null
		);

		_cloudActivationRequestService.addActivationRequest(
			_ACCOUNT_ENTRY_ID, _ACCOUNT_ERC, _CONTRACT_ID, "paas",
			fieldsJSONObject, _PROJECT_ERC);

		ArgumentCaptor<Map<String, String>> placeholdersArgumentCaptor =
			ArgumentCaptor.forClass(Map.class);

		Mockito.verify(
			_notificationTemplateService
		).getAndProcessTemplateJSONObject(
			Mockito.eq("SETUP-PAAS-ENVIRONMENT"), Mockito.eq("en_US"),
			placeholdersArgumentCaptor.capture()
		);

		Map<String, String> placeholders =
			placeholdersArgumentCaptor.getValue();

		Assertions.assertEquals(
			_getPaaSProjectAdmin(
				"admin@liferay.com",
				"&lt;a href=&#34;http://evil&#34;&gt;Click here&lt;/a&gt;",
				"janedoe", "Doe"),
			placeholders.get("PROJECT_ADMIN"));
		Assertions.assertEquals(
			"<strong>Disaster Recovery Region:</strong> " +
				"&lt;script&gt;alert(1)&lt;/script&gt;<br />",
			placeholders.get("DISASTER_RECOVERY_REGION"));
	}

	@Test
	public void testAddActivationRequestFormatsDateAndTimeSubmittedInUTC()
		throws Exception {

		JSONObject fieldsJSONObject = _createPaaSFieldsJSONObject();

		Mockito.when(
			_environmentService.fetchActivationEnvironment(
				_ACCOUNT_ENTRY_ID, EnvironmentConstants.OFFERING_PAAS,
				_PROJECT_ERC)
		).thenReturn(
			null
		);

		Mockito.when(
			_notificationTemplateService.getAndProcessTemplateJSONObject(
				Mockito.eq("SETUP-PAAS-ENVIRONMENT"), Mockito.eq("en_US"),
				Mockito.anyMap())
		).thenReturn(
			_createProcessedTemplateJSONObject()
		);

		_cloudActivationRequestService.addActivationRequest(
			_ACCOUNT_ENTRY_ID, _ACCOUNT_ERC, _CONTRACT_ID, "paas",
			fieldsJSONObject, _PROJECT_ERC);

		ArgumentCaptor<Map<String, String>> placeholdersArgumentCaptor =
			ArgumentCaptor.forClass(Map.class);

		Mockito.verify(
			_notificationTemplateService
		).getAndProcessTemplateJSONObject(
			Mockito.eq("SETUP-PAAS-ENVIRONMENT"), Mockito.eq("en_US"),
			placeholdersArgumentCaptor.capture()
		);

		Map<String, String> placeholders =
			placeholdersArgumentCaptor.getValue();

		Assertions.assertTrue(
			Pattern.matches(
				"[A-Z][a-z]+ \\d{1,2}, \\d{4} at \\d{1,2}:\\d{2} " +
					"(AM|PM) UTC",
				placeholders.get("DATE_AND_TIME_SUBMITTED")));
	}

	@Test
	public void testAddActivationRequestOmitsAnalyticsCloudDisasterRecoveryRegionLineWhenAbsent()
		throws Exception {

		JSONObject fieldsJSONObject = new JSONObject(
		).put(
			"allowedEmailDomains", "liferay.com"
		).put(
			"friendlyURL", "/ac-workspace"
		).put(
			"ownerEmailAddress", "owner@liferay.com"
		).put(
			"region", "Oregon, USA"
		).put(
			"timeZone", "UTC-04:00"
		).put(
			"workspaceName", "ac-workspace"
		);

		Mockito.when(
			_environmentService.fetchActivationEnvironment(
				_ACCOUNT_ENTRY_ID,
				EnvironmentConstants.OFFERING_ANALYTICS_CLOUD, _PROJECT_ERC)
		).thenReturn(
			null
		);

		Mockito.when(
			_notificationTemplateService.getAndProcessTemplateJSONObject(
				Mockito.eq("SETUP-ANALYTICS-CLOUD-ENVIRONMENT"),
				Mockito.eq("en_US"), Mockito.anyMap())
		).thenReturn(
			new JSONObject(
			).put(
				"body", "body"
			).put(
				"from", _EMAIL_ADDRESS_GLOBAL
			).put(
				"fromName", _FROM_NAME
			).put(
				"subject", "subject"
			).put(
				"to", _CLOUD_PROVISIONING_EMAIL_ADDRESS
			)
		);

		_cloudActivationRequestService.addActivationRequest(
			_ACCOUNT_ENTRY_ID, _ACCOUNT_ERC, _CONTRACT_ID, "analytics-cloud",
			fieldsJSONObject, _PROJECT_ERC);

		ArgumentCaptor<Map<String, String>> placeholdersArgumentCaptor =
			ArgumentCaptor.forClass(Map.class);

		Mockito.verify(
			_notificationTemplateService
		).getAndProcessTemplateJSONObject(
			Mockito.eq("SETUP-ANALYTICS-CLOUD-ENVIRONMENT"),
			Mockito.eq("en_US"), placeholdersArgumentCaptor.capture()
		);

		Map<String, String> placeholders =
			placeholdersArgumentCaptor.getValue();

		Assertions.assertEquals(
			"", placeholders.get("DISASTER_RECOVERY_REGION"));
	}

	@Test
	public void testAddActivationRequestOmitsDisasterRecoveryRegionLineWhenAbsent()
		throws Exception {

		JSONObject fieldsJSONObject = _createPaaSFieldsJSONObject();

		fieldsJSONObject.remove("disasterRecoveryRegion");

		Mockito.when(
			_environmentService.fetchActivationEnvironment(
				_ACCOUNT_ENTRY_ID, EnvironmentConstants.OFFERING_PAAS,
				_PROJECT_ERC)
		).thenReturn(
			null
		);

		Mockito.when(
			_notificationTemplateService.getAndProcessTemplateJSONObject(
				Mockito.eq("SETUP-PAAS-ENVIRONMENT"), Mockito.eq("en_US"),
				Mockito.anyMap())
		).thenReturn(
			new JSONObject(
			).put(
				"body", "body"
			).put(
				"from", _EMAIL_ADDRESS_GLOBAL
			).put(
				"fromName", _FROM_NAME
			).put(
				"subject", "subject"
			).put(
				"to", _CLOUD_PROVISIONING_EMAIL_ADDRESS
			)
		);

		_cloudActivationRequestService.addActivationRequest(
			_ACCOUNT_ENTRY_ID, _ACCOUNT_ERC, _CONTRACT_ID, "paas",
			fieldsJSONObject, _PROJECT_ERC);

		ArgumentCaptor<Map<String, String>> placeholdersArgumentCaptor =
			ArgumentCaptor.forClass(Map.class);

		Mockito.verify(
			_notificationTemplateService
		).getAndProcessTemplateJSONObject(
			Mockito.eq("SETUP-PAAS-ENVIRONMENT"), Mockito.eq("en_US"),
			placeholdersArgumentCaptor.capture()
		);

		Map<String, String> placeholders =
			placeholdersArgumentCaptor.getValue();

		Assertions.assertEquals(
			"", placeholders.get("DISASTER_RECOVERY_REGION"));
	}

	@Test
	public void testAddActivationRequestRejectsEmptyAdmins() throws Exception {
		JSONObject fieldsJSONObject = _createPaaSFieldsJSONObject().put(
			"admins", new JSONArray());

		Mockito.when(
			_environmentService.fetchActivationEnvironment(
				_ACCOUNT_ENTRY_ID, EnvironmentConstants.OFFERING_PAAS,
				_PROJECT_ERC)
		).thenReturn(
			null
		);

		Assertions.assertThrows(
			InvalidEnvironmentAdminsException.class,
			() -> _cloudActivationRequestService.addActivationRequest(
				_ACCOUNT_ENTRY_ID, _ACCOUNT_ERC, _CONTRACT_ID, "paas",
				fieldsJSONObject, _PROJECT_ERC));

		Mockito.verify(
			_environmentService, Mockito.never()
		).addActivationEnvironment(
			Mockito.anyLong(), Mockito.anyLong(), Mockito.any(),
			Mockito.anyString(), Mockito.anyString()
		);

		Mockito.verifyNoInteractions(_environmentAdminService);
		Mockito.verifyNoInteractions(_notificationQueueEntryService);
		Mockito.verifyNoInteractions(_notificationTemplateService);
	}

	@Test
	public void testAddActivationRequestRejectsStringAdmins() throws Exception {
		JSONObject fieldsJSONObject = _createSaaSFieldsJSONObject().put(
			"admins",
			new JSONArray(
			).put(
				"admin@liferay.com"
			));

		Mockito.when(
			_environmentService.fetchActivationEnvironment(
				_ACCOUNT_ENTRY_ID, EnvironmentConstants.OFFERING_SAAS,
				_PROJECT_ERC)
		).thenReturn(
			null
		);

		Assertions.assertThrows(
			InvalidEnvironmentAdminsException.class,
			() -> _cloudActivationRequestService.addActivationRequest(
				_ACCOUNT_ENTRY_ID, _ACCOUNT_ERC, _CONTRACT_ID, "saas",
				fieldsJSONObject, _PROJECT_ERC));

		Mockito.verify(
			_environmentService, Mockito.never()
		).addActivationEnvironment(
			Mockito.anyLong(), Mockito.anyLong(), Mockito.any(),
			Mockito.anyString(), Mockito.anyString()
		);

		Mockito.verifyNoInteractions(_environmentAdminService);
		Mockito.verifyNoInteractions(_notificationQueueEntryService);
		Mockito.verifyNoInteractions(_notificationTemplateService);
	}

	@Test
	public void testAddActivationRequestRejectsUnentitledDisasterRecoveryRegion()
		throws Exception {

		JSONObject fieldsJSONObject = _createPaaSFieldsJSONObject();

		Mockito.when(
			_environmentService.fetchActivationEnvironment(
				_ACCOUNT_ENTRY_ID, EnvironmentConstants.OFFERING_PAAS,
				_PROJECT_ERC)
		).thenReturn(
			null
		);

		Mockito.when(
			_entitlementService.hasActiveEntitlement(
				_PROJECT_ERC, EntitlementConstants.NAME_DISASTER_RECOVERY)
		).thenReturn(
			false
		);

		Assertions.assertThrows(
			DisasterRecoveryEntitlementException.class,
			() -> _cloudActivationRequestService.addActivationRequest(
				_ACCOUNT_ENTRY_ID, _ACCOUNT_ERC, _CONTRACT_ID, "paas",
				fieldsJSONObject, _PROJECT_ERC));

		Mockito.verify(
			_environmentService, Mockito.never()
		).addActivationEnvironment(
			Mockito.anyLong(), Mockito.anyLong(), Mockito.any(),
			Mockito.anyString(), Mockito.anyString()
		);

		Mockito.verifyNoInteractions(_notificationTemplateService);
		Mockito.verifyNoInteractions(_notificationQueueEntryService);
	}

	@Test
	public void testAddActivationRequestSendsAnalyticsCloudNotificationWithDisasterRecoveryPlaceholder()
		throws Exception {

		JSONObject fieldsJSONObject = _createAnalyticsCloudFieldsJSONObject();

		Mockito.when(
			_environmentService.fetchActivationEnvironment(
				_ACCOUNT_ENTRY_ID,
				EnvironmentConstants.OFFERING_ANALYTICS_CLOUD, _PROJECT_ERC)
		).thenReturn(
			null
		);

		Mockito.when(
			_notificationTemplateService.getAndProcessTemplateJSONObject(
				Mockito.eq("SETUP-ANALYTICS-CLOUD-ENVIRONMENT"),
				Mockito.eq("en_US"), Mockito.anyMap())
		).thenReturn(
			new JSONObject(
			).put(
				"body", "body"
			).put(
				"from", _EMAIL_ADDRESS_GLOBAL
			).put(
				"fromName", _FROM_NAME
			).put(
				"subject", "subject"
			).put(
				"to", _CLOUD_PROVISIONING_EMAIL_ADDRESS
			)
		);

		_cloudActivationRequestService.addActivationRequest(
			_ACCOUNT_ENTRY_ID, _ACCOUNT_ERC, _CONTRACT_ID, "analytics-cloud",
			fieldsJSONObject, _PROJECT_ERC);

		ArgumentCaptor<Map<String, String>> placeholdersArgumentCaptor =
			ArgumentCaptor.forClass(Map.class);

		Mockito.verify(
			_notificationTemplateService
		).getAndProcessTemplateJSONObject(
			Mockito.eq("SETUP-ANALYTICS-CLOUD-ENVIRONMENT"),
			Mockito.eq("en_US"), placeholdersArgumentCaptor.capture()
		);

		Map<String, String> placeholders =
			placeholdersArgumentCaptor.getValue();

		Assertions.assertEquals(
			"Oregon, USA", placeholders.get("DATA_CENTER_LOCATION"));
		Assertions.assertEquals(
			"<strong>Disaster Recovery Region:</strong> Frankfurt, Germany" +
				"<br />",
			placeholders.get("DISASTER_RECOVERY_REGION"));
	}

	@Test
	public void testAddActivationRequestSendsAnalyticsCloudNotificationWithoutSalesforceAccountLink()
		throws Exception {

		JSONObject fieldsJSONObject = _createAnalyticsCloudFieldsJSONObject();

		Mockito.when(
			_environmentService.fetchActivationEnvironment(
				_ACCOUNT_ENTRY_ID,
				EnvironmentConstants.OFFERING_ANALYTICS_CLOUD, _PROJECT_ERC)
		).thenReturn(
			null
		);

		Mockito.when(
			_notificationTemplateService.getAndProcessTemplateJSONObject(
				Mockito.eq("SETUP-ANALYTICS-CLOUD-ENVIRONMENT"),
				Mockito.eq("en_US"), Mockito.anyMap())
		).thenReturn(
			new JSONObject(
			).put(
				"body", "body"
			).put(
				"from", _EMAIL_ADDRESS_GLOBAL
			).put(
				"fromName", _FROM_NAME
			).put(
				"subject", "subject"
			).put(
				"to", _CLOUD_PROVISIONING_EMAIL_ADDRESS
			)
		);

		_cloudActivationRequestService.addActivationRequest(
			_ACCOUNT_ENTRY_ID, "ACCNT-013", _CONTRACT_ID, "analytics-cloud",
			fieldsJSONObject, _PROJECT_ERC);

		ArgumentCaptor<Map<String, String>> placeholdersArgumentCaptor =
			ArgumentCaptor.forClass(Map.class);

		Mockito.verify(
			_notificationTemplateService
		).getAndProcessTemplateJSONObject(
			Mockito.eq("SETUP-ANALYTICS-CLOUD-ENVIRONMENT"),
			Mockito.eq("en_US"), placeholdersArgumentCaptor.capture()
		);

		Map<String, String> placeholders =
			placeholdersArgumentCaptor.getValue();

		Assertions.assertEquals(
			"< none >", placeholders.get("PROJECT_SALESFORCE_ACCOUNT_LINK"));
	}

	@Test
	public void testAddActivationRequestSendsAnalyticsCloudNotificationWithoutSalesforceProjectLinkForInvalidProjectId()
		throws Exception {

		JSONObject fieldsJSONObject = _createAnalyticsCloudFieldsJSONObject();

		Mockito.when(
			_environmentService.fetchActivationEnvironment(
				_ACCOUNT_ENTRY_ID,
				EnvironmentConstants.OFFERING_ANALYTICS_CLOUD, _PROJECT_ERC)
		).thenReturn(
			null
		);

		Mockito.when(
			_notificationTemplateService.getAndProcessTemplateJSONObject(
				Mockito.eq("SETUP-ANALYTICS-CLOUD-ENVIRONMENT"),
				Mockito.eq("en_US"), Mockito.anyMap())
		).thenReturn(
			_createProcessedTemplateJSONObject()
		);

		_cloudActivationRequestService.addActivationRequest(
			_ACCOUNT_ENTRY_ID, _ACCOUNT_ERC, _CONTRACT_ID, "analytics-cloud",
			fieldsJSONObject, _PROJECT_ERC);

		ArgumentCaptor<Map<String, String>> placeholdersArgumentCaptor =
			ArgumentCaptor.forClass(Map.class);

		Mockito.verify(
			_notificationTemplateService
		).getAndProcessTemplateJSONObject(
			Mockito.eq("SETUP-ANALYTICS-CLOUD-ENVIRONMENT"),
			Mockito.eq("en_US"), placeholdersArgumentCaptor.capture()
		);

		Map<String, String> placeholders =
			placeholdersArgumentCaptor.getValue();

		Assertions.assertEquals(
			"< none >", placeholders.get("PROJECT_SALESFORCE_PROJECT_LINK"));
	}

	@Test
	public void testAddActivationRequestSendsAnalyticsCloudNotificationWithSalesforceAccountLink()
		throws Exception {

		JSONObject fieldsJSONObject = _createAnalyticsCloudFieldsJSONObject();

		Mockito.when(
			_environmentService.fetchActivationEnvironment(
				_ACCOUNT_ENTRY_ID,
				EnvironmentConstants.OFFERING_ANALYTICS_CLOUD, _PROJECT_ERC)
		).thenReturn(
			null
		);

		Mockito.when(
			_notificationTemplateService.getAndProcessTemplateJSONObject(
				Mockito.eq("SETUP-ANALYTICS-CLOUD-ENVIRONMENT"),
				Mockito.eq("en_US"), Mockito.anyMap())
		).thenReturn(
			new JSONObject(
			).put(
				"body", "body"
			).put(
				"from", _EMAIL_ADDRESS_GLOBAL
			).put(
				"fromName", _FROM_NAME
			).put(
				"subject", "subject"
			).put(
				"to", _CLOUD_PROVISIONING_EMAIL_ADDRESS
			)
		);

		_cloudActivationRequestService.addActivationRequest(
			_ACCOUNT_ENTRY_ID, _SALESFORCE_ACCOUNT_ID, _CONTRACT_ID,
			"analytics-cloud", fieldsJSONObject, _PROJECT_ERC);

		ArgumentCaptor<Map<String, String>> placeholdersArgumentCaptor =
			ArgumentCaptor.forClass(Map.class);

		Mockito.verify(
			_notificationTemplateService
		).getAndProcessTemplateJSONObject(
			Mockito.eq("SETUP-ANALYTICS-CLOUD-ENVIRONMENT"),
			Mockito.eq("en_US"), placeholdersArgumentCaptor.capture()
		);

		Map<String, String> placeholders =
			placeholdersArgumentCaptor.getValue();

		Assertions.assertEquals(
			"https://liferay.lightning.force.com/lightning/r/Account/" +
				_SALESFORCE_ACCOUNT_ID + "/view",
			placeholders.get("PROJECT_SALESFORCE_ACCOUNT_LINK"));
	}

	@Test
	public void testAddActivationRequestSendsAnalyticsCloudNotificationWithSalesforceProjectLink()
		throws Exception {

		JSONObject fieldsJSONObject = _createAnalyticsCloudFieldsJSONObject();

		Mockito.when(
			_entitlementService.hasActiveEntitlement(
				_SALESFORCE_PROJECT_ID,
				EntitlementConstants.NAME_DISASTER_RECOVERY)
		).thenReturn(
			true
		);

		Mockito.when(
			_environmentService.fetchActivationEnvironment(
				_ACCOUNT_ENTRY_ID,
				EnvironmentConstants.OFFERING_ANALYTICS_CLOUD,
				_SALESFORCE_PROJECT_ID)
		).thenReturn(
			null
		);

		Mockito.when(
			_notificationTemplateService.getAndProcessTemplateJSONObject(
				Mockito.eq("SETUP-ANALYTICS-CLOUD-ENVIRONMENT"),
				Mockito.eq("en_US"), Mockito.anyMap())
		).thenReturn(
			new JSONObject(
			).put(
				"body", "body"
			).put(
				"from", _EMAIL_ADDRESS_GLOBAL
			).put(
				"fromName", _FROM_NAME
			).put(
				"subject", "subject"
			).put(
				"to", _CLOUD_PROVISIONING_EMAIL_ADDRESS
			)
		);

		_cloudActivationRequestService.addActivationRequest(
			_ACCOUNT_ENTRY_ID, _ACCOUNT_ERC, _CONTRACT_ID, "analytics-cloud",
			fieldsJSONObject, _SALESFORCE_PROJECT_ID);

		ArgumentCaptor<Map<String, String>> placeholdersArgumentCaptor =
			ArgumentCaptor.forClass(Map.class);

		Mockito.verify(
			_notificationTemplateService
		).getAndProcessTemplateJSONObject(
			Mockito.eq("SETUP-ANALYTICS-CLOUD-ENVIRONMENT"),
			Mockito.eq("en_US"), placeholdersArgumentCaptor.capture()
		);

		Map<String, String> placeholders =
			placeholdersArgumentCaptor.getValue();

		Assertions.assertEquals(
			"https://liferay.lightning.force.com/lightning/r/Project__c/" +
				_SALESFORCE_PROJECT_ID + "/view",
			placeholders.get("PROJECT_SALESFORCE_PROJECT_LINK"));
	}

	@Test
	public void testAddActivationRequestSendsNotificationWhenEnvironmentAdminCreationFails()
		throws Exception {

		JSONObject fieldsJSONObject = _createPaaSFieldsJSONObject();

		Mockito.when(
			_environmentService.fetchActivationEnvironment(
				_ACCOUNT_ENTRY_ID, EnvironmentConstants.OFFERING_PAAS,
				_PROJECT_ERC)
		).thenReturn(
			null
		);

		Mockito.doThrow(
			new RuntimeException(
				"Unable to reach the environment admin service")
		).when(
			_environmentAdminService
		).addEnvironmentAdmins(
			Mockito.any(), Mockito.anyLong()
		);

		Mockito.when(
			_notificationTemplateService.getAndProcessTemplateJSONObject(
				Mockito.eq("SETUP-PAAS-ENVIRONMENT"), Mockito.eq("en_US"),
				Mockito.anyMap())
		).thenReturn(
			_createProcessedTemplateJSONObject()
		);

		_cloudActivationRequestService.addActivationRequest(
			_ACCOUNT_ENTRY_ID, _ACCOUNT_ERC, _CONTRACT_ID, "paas",
			fieldsJSONObject, _PROJECT_ERC);

		Mockito.verify(
			_notificationQueueEntryService
		).addNotificationQueueEntry(
			_EMAIL_ADDRESS_GLOBAL, _FROM_NAME,
			_CLOUD_PROVISIONING_EMAIL_ADDRESS, "subject", "body"
		);
	}

	@Test
	public void testAddActivationRequestSendsPaaSNotification()
		throws Exception {

		JSONObject fieldsJSONObject = _createPaaSFieldsJSONObject();

		Mockito.when(
			_environmentService.fetchActivationEnvironment(
				_ACCOUNT_ENTRY_ID, EnvironmentConstants.OFFERING_PAAS,
				_PROJECT_ERC)
		).thenReturn(
			null
		);

		Mockito.when(
			_notificationTemplateService.getAndProcessTemplateJSONObject(
				Mockito.eq("SETUP-PAAS-ENVIRONMENT"), Mockito.eq("en_US"),
				Mockito.anyMap())
		).thenReturn(
			new JSONObject(
			).put(
				"body", "body"
			).put(
				"from", _EMAIL_ADDRESS_GLOBAL
			).put(
				"fromName", _FROM_NAME
			).put(
				"subject", "subject"
			).put(
				"to", _CLOUD_PROVISIONING_EMAIL_ADDRESS
			)
		);

		_cloudActivationRequestService.addActivationRequest(
			_ACCOUNT_ENTRY_ID, _ACCOUNT_ERC, _CONTRACT_ID, "paas",
			fieldsJSONObject, _PROJECT_ERC);

		Mockito.verify(
			_environmentService
		).addActivationEnvironment(
			_ACCOUNT_ENTRY_ID, _CONTRACT_ID, fieldsJSONObject,
			EnvironmentConstants.OFFERING_PAAS, _PROJECT_ERC
		);

		ArgumentCaptor<Map<String, String>> placeholdersArgumentCaptor =
			ArgumentCaptor.forClass(Map.class);

		Mockito.verify(
			_notificationTemplateService
		).getAndProcessTemplateJSONObject(
			Mockito.eq("SETUP-PAAS-ENVIRONMENT"), Mockito.eq("en_US"),
			placeholdersArgumentCaptor.capture()
		);

		Map<String, String> placeholders =
			placeholdersArgumentCaptor.getValue();

		Assertions.assertEquals("paaspending", placeholders.get("PROJECT_ID"));
		Assertions.assertEquals(
			_PROJECT_ERC, placeholders.get("PROJECT_EXTERNAL_REFERENCE_CODE"));
		Assertions.assertEquals(
			_getPaaSProjectAdmin("admin@liferay.com", "Jane", "janedoe", "Doe"),
			placeholders.get("PROJECT_ADMIN"));
		Assertions.assertEquals(
			"DXP 2024.Q3.1", placeholders.get("PROJECT_VERSION"));
		Assertions.assertEquals(
			"oregon-usa", placeholders.get("PRIMARY_DATA_CENTER_REGION"));
		Assertions.assertEquals(
			"<strong>Disaster Recovery Region:</strong> frankfurt-germany" +
				"<br />",
			placeholders.get("DISASTER_RECOVERY_REGION"));

		Mockito.verify(
			_notificationQueueEntryService
		).addNotificationQueueEntry(
			_EMAIL_ADDRESS_GLOBAL, _FROM_NAME,
			_CLOUD_PROVISIONING_EMAIL_ADDRESS, "subject", "body"
		);
	}

	@Test
	public void testAddActivationRequestSendsSaaSNotification()
		throws Exception {

		JSONObject fieldsJSONObject = _createSaaSFieldsJSONObject();

		Mockito.when(
			_environmentService.fetchActivationEnvironment(
				_ACCOUNT_ENTRY_ID, EnvironmentConstants.OFFERING_SAAS,
				_PROJECT_ERC)
		).thenReturn(
			null
		);

		Mockito.when(
			_notificationTemplateService.getAndProcessTemplateJSONObject(
				Mockito.eq("SETUP-SAAS-ENVIRONMENT"), Mockito.eq("en_US"),
				Mockito.anyMap())
		).thenReturn(
			new JSONObject(
			).put(
				"body", "body"
			).put(
				"from", _EMAIL_ADDRESS_GLOBAL
			).put(
				"fromName", _FROM_NAME
			).put(
				"subject", "subject"
			).put(
				"to", _CLOUD_PROVISIONING_EMAIL_ADDRESS
			)
		);

		_cloudActivationRequestService.addActivationRequest(
			_ACCOUNT_ENTRY_ID, _ACCOUNT_ERC, _CONTRACT_ID, "saas",
			fieldsJSONObject, _PROJECT_ERC);

		Mockito.verify(
			_environmentService
		).addActivationEnvironment(
			_ACCOUNT_ENTRY_ID, _CONTRACT_ID, fieldsJSONObject,
			EnvironmentConstants.OFFERING_SAAS, _PROJECT_ERC
		);

		ArgumentCaptor<Map<String, String>> placeholdersArgumentCaptor =
			ArgumentCaptor.forClass(Map.class);

		Mockito.verify(
			_notificationTemplateService
		).getAndProcessTemplateJSONObject(
			Mockito.eq("SETUP-SAAS-ENVIRONMENT"), Mockito.eq("en_US"),
			placeholdersArgumentCaptor.capture()
		);

		Map<String, String> placeholders =
			placeholdersArgumentCaptor.getValue();

		Assertions.assertEquals("saas-omni", placeholders.get("PROJECT_ID"));
		Assertions.assertEquals(
			_PROJECT_ERC, placeholders.get("PROJECT_EXTERNAL_REFERENCE_CODE"));
		Assertions.assertEquals(
			_getSaaSProjectAdmin("admin@liferay.com", "Jane", "Doe"),
			placeholders.get("PROJECT_ADMIN"));
		Assertions.assertEquals("Europe", placeholders.get("PRIMARY_REGION"));
		Assertions.assertEquals(
			"owner@liferay.com",
			placeholders.get("ANALYTICS_CLOUD_OWNER_EMAIL_ADDRESS"));

		Mockito.verify(
			_notificationQueueEntryService
		).addNotificationQueueEntry(
			_EMAIL_ADDRESS_GLOBAL, _FROM_NAME,
			_CLOUD_PROVISIONING_EMAIL_ADDRESS, "subject", "body"
		);
	}

	@Test
	public void testAddActivationRequestThrowsForConcurrentDuplicate()
		throws Exception {

		Environment environment = Mockito.mock(Environment.class);

		Mockito.when(
			environment.getExternalReferenceCode()
		).thenReturn(
			"ENV-1"
		);

		Mockito.when(
			_environmentService.fetchActivationEnvironment(
				_ACCOUNT_ENTRY_ID, EnvironmentConstants.OFFERING_PAAS,
				_PROJECT_ERC)
		).thenReturn(
			null, null, null, environment
		);

		_cloudActivationRequestService.addActivationRequest(
			_ACCOUNT_ENTRY_ID, _ACCOUNT_ERC, _CONTRACT_ID, "paas",
			_createPaaSFieldsJSONObject(), _PROJECT_ERC);

		Assertions.assertThrows(
			EnvironmentActivationAlreadyRequestedException.class,
			() -> _cloudActivationRequestService.addActivationRequest(
				_ACCOUNT_ENTRY_ID, _ACCOUNT_ERC, _CONTRACT_ID, "paas",
				_createPaaSFieldsJSONObject(), _PROJECT_ERC));

		Mockito.verify(
			_environmentService, Mockito.times(1)
		).addActivationEnvironment(
			Mockito.anyLong(), Mockito.anyLong(), Mockito.any(),
			Mockito.anyString(), Mockito.anyString()
		);
	}

	@Test
	public void testAddActivationRequestThrowsForDuplicate() throws Exception {
		Environment environment = Mockito.mock(Environment.class);

		Mockito.when(
			environment.getExternalReferenceCode()
		).thenReturn(
			"ENV-1"
		);

		Mockito.when(
			_environmentService.fetchActivationEnvironment(
				_ACCOUNT_ENTRY_ID, EnvironmentConstants.OFFERING_PAAS,
				_PROJECT_ERC)
		).thenReturn(
			environment
		);

		Assertions.assertThrows(
			EnvironmentActivationAlreadyRequestedException.class,
			() -> _cloudActivationRequestService.addActivationRequest(
				_ACCOUNT_ENTRY_ID, _ACCOUNT_ERC, _CONTRACT_ID, "paas",
				_createPaaSFieldsJSONObject(), _PROJECT_ERC));

		Mockito.verify(
			_environmentService, Mockito.never()
		).addActivationEnvironment(
			Mockito.anyLong(), Mockito.anyLong(), Mockito.any(),
			Mockito.anyString(), Mockito.anyString()
		);

		Mockito.verifyNoInteractions(_notificationTemplateService);
		Mockito.verifyNoInteractions(_notificationQueueEntryService);
	}

	@Test
	public void testAddActivationRequestUsesBlankTextForOmittedAnalyticsCloudPlaceholders()
		throws Exception {

		JSONObject fieldsJSONObject = _createAnalyticsCloudFieldsJSONObject();

		fieldsJSONObject.remove("allowedEmailDomains");
		fieldsJSONObject.remove("friendlyURL");
		fieldsJSONObject.remove("timeZone");

		Mockito.when(
			_environmentService.fetchActivationEnvironment(
				_ACCOUNT_ENTRY_ID,
				EnvironmentConstants.OFFERING_ANALYTICS_CLOUD, _PROJECT_ERC)
		).thenReturn(
			null
		);

		Mockito.when(
			_notificationTemplateService.getAndProcessTemplateJSONObject(
				Mockito.eq("SETUP-ANALYTICS-CLOUD-ENVIRONMENT"),
				Mockito.eq("en_US"), Mockito.anyMap())
		).thenReturn(
			_createProcessedTemplateJSONObject()
		);

		_cloudActivationRequestService.addActivationRequest(
			_ACCOUNT_ENTRY_ID, _ACCOUNT_ERC, _CONTRACT_ID, "analytics-cloud",
			fieldsJSONObject, _PROJECT_ERC);

		ArgumentCaptor<Map<String, String>> placeholdersArgumentCaptor =
			ArgumentCaptor.forClass(Map.class);

		Mockito.verify(
			_notificationTemplateService
		).getAndProcessTemplateJSONObject(
			Mockito.eq("SETUP-ANALYTICS-CLOUD-ENVIRONMENT"),
			Mockito.eq("en_US"), placeholdersArgumentCaptor.capture()
		);

		Map<String, String> placeholders =
			placeholdersArgumentCaptor.getValue();

		Assertions.assertEquals(
			"< none >", placeholders.get("ALLOWED_EMAIL_DOMAINS"));
		Assertions.assertEquals("< none >", placeholders.get("TIME_ZONE"));
		Assertions.assertEquals(
			"< none >", placeholders.get("WORKSPACE_FRIENDLY_URL"));
	}

	private JSONObject _createAnalyticsCloudFieldsJSONObject() {
		return new JSONObject(
		).put(
			"allowedEmailDomains", "liferay.com"
		).put(
			"disasterRecoveryRegion", "Frankfurt, Germany"
		).put(
			"friendlyURL", "/ac-workspace"
		).put(
			"ownerEmailAddress", "owner@liferay.com"
		).put(
			"region", "Oregon, USA"
		).put(
			"timeZone", "UTC-04:00"
		).put(
			"workspaceName", "ac-workspace"
		);
	}

	private JSONObject _createPaaSAdminJSONObject(
		String emailAddress, String firstName, String githubUserName,
		String lastName) {

		return new JSONObject(
		).put(
			"emailAddress", emailAddress
		).put(
			"firstName", firstName
		).put(
			"githubUsername", githubUserName
		).put(
			"lastName", lastName
		);
	}

	private JSONObject _createPaaSFieldsJSONObject() {
		return new JSONObject(
		).put(
			"admins",
			new JSONArray(
			).put(
				_createPaaSAdminJSONObject(
					"admin@liferay.com", "Jane", "janedoe", "Doe")
			)
		).put(
			"disasterRecoveryRegion", "frankfurt-germany"
		).put(
			"dxpVersion", "DXP 2024.Q3.1"
		).put(
			"projectId", "paaspending"
		).put(
			"region", "oregon-usa"
		);
	}

	private JSONObject _createProcessedTemplateJSONObject() {
		return new JSONObject(
		).put(
			"body", "body"
		).put(
			"from", _EMAIL_ADDRESS_GLOBAL
		).put(
			"fromName", _FROM_NAME
		).put(
			"subject", "subject"
		).put(
			"to", _CLOUD_PROVISIONING_EMAIL_ADDRESS
		);
	}

	private JSONObject _createSaaSAdminJSONObject(
		String emailAddress, String firstName, String lastName) {

		return new JSONObject(
		).put(
			"emailAddress", emailAddress
		).put(
			"firstName", firstName
		).put(
			"lastName", lastName
		);
	}

	private JSONObject _createSaaSFieldsJSONObject() {
		return new JSONObject(
		).put(
			"admins",
			new JSONArray(
			).put(
				_createSaaSAdminJSONObject("admin@liferay.com", "Jane", "Doe")
			)
		).put(
			"analyticsCloudOwnerEmailAddress", "owner@liferay.com"
		).put(
			"projectId", "saas-omni"
		).put(
			"region", "Europe"
		);
	}

	private String _getPaaSProjectAdmin(
		String emailAddress, String firstName, String githubUserName,
		String lastName) {

		return StringBundler.concat(
			"<strong>Email Address - </strong> ", emailAddress,
			"<br>\n<strong>First Name - </strong>", firstName,
			"<br>\n<strong>Last Name - </strong>", lastName,
			"<br>\n<strong>GitHub ID - </strong>", githubUserName, "<br><br>");
	}

	private String _getSaaSProjectAdmin(
		String emailAddress, String firstName, String lastName) {

		return StringBundler.concat(
			"<strong>First Name -</strong> ", firstName,
			"<br>\n<strong>Last Name - </strong>", lastName,
			"<br>\n<strong>Email Address - </strong>", emailAddress,
			"\n<br><br>");
	}

	private static final long _ACCOUNT_ENTRY_ID = 1000L;

	private static final String _ACCOUNT_ERC = "ACCNT-001";

	private static final String _CLOUD_PROVISIONING_EMAIL_ADDRESS =
		"cloud-provisioning@liferay.com";

	private static final long _CONTRACT_ID = 4000L;

	private static final String _EMAIL_ADDRESS_GLOBAL =
		"do-not-reply@liferay.com";

	private static final long _ENVIRONMENT_ID = 2000L;

	private static final String _FROM_NAME = "One Liferay";

	private static final String _PROJECT_ERC = "PRJCT-001";

	private static final String _SALESFORCE_ACCOUNT_ID = "001B000001Ga2XXIAU";

	private static final String _SALESFORCE_PROJECT_ID = "a0OB000000Kj3ZZIAT";

	private CloudActivationRequestService _cloudActivationRequestService;
	private EntitlementService _entitlementService;
	private EnvironmentAdminService _environmentAdminService;
	private EnvironmentService _environmentService;
	private NotificationQueueEntryService _notificationQueueEntryService;
	private NotificationTemplateService _notificationTemplateService;

}