/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.admin.user.client.dto.v1_0.Account;
import com.liferay.one.jira.service.JiraIssueService;
import com.liferay.one.pubsub.Message;
import com.liferay.one.salesforce.model.SalesforceAccount;
import com.liferay.one.salesforce.model.SalesforceModelTestUtil;
import com.liferay.one.salesforce.model.SalesforceOpportunity;
import com.liferay.one.salesforce.model.SalesforceOpportunityLineItem;
import com.liferay.portal.kernel.util.ListUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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
public class ProvisioningIssueServiceTest {

	@BeforeEach
	public void setUp() {
		_provisioningIssueService = new ProvisioningIssueService();

		_jiraIssueService = Mockito.mock(JiraIssueService.class);

		ReflectionTestUtils.setField(
			_provisioningIssueService, "_jiraIssueProvisioningFieldCountry",
			"customfield_country");
		ReflectionTestUtils.setField(
			_provisioningIssueService, "_jiraIssueProvisioningFieldOffering",
			"customfield_offering");
		ReflectionTestUtils.setField(
			_provisioningIssueService,
			"_jiraIssueProvisioningFieldOrganization",
			"customfield_organization");
		ReflectionTestUtils.setField(
			_provisioningIssueService, "_jiraIssueProvisioningFieldOwner",
			"customfield_owner");
		ReflectionTestUtils.setField(
			_provisioningIssueService,
			"_jiraIssueProvisioningFieldProvisioningComponent",
			"customfield_provisioning_component");
		ReflectionTestUtils.setField(
			_provisioningIssueService,
			"_jiraIssueProvisioningFieldSupportRegion",
			"customfield_support_region");
		ReflectionTestUtils.setField(
			_provisioningIssueService, "_jiraIssueProvisioningId", "10001");
		ReflectionTestUtils.setField(
			_provisioningIssueService, "_jiraIssueProvisioningOfferingId",
			"20001");
		ReflectionTestUtils.setField(
			_provisioningIssueService, "_jiraIssueService", _jiraIssueService);
		ReflectionTestUtils.setField(
			_provisioningIssueService, "_jiraIssueSupportHCFieldRequestType",
			"customfield_request_type");
		ReflectionTestUtils.setField(
			_provisioningIssueService, "_jiraOrganizationProvisioningId",
			"30001");
		ReflectionTestUtils.setField(
			_provisioningIssueService, "_jiraProjectSupportHC", "SUPPORTHC");
		ReflectionTestUtils.setField(
			_provisioningIssueService, "_jiraRequestProvisioningId", "40001");
		ReflectionTestUtils.setField(
			_provisioningIssueService, "_jiraWorkspaceId", "WORKSPACE-1");
		ReflectionTestUtils.setField(
			_provisioningIssueService, "_portalURL", "https://one.liferay.com");
	}

	@Test
	public void testAddErrorIssueSwallowsFailure() throws Exception {
		Mockito.when(
			_jiraIssueService.addIssue(
				Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any())
		).thenThrow(
			new RuntimeException("Unable to add issue")
		);

		Message message = new Message(
			Collections.emptyMap(), "{}", "test-topic");

		Assertions.assertDoesNotThrow(
			() -> _provisioningIssueService.addErrorIssue(
				message, new RuntimeException("boom")));
	}

	@Test
	public void testAddErrorIssueUsesGenericSummary() throws Exception {
		Message message = new Message(
			Collections.emptyMap(), "{}", "test-topic");

		_provisioningIssueService.addErrorIssue(
			message, new RuntimeException("boom"));

		ArgumentCaptor<Map<String, Object>> customFieldsArgumentCaptor =
			ArgumentCaptor.forClass(Map.class);
		ArgumentCaptor<String> summaryArgumentCaptor = ArgumentCaptor.forClass(
			String.class);

		Mockito.verify(
			_jiraIssueService
		).addIssue(
			customFieldsArgumentCaptor.capture(), Mockito.any(),
			Mockito.eq(_JIRA_ISSUE_PROVISIONING_ID),
			Mockito.eq(_JIRA_PROJECT_SUPPORT_HC),
			summaryArgumentCaptor.capture()
		);

		Assertions.assertEquals(
			"Auto-Provisioning Error", summaryArgumentCaptor.getValue());

		JSONArray labelsJSONArray =
			(JSONArray)customFieldsArgumentCaptor.getValue(
			).get(
				"labels"
			);

		Assertions.assertTrue(
			labelsJSONArray.toList(
			).contains(
				"auto-generated"
			));
		Assertions.assertTrue(
			labelsJSONArray.toList(
			).contains(
				"provisioning-error"
			));
	}

	@Test
	public void testAddErrorIssueWithRecordIncludesOpportunityId()
		throws Exception {

		Message message = new Message(
			Collections.emptyMap(), "{}", "test-topic");

		JSONObject recordJSONObject = new JSONObject(
		).put(
			"opportunity",
			new JSONObject(
			).put(
				"Id", _OPPORTUNITY_ID
			)
		);

		_provisioningIssueService.addErrorIssue(
			message, recordJSONObject, new RuntimeException("boom"));

		ArgumentCaptor<String> summaryArgumentCaptor = ArgumentCaptor.forClass(
			String.class);

		Mockito.verify(
			_jiraIssueService
		).addIssue(
			Mockito.any(), Mockito.any(),
			Mockito.eq(_JIRA_ISSUE_PROVISIONING_ID),
			Mockito.eq(_JIRA_PROJECT_SUPPORT_HC),
			summaryArgumentCaptor.capture()
		);

		Assertions.assertEquals(
			"Auto-Provisioning Error for opportunity " + _OPPORTUNITY_ID,
			summaryArgumentCaptor.getValue());
	}

	@Test
	public void testAddOpportunityInvoicedIssueIncludesCountryAndOwnerWhenPresent()
		throws Exception {

		_invokeAddOpportunityInvoicedIssue(
			"US", "owner@example.com", Collections.emptyList());

		ArgumentCaptor<Map<String, Object>> customFieldsArgumentCaptor =
			ArgumentCaptor.forClass(Map.class);

		Mockito.verify(
			_jiraIssueService
		).addIssue(
			customFieldsArgumentCaptor.capture(), Mockito.any(),
			Mockito.eq(_JIRA_ISSUE_PROVISIONING_ID),
			Mockito.eq(_JIRA_PROJECT_SUPPORT_HC), Mockito.any()
		);

		Map<String, Object> customFields =
			customFieldsArgumentCaptor.getValue();

		Assertions.assertEquals("US", customFields.get("customfield_country"));
		Assertions.assertEquals(
			"owner@example.com", customFields.get("customfield_owner"));
	}

	@Test
	public void testAddOpportunityInvoicedIssueOmitsCountryAndOwnerWhenBlank()
		throws Exception {

		_invokeAddOpportunityInvoicedIssue("", "", Collections.emptyList());

		ArgumentCaptor<Map<String, Object>> customFieldsArgumentCaptor =
			ArgumentCaptor.forClass(Map.class);

		Mockito.verify(
			_jiraIssueService
		).addIssue(
			customFieldsArgumentCaptor.capture(), Mockito.any(),
			Mockito.eq(_JIRA_ISSUE_PROVISIONING_ID),
			Mockito.eq(_JIRA_PROJECT_SUPPORT_HC), Mockito.any()
		);

		Map<String, Object> customFields =
			customFieldsArgumentCaptor.getValue();

		Assertions.assertFalse(customFields.containsKey("customfield_country"));
		Assertions.assertFalse(customFields.containsKey("customfield_owner"));
	}

	@Test
	public void testAddOpportunityInvoicedIssuePinsSupportRegionAndOrganizationFields()
		throws Exception {

		_invokeAddOpportunityInvoicedIssue(
			"US", "", "Subscription", "Liferay Brazil",
			Collections.emptyList());

		ArgumentCaptor<Map<String, Object>> customFieldsArgumentCaptor =
			ArgumentCaptor.forClass(Map.class);

		Mockito.verify(
			_jiraIssueService
		).addIssue(
			customFieldsArgumentCaptor.capture(), Mockito.any(),
			Mockito.eq(_JIRA_ISSUE_PROVISIONING_ID),
			Mockito.eq(_JIRA_PROJECT_SUPPORT_HC), Mockito.any()
		);

		Map<String, Object> customFields =
			customFieldsArgumentCaptor.getValue();

		JSONObject supportRegionJSONObject = (JSONObject)customFields.get(
			"customfield_support_region");

		Assertions.assertEquals(
			"Brazil", supportRegionJSONObject.getString("value"));

		JSONArray offeringJSONArray = (JSONArray)customFields.get(
			"customfield_offering");

		Assertions.assertEquals(
			"WORKSPACE-1:20001",
			offeringJSONArray.getJSONObject(
				0
			).getString(
				"id"
			));

		JSONArray organizationJSONArray = (JSONArray)customFields.get(
			"customfield_organization");

		Assertions.assertEquals(
			"WORKSPACE-1:30001",
			organizationJSONArray.getJSONObject(
				0
			).getString(
				"id"
			));

		Assertions.assertEquals(
			"40001", customFields.get("customfield_request_type"));
	}

	@Test
	public void testAddOpportunityInvoicedIssuePrefixesWarningIndicator()
		throws Exception {

		_invokeAddOpportunityInvoicedIssue(
			"US", "", List.of("Something went wrong"));

		ArgumentCaptor<String> summaryArgumentCaptor = ArgumentCaptor.forClass(
			String.class);

		Mockito.verify(
			_jiraIssueService
		).addIssue(
			Mockito.any(), Mockito.any(),
			Mockito.eq(_JIRA_ISSUE_PROVISIONING_ID),
			Mockito.eq(_JIRA_PROJECT_SUPPORT_HC),
			summaryArgumentCaptor.capture()
		);

		Assertions.assertTrue(
			summaryArgumentCaptor.getValue(
			).startsWith(
				"[Warning] "
			));
	}

	@Test
	public void testAddOpportunityInvoicedIssueSummaryContainsAccountNameAndProductType()
		throws Exception {

		_invokeAddOpportunityInvoicedIssue(
			"US", "", "License", Collections.emptyList());

		ArgumentCaptor<String> summaryArgumentCaptor = ArgumentCaptor.forClass(
			String.class);

		Mockito.verify(
			_jiraIssueService
		).addIssue(
			Mockito.any(), Mockito.any(),
			Mockito.eq(_JIRA_ISSUE_PROVISIONING_ID),
			Mockito.eq(_JIRA_PROJECT_SUPPORT_HC),
			summaryArgumentCaptor.capture()
		);

		String summary = summaryArgumentCaptor.getValue();

		Assertions.assertFalse(summary.startsWith("[Warning] "));
		Assertions.assertTrue(summary.contains(_ACCOUNT_NAME));
		Assertions.assertTrue(summary.contains("License"));
	}

	@Test
	public void testAddOpportunityInvoicedIssueSwallowsFailure()
		throws Exception {

		Mockito.when(
			_jiraIssueService.addIssue(
				Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any())
		).thenThrow(
			new RuntimeException("Unable to add issue")
		);

		Assertions.assertDoesNotThrow(
			() -> _invokeAddOpportunityInvoicedIssue(
				"US", "", Collections.emptyList()));
	}

	private void _invokeAddOpportunityInvoicedIssue(
		String billingCountry, String ownerEmail,
		List<String> warningMessages) {

		_invokeAddOpportunityInvoicedIssue(
			billingCountry, ownerEmail, "Subscription", warningMessages);
	}

	private void _invokeAddOpportunityInvoicedIssue(
		String billingCountry, String ownerEmail, String productType,
		List<String> warningMessages) {

		_invokeAddOpportunityInvoicedIssue(
			billingCountry, ownerEmail, productType, "", warningMessages);
	}

	private void _invokeAddOpportunityInvoicedIssue(
		String billingCountry, String ownerEmail, String productType,
		String soldBy, List<String> warningMessages) {

		Account account = new Account();

		account.setExternalReferenceCode(_ACCOUNT_ERC);
		account.setName(_ACCOUNT_NAME);

		SalesforceAccount salesforceAccount = new SalesforceAccount(
			SalesforceModelTestUtil.createAccountJSONObject(
				billingCountry, _ACCOUNT_ERC, _ACCOUNT_NAME));

		SalesforceOpportunity salesforceOpportunity = new SalesforceOpportunity(
			SalesforceModelTestUtil.createOpportunityJSONObject(
				_ACCOUNT_ERC, "", false, _OPPORTUNITY_ID, ownerEmail, "E", "",
				soldBy, "Closed Won", "New Business"));

		List<SalesforceOpportunityLineItem> salesforceOpportunityLineItems =
			ListUtil.fromArray(
				new SalesforceOpportunityLineItem(
					SalesforceModelTestUtil.createOpportunityLineItemJSONObject(
						"USD", null, "LINE-1", "PROD-1", "Widget", productType,
						5, null)));

		_provisioningIssueService.addOpportunityInvoicedIssue(
			account, salesforceAccount, salesforceOpportunity,
			salesforceOpportunityLineItems, warningMessages);
	}

	private static final String _ACCOUNT_ERC = "ACCOUNT-1";

	private static final String _ACCOUNT_NAME = "Test Account";

	private static final String _JIRA_ISSUE_PROVISIONING_ID = "10001";

	private static final String _JIRA_PROJECT_SUPPORT_HC = "SUPPORTHC";

	private static final String _OPPORTUNITY_ID = "OPP-1";

	private JiraIssueService _jiraIssueService;
	private ProvisioningIssueService _provisioningIssueService;

}