/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.salesforce.pubsub;

import com.liferay.headless.admin.user.client.dto.v1_0.Account;
import com.liferay.headless.commerce.admin.catalog.client.dto.v1_0.Sku;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.Order;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.OrderItem;
import com.liferay.one.constants.CommerceOrderConstants;
import com.liferay.one.constants.OpportunityConstants;
import com.liferay.one.model.Contract;
import com.liferay.one.model.Project;
import com.liferay.one.pubsub.Message;
import com.liferay.one.salesforce.model.SalesforceModelTestUtil;
import com.liferay.one.salesforce.model.SalesforceOpportunity;
import com.liferay.one.salesforce.model.SalesforceOpportunityLineItem;
import com.liferay.one.salesforce.model.SalesforceProject;
import com.liferay.one.salesforce.model.SalesforceProjectContactRole;
import com.liferay.one.service.AccountService;
import com.liferay.one.service.CommerceOrderItemService;
import com.liferay.one.service.CommerceOrderService;
import com.liferay.one.service.CommerceSkuService;
import com.liferay.one.service.ContractService;
import com.liferay.one.service.EntitlementService;
import com.liferay.one.service.ProjectService;
import com.liferay.one.service.ProvisioningContactService;
import com.liferay.one.service.ProvisioningEmailService;
import com.liferay.one.service.ProvisioningIssueService;
import com.liferay.one.service.ProvisioningOrderService;
import com.liferay.one.service.ProvisioningSubdomainService;
import com.liferay.one.service.UserAccountService;
import com.liferay.petra.string.StringBundler;

import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Felipe Franca
 */
public class SalesforceOpportunityPubsubSubscriberTest {

	@BeforeEach
	public void setUp() throws Exception {
		_subscriber = new SalesforceOpportunityPubsubSubscriber();

		_accountService = Mockito.mock(AccountService.class);
		_commerceOrderItemService = Mockito.mock(
			CommerceOrderItemService.class);
		_commerceOrderService = Mockito.mock(CommerceOrderService.class);
		_commerceSkuService = Mockito.mock(CommerceSkuService.class);
		_contractService = Mockito.mock(ContractService.class);
		_entitlementService = Mockito.mock(EntitlementService.class);
		_projectService = Mockito.mock(ProjectService.class);
		_provisioningContactService = Mockito.mock(
			ProvisioningContactService.class);
		_provisioningEmailService = Mockito.mock(
			ProvisioningEmailService.class);
		_provisioningIssueService = Mockito.mock(
			ProvisioningIssueService.class);
		_provisioningOrderService = Mockito.mock(
			ProvisioningOrderService.class);
		_provisioningSubdomainService = Mockito.mock(
			ProvisioningSubdomainService.class);
		_userAccountService = Mockito.mock(UserAccountService.class);

		_account = new Account();

		_account.setExternalReferenceCode(_ACCOUNT_ID_SF);
		_account.setId(_ACCOUNT_ID);
		_account.setName("Test Account");

		Mockito.when(
			_accountService.fetchAccountByExternalReferenceCode(
				Mockito.anyString())
		).thenReturn(
			_account
		);

		Sku sku = new Sku();

		sku.setId(_SKU_ID);

		Mockito.when(
			_commerceSkuService.fetchSku(Mockito.anyString())
		).thenReturn(
			sku
		);

		Order newOrder = new Order();

		newOrder.setId(_NEW_ORDER_ID);

		Mockito.when(
			_commerceOrderService.upsertOrder(
				Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.anyList(), Mockito.any())
		).thenReturn(
			newOrder
		);

		OrderItem upsertedOrderItem = new OrderItem();

		upsertedOrderItem.setId(_ORDER_ITEM_ID);

		Mockito.when(
			_commerceOrderItemService.upsertOrderItem(
				Mockito.any(), Mockito.any(), Mockito.any())
		).thenReturn(
			upsertedOrderItem
		);

		ReflectionTestUtils.setField(
			_subscriber, "_accountService", _accountService);
		ReflectionTestUtils.setField(
			_subscriber, "_commerceOrderItemService",
			_commerceOrderItemService);
		ReflectionTestUtils.setField(
			_subscriber, "_commerceOrderService", _commerceOrderService);
		ReflectionTestUtils.setField(
			_subscriber, "_commerceSkuService", _commerceSkuService);
		ReflectionTestUtils.setField(
			_subscriber, "_contractService", _contractService);
		ReflectionTestUtils.setField(
			_subscriber, "_entitlementService", _entitlementService);
		ReflectionTestUtils.setField(_subscriber, "_projectId", "test-project");
		ReflectionTestUtils.setField(
			_subscriber, "_projectService", _projectService);
		ReflectionTestUtils.setField(
			_subscriber, "_provisioningContactService",
			_provisioningContactService);
		ReflectionTestUtils.setField(
			_subscriber, "_provisioningEmailService",
			_provisioningEmailService);
		ReflectionTestUtils.setField(
			_subscriber, "_provisioningIssueService",
			_provisioningIssueService);
		ReflectionTestUtils.setField(
			_subscriber, "_provisioningOrderService",
			_provisioningOrderService);
		ReflectionTestUtils.setField(
			_subscriber, "_provisioningSubdomainService",
			_provisioningSubdomainService);
		ReflectionTestUtils.setField(
			_subscriber, "_subscription", "test-subscription");
		ReflectionTestUtils.setField(_subscriber, "_topic", "test-topic");
		ReflectionTestUtils.setField(
			_subscriber, "_userAccountService", _userAccountService);
	}

	@Test
	public void testIsAutoCreateTopicReturnsFalse() {
		Assertions.assertFalse(_subscriber.isAutoCreateTopic());
	}

	@Test
	public void testReceiveAddsErrorIssueForEveryFailedRecord()
		throws Exception {

		Mockito.doThrow(
			new RuntimeException("Unable to upsert account")
		).when(
			_accountService
		).upsertAccount(
			Mockito.any(), Mockito.any()
		);

		Message message = new Message(
			Collections.emptyMap(),
			SalesforceModelTestUtil.createOpportunityMessagePayload(
				_createNewBusinessRecordJSONObject("OPP-FAIL-1"),
				_createNewBusinessRecordJSONObject("OPP-FAIL-2")
			).toString(),
			"test-topic");

		Assertions.assertDoesNotThrow(() -> _subscriber.receive(message));

		Mockito.verify(
			_provisioningIssueService, Mockito.times(2)
		).addErrorIssue(
			Mockito.eq(message), Mockito.any(JSONObject.class),
			Mockito.any(Exception.class)
		);

		Mockito.verify(
			_provisioningIssueService, Mockito.never()
		).addErrorIssue(
			Mockito.any(Message.class), Mockito.any(Exception.class)
		);

		Mockito.verify(
			_commerceOrderService, Mockito.never()
		).upsertOrder(
			Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
			Mockito.anyList(), Mockito.any()
		);
	}

	@Test
	public void testReceiveAddsErrorIssueForMalformedPayload()
		throws Exception {

		Message message = new Message(
			Collections.emptyMap(), "not json", "test-topic");

		Assertions.assertDoesNotThrow(() -> _subscriber.receive(message));

		Mockito.verify(
			_provisioningIssueService
		).addErrorIssue(
			Mockito.eq(message), Mockito.any(Exception.class)
		);
	}

	@Test
	public void testReceiveAddsErrorIssueWhenRecordHasNoOpportunityKey()
		throws Exception {

		JSONObject recordJSONObject = new JSONObject(
		).put(
			"someOtherKey", "x"
		);

		Message message = new Message(
			Collections.emptyMap(),
			SalesforceModelTestUtil.createOpportunityMessagePayload(
				recordJSONObject
			).toString(),
			"test-topic");

		Assertions.assertDoesNotThrow(() -> _subscriber.receive(message));

		ArgumentCaptor<JSONObject> recordJSONObjectArgumentCaptor =
			ArgumentCaptor.forClass(JSONObject.class);

		Mockito.verify(
			_provisioningIssueService
		).addErrorIssue(
			Mockito.eq(message), recordJSONObjectArgumentCaptor.capture(),
			Mockito.any(Exception.class)
		);

		Assertions.assertTrue(
			recordJSONObjectArgumentCaptor.getValue(
			).has(
				"someOtherKey"
			));
	}

	@Test
	public void testReceiveAddsErrorIssueWhenRecordProcessingThrows()
		throws Exception {

		Mockito.doThrow(
			new RuntimeException("Unable to upsert account")
		).when(
			_accountService
		).upsertAccount(
			Mockito.any(), Mockito.any()
		);

		JSONObject recordJSONObject = _createNewBusinessRecordJSONObject();

		Message message = new Message(
			Collections.emptyMap(),
			SalesforceModelTestUtil.createOpportunityMessagePayload(
				recordJSONObject
			).toString(),
			"test-topic");

		Assertions.assertDoesNotThrow(() -> _subscriber.receive(message));

		Mockito.verify(
			_provisioningIssueService
		).addErrorIssue(
			Mockito.eq(message), Mockito.any(JSONObject.class),
			Mockito.any(Exception.class)
		);
	}

	@Test
	public void testReceiveAttachesContractToProjectWhenProjectExternalReferenceCodeBlank()
		throws Exception {

		Mockito.when(
			_contractService.fetchLatestContractByOpportunityId(
				Mockito.anyString())
		).thenReturn(
			new Contract(
				new JSONObject(
				).put(
					"id", 888L
				))
		);

		JSONObject opportunityJSONObject =
			SalesforceModelTestUtil.createOpportunityJSONObject(
				_ACCOUNT_ID_SF, "", false, _OPPORTUNITY_ID, "", "E", "", "",
				"Closed Won", OpportunityConstants.TYPE_NEW_BUSINESS);

		JSONObject lineItemJSONObject =
			SalesforceModelTestUtil.createOpportunityLineItemJSONObject(
				"USD", null, "LINE-1", "PROD-1", "Widget", "Subscription", 5,
				null);

		JSONObject projectJSONObject = new JSONObject(
		).put(
			"Id", "SF-PROJ-2"
		).put(
			"Name", "My Project"
		);

		JSONObject recordJSONObject =
			SalesforceModelTestUtil.createOpportunityRecordJSONObject(
				SalesforceModelTestUtil.createAccountJSONObject(
					_ACCOUNT_ID_SF, "Test Salesforce Account"),
				opportunityJSONObject,
				new JSONArray(
				).put(
					lineItemJSONObject
				),
				new JSONArray(), projectJSONObject);

		_receiveOpportunityMessage(recordJSONObject);

		Mockito.verify(
			_contractService
		).attachContractToProject(
			888L, "SF-PROJ-2"
		);
	}

	@Test
	public void testReceiveCompletesOrderBeforeProvisioningSubdomain()
		throws Exception {

		_receiveOpportunityMessage(_createNewBusinessRecordJSONObject());

		InOrder inOrder = Mockito.inOrder(
			_commerceOrderItemService, _commerceOrderService,
			_provisioningEmailService, _provisioningSubdomainService);

		inOrder.verify(
			_commerceOrderService
		).upsertOrder(
			Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
			Mockito.anyList(), Mockito.any()
		);

		inOrder.verify(
			_commerceOrderItemService
		).upsertOrderItem(
			Mockito.any(), Mockito.any(), Mockito.any()
		);

		inOrder.verify(
			_commerceOrderService
		).completeOrder(
			Mockito.anyLong(), Mockito.anyInt()
		);

		inOrder.verify(
			_provisioningSubdomainService
		).provisionSubdomain(
			Mockito.any(), Mockito.anyList()
		);

		inOrder.verify(
			_provisioningEmailService
		).sendWelcomeEmails(
			Mockito.any(), Mockito.any(), Mockito.anyList()
		);
	}

	@Test
	public void testReceiveCompletesOrderOnFirstDelivery() throws Exception {
		_receiveOpportunityMessage(_createNewBusinessRecordJSONObject());

		Mockito.verify(
			_commerceOrderService
		).completeOrder(
			_NEW_ORDER_ID,
			CommerceOrderConstants.ORDER_PAYMENT_STATUS_NOT_REQUIRED
		);

		Mockito.verify(
			_provisioningEmailService
		).sendWelcomeEmails(
			Mockito.eq(_account),
			Mockito.eq(OpportunityConstants.TYPE_NEW_BUSINESS),
			Mockito.anyList()
		);

		Mockito.verify(
			_provisioningEmailService, Mockito.never()
		).sendAssignedWelcomeEmails(
			Mockito.any(), Mockito.any()
		);

		Mockito.verify(
			_provisioningIssueService
		).addOpportunityInvoicedIssue(
			Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
			Mockito.any()
		);
	}

	@Test
	public void testReceiveCompletesOrderOnReprocessWhenNotYetCompleted()
		throws Exception {

		OrderItem existingOrderItem = SalesforceModelTestUtil.createOrderItem(
			null, null, null, "LINE-1", _EXISTING_ORDER_ITEM_ID, "PROD-1",
			null);

		Order existingOrder = new Order();

		existingOrder.setExternalReferenceCode(_OPPORTUNITY_ID);
		existingOrder.setOrderItems(new OrderItem[] {existingOrderItem});
		existingOrder.setOrderStatus(
			CommerceOrderConstants.ORDER_STATUS_PROCESSING);

		Mockito.when(
			_commerceOrderService.fetchOrderByExternalReferenceCode(
				_OPPORTUNITY_ID)
		).thenReturn(
			existingOrder
		);

		_receiveOpportunityMessage(_createNewBusinessRecordJSONObject());

		Mockito.verify(
			_commerceOrderService
		).completeOrder(
			_NEW_ORDER_ID,
			CommerceOrderConstants.ORDER_PAYMENT_STATUS_NOT_REQUIRED
		);

		Mockito.verify(
			_provisioningEmailService
		).sendAssignedWelcomeEmails(
			Mockito.eq(_account), Mockito.anyList()
		);
	}

	@Test
	public void testReceiveContinuesWhenUpdateEntitlementsThrows()
		throws Exception {

		OrderItem existingOrderItem = SalesforceModelTestUtil.createOrderItem(
			null, null, null, "LINE-1", _EXISTING_ORDER_ITEM_ID, "PROD-1",
			null);

		Order existingOrder = new Order();

		existingOrder.setExternalReferenceCode(_OPPORTUNITY_ID);
		existingOrder.setOrderItems(new OrderItem[] {existingOrderItem});
		existingOrder.setOrderStatus(
			CommerceOrderConstants.ORDER_STATUS_COMPLETED);

		Mockito.when(
			_commerceOrderService.fetchOrderByExternalReferenceCode(
				_OPPORTUNITY_ID)
		).thenReturn(
			existingOrder
		);

		Mockito.doThrow(
			new RuntimeException("Unable to update entitlements")
		).when(
			_entitlementService
		).updateEntitlements(
			_EXISTING_ORDER_ITEM_ID
		);

		Assertions.assertDoesNotThrow(
			() -> _receiveOpportunityMessage(
				_createNewBusinessRecordJSONObject()));

		Mockito.verify(
			_provisioningEmailService
		).sendAssignedWelcomeEmails(
			Mockito.eq(_account), Mockito.anyList()
		);
	}

	@Test
	public void testReceiveDefaultsToUsdWhenLineCurrenciesAreBlank()
		throws Exception {

		JSONObject opportunityJSONObject =
			SalesforceModelTestUtil.createOpportunityJSONObject(
				_ACCOUNT_ID_SF, "", false, _OPPORTUNITY_ID, "", "E", "", "",
				"Closed Won", OpportunityConstants.TYPE_NEW_BUSINESS);

		JSONObject lineItemJSONObject =
			SalesforceModelTestUtil.createOpportunityLineItemJSONObject(
				"", null, "LINE-1", "PROD-1", "Widget", "Subscription", 5,
				null);

		JSONObject recordJSONObject =
			SalesforceModelTestUtil.createOpportunityRecordJSONObject(
				SalesforceModelTestUtil.createAccountJSONObject(
					_ACCOUNT_ID_SF, "Test Salesforce Account"),
				opportunityJSONObject,
				new JSONArray(
				).put(
					lineItemJSONObject
				),
				new JSONArray(), null);

		_receiveOpportunityMessage(recordJSONObject);

		ArgumentCaptor<String> currencyCodeArgumentCaptor =
			ArgumentCaptor.forClass(String.class);

		Mockito.verify(
			_commerceOrderService
		).upsertOrder(
			Mockito.any(), Mockito.any(), currencyCodeArgumentCaptor.capture(),
			Mockito.any(), Mockito.anyList(), Mockito.any()
		);

		Assertions.assertEquals("USD", currencyCodeArgumentCaptor.getValue());

		ArgumentCaptor<List<String>> warningMessagesArgumentCaptor =
			ArgumentCaptor.forClass(List.class);

		Mockito.verify(
			_provisioningIssueService
		).addOpportunityInvoicedIssue(
			Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
			warningMessagesArgumentCaptor.capture()
		);

		Assertions.assertFalse(
			_containsSubstring(
				"reconcile mixed currencies",
				warningMessagesArgumentCaptor.getValue()));
	}

	@Test
	public void testReceiveDoesNotAttachContractToProjectWhenProjectAlreadyAttached()
		throws Exception {

		Mockito.when(
			_contractService.fetchLatestContractByOpportunityId(
				Mockito.anyString())
		).thenReturn(
			new Contract(
				new JSONObject(
				).put(
					"id", 888L
				).put(
					"r_projectToContract_c_projectERC", "SF-PROJ-EXISTING"
				))
		);

		JSONObject opportunityJSONObject =
			SalesforceModelTestUtil.createOpportunityJSONObject(
				_ACCOUNT_ID_SF, "", false, _OPPORTUNITY_ID, "", "E", "", "",
				"Closed Won", OpportunityConstants.TYPE_NEW_BUSINESS);

		JSONObject lineItemJSONObject =
			SalesforceModelTestUtil.createOpportunityLineItemJSONObject(
				"USD", null, "LINE-1", "PROD-1", "Widget", "Subscription", 5,
				null);

		JSONObject projectJSONObject = new JSONObject(
		).put(
			"Id", "SF-PROJ-2"
		).put(
			"Name", "My Project"
		);

		JSONObject recordJSONObject =
			SalesforceModelTestUtil.createOpportunityRecordJSONObject(
				SalesforceModelTestUtil.createAccountJSONObject(
					_ACCOUNT_ID_SF, "Test Salesforce Account"),
				opportunityJSONObject,
				new JSONArray(
				).put(
					lineItemJSONObject
				),
				new JSONArray(), projectJSONObject);

		_receiveOpportunityMessage(recordJSONObject);

		Mockito.verify(
			_contractService, Mockito.never()
		).attachContractToProject(
			Mockito.anyLong(), Mockito.anyString()
		);

		ArgumentCaptor<Long> contractIdArgumentCaptor = ArgumentCaptor.forClass(
			Long.class);

		Mockito.verify(
			_commerceOrderService
		).upsertOrder(
			Mockito.any(), contractIdArgumentCaptor.capture(), Mockito.any(),
			Mockito.any(), Mockito.anyList(), Mockito.any()
		);

		Assertions.assertEquals(888L, contractIdArgumentCaptor.getValue());
	}

	@Test
	public void testReceiveDoesNothingWhenRecordsArrayIsEmpty()
		throws Exception {

		Message message = new Message(
			Collections.emptyMap(),
			SalesforceModelTestUtil.createOpportunityMessagePayload(
			).toString(),
			"test-topic");

		Assertions.assertDoesNotThrow(() -> _subscriber.receive(message));

		_verifyNoProvisioningInteractions();
	}

	@Test
	public void testReceiveForwardsProjectContactRolesToAddProjectContacts()
		throws Exception {

		JSONObject opportunityJSONObject =
			SalesforceModelTestUtil.createOpportunityJSONObject(
				_ACCOUNT_ID_SF, "", false, _OPPORTUNITY_ID, "", "E", "", "",
				"Closed Won", OpportunityConstants.TYPE_NEW_BUSINESS);

		JSONObject lineItemJSONObject =
			SalesforceModelTestUtil.createOpportunityLineItemJSONObject(
				"USD", null, "LINE-1", "PROD-1", "Widget", "Subscription", 5,
				null);

		JSONObject contactRoleJSONObject =
			SalesforceModelTestUtil.createProjectContactRoleJSONObject(
				"Member", "contact@example.com", "Jane", "Doe", _PROJECT_ID);

		JSONObject recordJSONObject =
			SalesforceModelTestUtil.createOpportunityRecordJSONObject(
				SalesforceModelTestUtil.createAccountJSONObject(
					_ACCOUNT_ID_SF, "Test Salesforce Account"),
				opportunityJSONObject,
				new JSONArray(
				).put(
					lineItemJSONObject
				),
				new JSONArray(
				).put(
					contactRoleJSONObject
				),
				null);

		_receiveOpportunityMessage(recordJSONObject);

		ArgumentCaptor<List<SalesforceProjectContactRole>>
			contactRolesArgumentCaptor = ArgumentCaptor.forClass(List.class);

		Mockito.verify(
			_provisioningContactService
		).addProjectContacts(
			Mockito.any(), contactRolesArgumentCaptor.capture(), Mockito.any(),
			Mockito.any()
		);

		List<SalesforceProjectContactRole> contactRoles =
			contactRolesArgumentCaptor.getValue();

		Assertions.assertEquals(1, contactRoles.size());
		Assertions.assertEquals(
			"contact@example.com",
			contactRoles.get(
				0
			).getEmailAddress());
	}

	@Test
	public void testReceiveProcessesEveryRecordWhenAllRecordsSucceed()
		throws Exception {

		Message message = new Message(
			Collections.emptyMap(),
			SalesforceModelTestUtil.createOpportunityMessagePayload(
				_createNewBusinessRecordJSONObject("OPP-1"),
				_createNewBusinessRecordJSONObject("OPP-2")
			).toString(),
			"test-topic");

		_subscriber.receive(message);

		Mockito.verify(
			_commerceOrderService, Mockito.times(2)
		).upsertOrder(
			Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
			Mockito.anyList(), Mockito.any()
		);

		Mockito.verify(
			_provisioningEmailService, Mockito.times(2)
		).sendWelcomeEmails(
			Mockito.any(), Mockito.any(), Mockito.anyList()
		);

		Mockito.verify(
			_provisioningIssueService, Mockito.times(2)
		).addOpportunityInvoicedIssue(
			Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
			Mockito.any()
		);

		Mockito.verify(
			_provisioningIssueService, Mockito.never()
		).addErrorIssue(
			Mockito.any(Message.class), Mockito.any(JSONObject.class),
			Mockito.any(Exception.class)
		);
	}

	@Test
	public void testReceiveProcessesRemainingRecordAfterOneRecordFails()
		throws Exception {

		Mockito.doThrow(
			new RuntimeException("Unable to upsert account")
		).doNothing(
		).when(
			_accountService
		).upsertAccount(
			Mockito.any(), Mockito.any()
		);

		JSONObject firstRecordJSONObject = _createNewBusinessRecordJSONObject(
			"OPP-FAIL");
		JSONObject secondRecordJSONObject = _createNewBusinessRecordJSONObject(
			_OPPORTUNITY_ID);

		Message message = new Message(
			Collections.emptyMap(),
			SalesforceModelTestUtil.createOpportunityMessagePayload(
				firstRecordJSONObject, secondRecordJSONObject
			).toString(),
			"test-topic");

		_subscriber.receive(message);

		Mockito.verify(
			_provisioningIssueService
		).addErrorIssue(
			Mockito.eq(message), Mockito.any(JSONObject.class),
			Mockito.any(Exception.class)
		);

		Mockito.verify(
			_commerceOrderService
		).upsertOrder(
			Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
			Mockito.anyList(), Mockito.any()
		);
	}

	@Test
	public void testReceiveProvisionsExistingBusinessOpportunity()
		throws Exception {

		Mockito.when(
			_projectService.fetchProject(_PROJECT_ID)
		).thenReturn(
			new Project(new JSONObject())
		);

		JSONObject recordJSONObject = _createOpportunityRecordJSONObject(
			_OPPORTUNITY_ID, "E", _PROJECT_ID, "Closed Won",
			OpportunityConstants.TYPE_EXISTING_BUSINESS);

		_receiveOpportunityMessage(recordJSONObject);

		Mockito.verify(
			_provisioningEmailService
		).sendWelcomeEmails(
			Mockito.eq(_account),
			Mockito.eq(OpportunityConstants.TYPE_EXISTING_BUSINESS),
			Mockito.anyList()
		);

		ArgumentCaptor<List<String>> warningMessagesArgumentCaptor =
			ArgumentCaptor.forClass(List.class);

		Mockito.verify(
			_provisioningIssueService
		).addOpportunityInvoicedIssue(
			Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
			warningMessagesArgumentCaptor.capture()
		);

		for (String warningMessage : warningMessagesArgumentCaptor.getValue()) {
			Assertions.assertFalse(
				warningMessage.contains("project does not exist"));
		}
	}

	@Test
	public void testReceiveProvisionsNewBusinessOpportunity() throws Exception {
		_receiveOpportunityMessage(_createNewBusinessRecordJSONObject());

		ArgumentCaptor<String> currencyCodeArgumentCaptor =
			ArgumentCaptor.forClass(String.class);

		Mockito.verify(
			_commerceOrderService
		).upsertOrder(
			Mockito.any(), Mockito.any(), currencyCodeArgumentCaptor.capture(),
			Mockito.any(), Mockito.anyList(), Mockito.any()
		);

		Assertions.assertEquals("USD", currencyCodeArgumentCaptor.getValue());

		Mockito.verify(
			_provisioningSubdomainService
		).provisionSubdomain(
			Mockito.eq(_account), Mockito.anyList()
		);

		Mockito.verify(
			_commerceOrderItemService
		).upsertOrderItem(
			Mockito.any(), Mockito.any(), Mockito.any()
		);

		Mockito.verify(
			_commerceOrderService
		).completeOrder(
			_NEW_ORDER_ID,
			CommerceOrderConstants.ORDER_PAYMENT_STATUS_NOT_REQUIRED
		);

		Mockito.verify(
			_provisioningEmailService
		).sendWelcomeEmails(
			Mockito.eq(_account),
			Mockito.eq(OpportunityConstants.TYPE_NEW_BUSINESS),
			Mockito.anyList()
		);

		Mockito.verify(
			_provisioningIssueService
		).addOpportunityInvoicedIssue(
			Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
			Mockito.any()
		);

		Mockito.verify(
			_provisioningOrderService, Mockito.never()
		).trimRealignedOrderItems(
			Mockito.anyLong(), Mockito.anyString(), Mockito.anyString(),
			Mockito.anyList(), Mockito.anyList()
		);

		Mockito.verify(
			_provisioningOrderService, Mockito.never()
		).trimRenewedOrderItems(
			Mockito.anyLong(), Mockito.anyString(), Mockito.anyList(),
			Mockito.anyList()
		);
	}

	@Test
	public void testReceiveProvisionsOpportunityWithSupportProductFamily()
		throws Exception {

		JSONObject recordJSONObject = _createOpportunityRecordJSONObject(
			_OPPORTUNITY_ID, "S", "", "Closed Won",
			OpportunityConstants.TYPE_NEW_BUSINESS);

		_receiveOpportunityMessage(recordJSONObject);

		Mockito.verify(
			_commerceOrderService
		).completeOrder(
			_NEW_ORDER_ID,
			CommerceOrderConstants.ORDER_PAYMENT_STATUS_NOT_REQUIRED
		);

		Mockito.verify(
			_provisioningIssueService
		).addOpportunityInvoicedIssue(
			Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
			Mockito.any()
		);
	}

	@Test
	public void testReceiveProvisionsRenewalOpportunity() throws Exception {
		JSONObject recordJSONObject = _createOpportunityRecordJSONObject(
			_OPPORTUNITY_ID, "E", "", "Closed Lost",
			OpportunityConstants.TYPE_RENEWAL);

		_receiveOpportunityMessage(recordJSONObject);

		Mockito.verify(
			_provisioningOrderService
		).trimRenewedOrderItems(
			Mockito.eq(_ACCOUNT_ID), Mockito.eq(_OPPORTUNITY_ID),
			Mockito.anyList(), Mockito.anyList()
		);

		Mockito.verify(
			_provisioningContactService, Mockito.never()
		).addProjectContacts(
			Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()
		);

		Mockito.verify(
			_provisioningEmailService
		).sendWelcomeEmails(
			Mockito.eq(_account), Mockito.eq(OpportunityConstants.TYPE_RENEWAL),
			Mockito.anyList()
		);
	}

	@Test
	public void testReceiveReprocessesRedeliveredMessageWithoutDuplicating()
		throws Exception {

		OrderItem existingOrderItem = SalesforceModelTestUtil.createOrderItem(
			null, null, null, "LINE-1", _EXISTING_ORDER_ITEM_ID, "PROD-1",
			null);

		Order existingOrder = new Order();

		existingOrder.setExternalReferenceCode(_OPPORTUNITY_ID);
		existingOrder.setOrderItems(new OrderItem[] {existingOrderItem});
		existingOrder.setOrderStatus(
			CommerceOrderConstants.ORDER_STATUS_COMPLETED);

		Mockito.when(
			_commerceOrderService.fetchOrderByExternalReferenceCode(
				_OPPORTUNITY_ID)
		).thenReturn(
			existingOrder
		);

		_receiveOpportunityMessage(_createNewBusinessRecordJSONObject());

		Mockito.verify(
			_commerceOrderService, Mockito.never()
		).completeOrder(
			Mockito.anyLong(), Mockito.anyInt()
		);

		Mockito.verify(
			_provisioningIssueService, Mockito.never()
		).addOpportunityInvoicedIssue(
			Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
			Mockito.any()
		);

		Mockito.verify(
			_provisioningEmailService
		).sendAssignedWelcomeEmails(
			Mockito.eq(_account), Mockito.anyList()
		);

		Mockito.verify(
			_provisioningEmailService, Mockito.never()
		).sendWelcomeEmails(
			Mockito.any(), Mockito.any(), Mockito.any()
		);

		Mockito.verify(
			_entitlementService
		).updateEntitlements(
			_EXISTING_ORDER_ITEM_ID
		);

		ArgumentCaptor<SalesforceOpportunity>
			salesforceOpportunityArgumentCaptor = ArgumentCaptor.forClass(
				SalesforceOpportunity.class);

		Mockito.verify(
			_commerceOrderService
		).upsertOrder(
			Mockito.any(), Mockito.any(), Mockito.any(),
			salesforceOpportunityArgumentCaptor.capture(), Mockito.anyList(),
			Mockito.any()
		);

		Assertions.assertEquals(
			_OPPORTUNITY_ID,
			salesforceOpportunityArgumentCaptor.getValue(
			).getId());
	}

	@Test
	public void testReceiveResolvesSingleLineCurrency() throws Exception {
		JSONObject opportunityJSONObject =
			SalesforceModelTestUtil.createOpportunityJSONObject(
				_ACCOUNT_ID_SF, "", false, _OPPORTUNITY_ID, "", "E", "", "",
				"Closed Won", OpportunityConstants.TYPE_NEW_BUSINESS);

		JSONObject lineItemJSONObject =
			SalesforceModelTestUtil.createOpportunityLineItemJSONObject(
				"EUR", null, "LINE-1", "PROD-1", "Widget", "Subscription", 5,
				null);

		JSONObject recordJSONObject =
			SalesforceModelTestUtil.createOpportunityRecordJSONObject(
				SalesforceModelTestUtil.createAccountJSONObject(
					_ACCOUNT_ID_SF, "Test Salesforce Account"),
				opportunityJSONObject,
				new JSONArray(
				).put(
					lineItemJSONObject
				),
				new JSONArray(), null);

		_receiveOpportunityMessage(recordJSONObject);

		ArgumentCaptor<String> currencyCodeArgumentCaptor =
			ArgumentCaptor.forClass(String.class);

		Mockito.verify(
			_commerceOrderService
		).upsertOrder(
			Mockito.any(), Mockito.any(), currencyCodeArgumentCaptor.capture(),
			Mockito.any(), Mockito.anyList(), Mockito.any()
		);

		Assertions.assertEquals("EUR", currencyCodeArgumentCaptor.getValue());
	}

	@Test
	public void testReceiveSkipsLineItemWithoutMatchingSku() throws Exception {
		Mockito.when(
			_commerceSkuService.fetchSku(Mockito.anyString())
		).thenReturn(
			null
		);

		_receiveOpportunityMessage(_createNewBusinessRecordJSONObject());

		Mockito.verify(
			_commerceOrderItemService, Mockito.never()
		).upsertOrderItem(
			Mockito.any(), Mockito.any(), Mockito.any()
		);

		Mockito.verify(
			_commerceOrderService, Mockito.never()
		).completeOrder(
			Mockito.anyLong(), Mockito.anyInt()
		);

		ArgumentCaptor<List<String>> warningMessagesArgumentCaptor =
			ArgumentCaptor.forClass(List.class);

		Mockito.verify(
			_provisioningIssueService
		).addOpportunityInvoicedIssue(
			Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
			warningMessagesArgumentCaptor.capture()
		);

		Assertions.assertTrue(
			_containsSubstring(
				"Unable to find SKU",
				warningMessagesArgumentCaptor.getValue()));
	}

	@Test
	public void testReceiveSkipsOpportunityInvoicedIssueForProductFamilyP()
		throws Exception {

		JSONObject recordJSONObject = _createOpportunityRecordJSONObject(
			_OPPORTUNITY_ID, "P", "", "Closed Won",
			OpportunityConstants.TYPE_NEW_BUSINESS);

		_receiveOpportunityMessage(recordJSONObject);

		Mockito.verify(
			_commerceOrderService
		).completeOrder(
			_NEW_ORDER_ID,
			CommerceOrderConstants.ORDER_PAYMENT_STATUS_NOT_REQUIRED
		);

		Mockito.verify(
			_provisioningEmailService
		).sendWelcomeEmails(
			Mockito.eq(_account),
			Mockito.eq(OpportunityConstants.TYPE_NEW_BUSINESS),
			Mockito.anyList()
		);

		Mockito.verify(
			_provisioningIssueService, Mockito.never()
		).addOpportunityInvoicedIssue(
			Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
			Mockito.any()
		);
	}

	@Test
	public void testReceiveSkipsOpportunityThatIsNotClosedWon()
		throws Exception {

		JSONObject recordJSONObject = _createOpportunityRecordJSONObject(
			_OPPORTUNITY_ID, "E", "", "Negotiation",
			OpportunityConstants.TYPE_NEW_BUSINESS);

		_receiveOpportunityMessage(recordJSONObject);

		_verifyNoProvisioningInteractions();
	}

	@Test
	public void testReceiveSkipsOpportunityWithoutLineItems() throws Exception {
		JSONObject opportunityJSONObject =
			SalesforceModelTestUtil.createOpportunityJSONObject(
				_ACCOUNT_ID_SF, "", false, _OPPORTUNITY_ID, "", "E", "", "",
				"Closed Won", OpportunityConstants.TYPE_NEW_BUSINESS);

		JSONObject recordJSONObject =
			SalesforceModelTestUtil.createOpportunityRecordJSONObject(
				SalesforceModelTestUtil.createAccountJSONObject(
					_ACCOUNT_ID_SF, "Test Salesforce Account"),
				opportunityJSONObject, new JSONArray(), new JSONArray(), null);

		_receiveOpportunityMessage(recordJSONObject);

		_verifyNoProvisioningInteractions();
	}

	@Test
	public void testReceiveSkipsOpportunityWithoutProductFamily()
		throws Exception {

		JSONObject recordJSONObject = _createOpportunityRecordJSONObject(
			_OPPORTUNITY_ID, "", "", "Closed Won",
			OpportunityConstants.TYPE_NEW_BUSINESS);

		_receiveOpportunityMessage(recordJSONObject);

		_verifyNoProvisioningInteractions();
	}

	@Test
	public void testReceiveSkipsOpportunityWithUnmatchedProductFamily()
		throws Exception {

		JSONObject recordJSONObject = _createOpportunityRecordJSONObject(
			_OPPORTUNITY_ID, "X", "", "Closed Won",
			OpportunityConstants.TYPE_NEW_BUSINESS);

		_receiveOpportunityMessage(recordJSONObject);

		_verifyNoProvisioningInteractions();
	}

	@Test
	public void testReceiveSkipsRenewalOpportunityThatIsClosedWon()
		throws Exception {

		JSONObject recordJSONObject = _createOpportunityRecordJSONObject(
			_OPPORTUNITY_ID, "E", "", "Closed Won",
			OpportunityConstants.TYPE_RENEWAL);

		_receiveOpportunityMessage(recordJSONObject);

		_verifyNoProvisioningInteractions();
	}

	@Test
	public void testReceiveSkipsWhenAccountNotFoundAfterUpsert()
		throws Exception {

		Mockito.when(
			_accountService.fetchAccountByExternalReferenceCode(
				Mockito.anyString())
		).thenReturn(
			null
		);

		_receiveOpportunityMessage(_createNewBusinessRecordJSONObject());

		Mockito.verify(
			_commerceOrderService, Mockito.never()
		).upsertOrder(
			Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
			Mockito.anyList(), Mockito.any()
		);
	}

	@Test
	public void testReceiveSkipsWhenSalesforceAccountIdIsBlank()
		throws Exception {

		JSONObject opportunityJSONObject =
			SalesforceModelTestUtil.createOpportunityJSONObject(
				_ACCOUNT_ID_SF, "", false, _OPPORTUNITY_ID, "", "E", "", "",
				"Closed Won", OpportunityConstants.TYPE_NEW_BUSINESS);

		JSONObject lineItemJSONObject =
			SalesforceModelTestUtil.createOpportunityLineItemJSONObject(
				"USD", null, "LINE-1", "PROD-1", "Widget", "Subscription", 5,
				null);

		JSONObject recordJSONObject =
			SalesforceModelTestUtil.createOpportunityRecordJSONObject(
				SalesforceModelTestUtil.createAccountJSONObject(
					"", "Test Salesforce Account"),
				opportunityJSONObject,
				new JSONArray(
				).put(
					lineItemJSONObject
				),
				new JSONArray(), null);

		_receiveOpportunityMessage(recordJSONObject);

		Mockito.verify(
			_accountService, Mockito.never()
		).upsertAccount(
			Mockito.any(), Mockito.any()
		);
	}

	@Test
	public void testReceiveTreatsOrderWithoutOrderItemsAsFirstDelivery()
		throws Exception {

		Order existingOrder = new Order();

		existingOrder.setExternalReferenceCode(_OPPORTUNITY_ID);
		existingOrder.setOrderStatus(
			CommerceOrderConstants.ORDER_STATUS_COMPLETED);

		Mockito.when(
			_commerceOrderService.fetchOrderByExternalReferenceCode(
				_OPPORTUNITY_ID)
		).thenReturn(
			existingOrder
		);

		_receiveOpportunityMessage(_createNewBusinessRecordJSONObject());

		Mockito.verify(
			_provisioningEmailService
		).sendWelcomeEmails(
			Mockito.eq(_account),
			Mockito.eq(OpportunityConstants.TYPE_NEW_BUSINESS),
			Mockito.anyList()
		);

		Mockito.verify(
			_provisioningEmailService, Mockito.never()
		).sendAssignedWelcomeEmails(
			Mockito.any(), Mockito.any()
		);

		Mockito.verify(
			_provisioningIssueService
		).addOpportunityInvoicedIssue(
			Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
			Mockito.any()
		);

		Mockito.verify(
			_commerceOrderService, Mockito.never()
		).completeOrder(
			Mockito.anyLong(), Mockito.anyInt()
		);
	}

	@Test
	public void testReceiveTrimsRealignedOrderItemsForAmendment()
		throws Exception {

		JSONObject opportunityJSONObject =
			SalesforceModelTestUtil.createOpportunityJSONObject(
				_ACCOUNT_ID_SF, _PARENT_OPPORTUNITY_ID, false, _OPPORTUNITY_ID,
				"", "E", "", "", "Closed Won", "Amendment");

		JSONObject realignmentLineItemJSONObject =
			SalesforceModelTestUtil.createOpportunityLineItemJSONObject(
				"USD", null, "LINE-R", "PROD-R", "Widget", "Subscription", 0,
				null);
		JSONObject provisionableLineItemJSONObject =
			SalesforceModelTestUtil.createOpportunityLineItemJSONObject(
				"USD", null, "LINE-P", "PROD-P", "Gadget", "Subscription", 5,
				null);

		JSONObject recordJSONObject =
			SalesforceModelTestUtil.createOpportunityRecordJSONObject(
				SalesforceModelTestUtil.createAccountJSONObject(
					_ACCOUNT_ID_SF, "Test Salesforce Account"),
				opportunityJSONObject,
				new JSONArray(
				).put(
					realignmentLineItemJSONObject
				).put(
					provisionableLineItemJSONObject
				),
				new JSONArray(), null);

		_receiveOpportunityMessage(recordJSONObject);

		ArgumentCaptor<List<SalesforceOpportunityLineItem>>
			realignedLinesArgumentCaptor = ArgumentCaptor.forClass(List.class);

		Mockito.verify(
			_provisioningOrderService
		).trimRealignedOrderItems(
			Mockito.eq(_ACCOUNT_ID), Mockito.eq(_OPPORTUNITY_ID),
			Mockito.eq(_PARENT_OPPORTUNITY_ID),
			realignedLinesArgumentCaptor.capture(), Mockito.anyList()
		);

		List<SalesforceOpportunityLineItem> realignedLines =
			realignedLinesArgumentCaptor.getValue();

		Assertions.assertEquals(1, realignedLines.size());

		ArgumentCaptor<List<SalesforceOpportunityLineItem>>
			provisionableLinesArgumentCaptor = ArgumentCaptor.forClass(
				List.class);

		Mockito.verify(
			_commerceOrderService
		).upsertOrder(
			Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
			provisionableLinesArgumentCaptor.capture(), Mockito.any()
		);

		Assertions.assertEquals(
			1,
			provisionableLinesArgumentCaptor.getValue(
			).size());
	}

	@Test
	public void testReceiveTrimsRenewedOrderItemsWhenHasRenewalFlagSet()
		throws Exception {

		JSONObject opportunityJSONObject =
			SalesforceModelTestUtil.createOpportunityJSONObject(
				_ACCOUNT_ID_SF, "", true, _OPPORTUNITY_ID, "", "E", "", "",
				"Closed Won", OpportunityConstants.TYPE_NEW_BUSINESS);

		JSONObject lineItemJSONObject =
			SalesforceModelTestUtil.createOpportunityLineItemJSONObject(
				"USD", null, "LINE-1", "PROD-1", "Widget", "Subscription", 5,
				null);

		JSONObject recordJSONObject =
			SalesforceModelTestUtil.createOpportunityRecordJSONObject(
				SalesforceModelTestUtil.createAccountJSONObject(
					_ACCOUNT_ID_SF, "Test Salesforce Account"),
				opportunityJSONObject,
				new JSONArray(
				).put(
					lineItemJSONObject
				),
				new JSONArray(), null);

		_receiveOpportunityMessage(recordJSONObject);

		Mockito.verify(
			_provisioningOrderService
		).trimRenewedOrderItems(
			Mockito.eq(_ACCOUNT_ID), Mockito.eq(_OPPORTUNITY_ID),
			Mockito.anyList(), Mockito.anyList()
		);
	}

	@Test
	public void testReceiveUpsertsAccountWithSoldByFromOpportunity()
		throws Exception {

		JSONObject opportunityJSONObject =
			SalesforceModelTestUtil.createOpportunityJSONObject(
				_ACCOUNT_ID_SF, "", false, _OPPORTUNITY_ID, "", "E", "",
				"SALES-REP-1", "Closed Won",
				OpportunityConstants.TYPE_NEW_BUSINESS);

		JSONObject lineItemJSONObject =
			SalesforceModelTestUtil.createOpportunityLineItemJSONObject(
				"USD", null, "LINE-1", "PROD-1", "Widget", "Subscription", 5,
				null);

		JSONObject recordJSONObject =
			SalesforceModelTestUtil.createOpportunityRecordJSONObject(
				SalesforceModelTestUtil.createAccountJSONObject(
					_ACCOUNT_ID_SF, "Test Salesforce Account"),
				opportunityJSONObject,
				new JSONArray(
				).put(
					lineItemJSONObject
				),
				new JSONArray(), null);

		_receiveOpportunityMessage(recordJSONObject);

		ArgumentCaptor<String> soldByArgumentCaptor = ArgumentCaptor.forClass(
			String.class);

		Mockito.verify(
			_accountService
		).upsertAccount(
			Mockito.any(), soldByArgumentCaptor.capture()
		);

		Assertions.assertEquals("SALES-REP-1", soldByArgumentCaptor.getValue());
	}

	@Test
	public void testReceiveUpsertsProjectWhenProjectNodePresent()
		throws Exception {

		JSONObject opportunityJSONObject =
			SalesforceModelTestUtil.createOpportunityJSONObject(
				_ACCOUNT_ID_SF, "", false, _OPPORTUNITY_ID, "", "E", "", "",
				"Closed Won", OpportunityConstants.TYPE_NEW_BUSINESS);

		JSONObject lineItemJSONObject =
			SalesforceModelTestUtil.createOpportunityLineItemJSONObject(
				"USD", null, "LINE-1", "PROD-1", "Widget", "Subscription", 5,
				null);

		JSONObject projectJSONObject = new JSONObject(
		).put(
			"Id", "SF-PROJ-1"
		).put(
			"Name", "My Project"
		);

		JSONObject recordJSONObject =
			SalesforceModelTestUtil.createOpportunityRecordJSONObject(
				SalesforceModelTestUtil.createAccountJSONObject(
					_ACCOUNT_ID_SF, "Test Salesforce Account"),
				opportunityJSONObject,
				new JSONArray(
				).put(
					lineItemJSONObject
				),
				new JSONArray(), projectJSONObject);

		_receiveOpportunityMessage(recordJSONObject);

		Mockito.verify(
			_projectService
		).upsertProject(
			Mockito.eq(_ACCOUNT_ID_SF), Mockito.any()
		);

		ArgumentCaptor<SalesforceProject> salesforceProjectArgumentCaptor =
			ArgumentCaptor.forClass(SalesforceProject.class);

		Mockito.verify(
			_commerceOrderService
		).upsertOrder(
			Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
			Mockito.anyList(), salesforceProjectArgumentCaptor.capture()
		);

		Assertions.assertNotNull(salesforceProjectArgumentCaptor.getValue());
	}

	@Test
	public void testReceiveUsesContractIdFromFoundContract() throws Exception {
		Mockito.when(
			_contractService.fetchLatestContractByOpportunityId(
				Mockito.anyString())
		).thenReturn(
			new Contract(
				new JSONObject(
				).put(
					"id", 777L
				))
		);

		_receiveOpportunityMessage(_createNewBusinessRecordJSONObject());

		ArgumentCaptor<Long> contractIdArgumentCaptor = ArgumentCaptor.forClass(
			Long.class);

		Mockito.verify(
			_commerceOrderService
		).upsertOrder(
			Mockito.any(), contractIdArgumentCaptor.capture(), Mockito.any(),
			Mockito.any(), Mockito.anyList(), Mockito.any()
		);

		Assertions.assertEquals(777L, contractIdArgumentCaptor.getValue());
	}

	@Test
	public void testReceiveWarnsAndContinuesWhenCompleteOrderThrows()
		throws Exception {

		Mockito.doThrow(
			new RuntimeException("Unable to complete order")
		).when(
			_commerceOrderService
		).completeOrder(
			Mockito.anyLong(), Mockito.anyInt()
		);

		Assertions.assertDoesNotThrow(
			() -> _receiveOpportunityMessage(
				_createNewBusinessRecordJSONObject()));

		ArgumentCaptor<List<String>> warningMessagesArgumentCaptor =
			ArgumentCaptor.forClass(List.class);

		Mockito.verify(
			_provisioningIssueService
		).addOpportunityInvoicedIssue(
			Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
			warningMessagesArgumentCaptor.capture()
		);

		Assertions.assertTrue(
			_containsSubstring(
				"Unable to complete order",
				warningMessagesArgumentCaptor.getValue()));
	}

	@Test
	public void testReceiveWarnsAndContinuesWhenUpsertOrderItemThrows()
		throws Exception {

		Mockito.doThrow(
			new RuntimeException("Unable to upsert order item")
		).when(
			_commerceOrderItemService
		).upsertOrderItem(
			Mockito.any(), Mockito.any(), Mockito.any()
		);

		Assertions.assertDoesNotThrow(
			() -> _receiveOpportunityMessage(
				_createNewBusinessRecordJSONObject()));

		Mockito.verify(
			_commerceOrderService, Mockito.never()
		).completeOrder(
			Mockito.anyLong(), Mockito.anyInt()
		);

		ArgumentCaptor<List<String>> warningMessagesArgumentCaptor =
			ArgumentCaptor.forClass(List.class);

		Mockito.verify(
			_provisioningIssueService
		).addOpportunityInvoicedIssue(
			Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
			warningMessagesArgumentCaptor.capture()
		);

		Assertions.assertTrue(
			_containsSubstring(
				"Unable to provision line",
				warningMessagesArgumentCaptor.getValue()));
	}

	@Test
	public void testReceiveWarnsOnMixedLineCurrenciesAndDefaultsToUsd()
		throws Exception {

		JSONObject opportunityJSONObject =
			SalesforceModelTestUtil.createOpportunityJSONObject(
				_ACCOUNT_ID_SF, "", false, _OPPORTUNITY_ID, "", "E", "", "",
				"Closed Won", OpportunityConstants.TYPE_NEW_BUSINESS);

		JSONObject firstLineItemJSONObject =
			SalesforceModelTestUtil.createOpportunityLineItemJSONObject(
				"USD", null, "LINE-1", "PROD-1", "Widget", "Subscription", 5,
				null);
		JSONObject secondLineItemJSONObject =
			SalesforceModelTestUtil.createOpportunityLineItemJSONObject(
				"EUR", null, "LINE-2", "PROD-2", "Gadget", "Subscription", 3,
				null);

		JSONObject recordJSONObject =
			SalesforceModelTestUtil.createOpportunityRecordJSONObject(
				SalesforceModelTestUtil.createAccountJSONObject(
					_ACCOUNT_ID_SF, "Test Salesforce Account"),
				opportunityJSONObject,
				new JSONArray(
				).put(
					firstLineItemJSONObject
				).put(
					secondLineItemJSONObject
				),
				new JSONArray(), null);

		_receiveOpportunityMessage(recordJSONObject);

		ArgumentCaptor<String> currencyCodeArgumentCaptor =
			ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<List<String>> warningMessagesArgumentCaptor =
			ArgumentCaptor.forClass(List.class);

		Mockito.verify(
			_commerceOrderService
		).upsertOrder(
			Mockito.any(), Mockito.any(), currencyCodeArgumentCaptor.capture(),
			Mockito.any(), Mockito.anyList(), Mockito.any()
		);

		Assertions.assertEquals("USD", currencyCodeArgumentCaptor.getValue());

		Mockito.verify(
			_provisioningIssueService
		).addOpportunityInvoicedIssue(
			Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
			warningMessagesArgumentCaptor.capture()
		);

		Assertions.assertTrue(
			_containsSubstring(
				"reconcile mixed currencies",
				warningMessagesArgumentCaptor.getValue()));
	}

	@Test
	public void testReceiveWarnsWhenAmendmentHasNoParentOpportunity()
		throws Exception {

		JSONObject opportunityJSONObject =
			SalesforceModelTestUtil.createOpportunityJSONObject(
				_ACCOUNT_ID_SF, "", false, _OPPORTUNITY_ID, "", "E", "", "",
				"Closed Won", "Amendment");

		JSONObject lineItemJSONObject =
			SalesforceModelTestUtil.createOpportunityLineItemJSONObject(
				"USD", null, "LINE-R", "PROD-R", "Widget", "Subscription", 0,
				null);

		JSONObject recordJSONObject =
			SalesforceModelTestUtil.createOpportunityRecordJSONObject(
				SalesforceModelTestUtil.createAccountJSONObject(
					_ACCOUNT_ID_SF, "Test Salesforce Account"),
				opportunityJSONObject,
				new JSONArray(
				).put(
					lineItemJSONObject
				),
				new JSONArray(), null);

		_receiveOpportunityMessage(recordJSONObject);

		Mockito.verify(
			_provisioningOrderService, Mockito.never()
		).trimRealignedOrderItems(
			Mockito.anyLong(), Mockito.anyString(), Mockito.anyString(),
			Mockito.anyList(), Mockito.anyList()
		);

		ArgumentCaptor<List<String>> warningMessagesArgumentCaptor =
			ArgumentCaptor.forClass(List.class);

		Mockito.verify(
			_provisioningIssueService
		).addOpportunityInvoicedIssue(
			Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
			warningMessagesArgumentCaptor.capture()
		);

		boolean hasProductWarning = false;

		for (String warningMessage : warningMessagesArgumentCaptor.getValue()) {
			if (warningMessage.contains("Widget")) {
				hasProductWarning = true;
			}
		}

		Assertions.assertTrue(hasProductWarning);
	}

	@Test
	public void testReceiveWarnsWhenContractNotFound() throws Exception {
		_receiveOpportunityMessage(_createNewBusinessRecordJSONObject());

		ArgumentCaptor<Long> contractIdArgumentCaptor = ArgumentCaptor.forClass(
			Long.class);
		ArgumentCaptor<List<String>> warningMessagesArgumentCaptor =
			ArgumentCaptor.forClass(List.class);

		Mockito.verify(
			_commerceOrderService
		).upsertOrder(
			Mockito.any(), contractIdArgumentCaptor.capture(), Mockito.any(),
			Mockito.any(), Mockito.anyList(), Mockito.any()
		);

		Assertions.assertNull(contractIdArgumentCaptor.getValue());

		Mockito.verify(
			_provisioningIssueService
		).addOpportunityInvoicedIssue(
			Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
			warningMessagesArgumentCaptor.capture()
		);

		Assertions.assertTrue(
			_containsSubstring(
				"Unable to find a contract for opportunity " + _OPPORTUNITY_ID,
				warningMessagesArgumentCaptor.getValue()));

		Mockito.verify(
			_contractService, Mockito.never()
		).attachContractToProject(
			Mockito.anyLong(), Mockito.anyString()
		);
	}

	@Test
	public void testReceiveWarnsWhenDuplicateAccountName() throws Exception {
		Mockito.when(
			_accountService.hasDuplicateAccountName(
				Mockito.any(), Mockito.any())
		).thenReturn(
			true
		);

		_receiveOpportunityMessage(_createNewBusinessRecordJSONObject());

		ArgumentCaptor<List<String>> warningMessagesArgumentCaptor =
			ArgumentCaptor.forClass(List.class);

		Mockito.verify(
			_provisioningIssueService
		).addOpportunityInvoicedIssue(
			Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
			warningMessagesArgumentCaptor.capture()
		);

		Assertions.assertTrue(
			_containsSubstring(
				"Another account already uses the name",
				warningMessagesArgumentCaptor.getValue()));

		Mockito.verify(
			_commerceOrderService
		).upsertOrder(
			Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
			Mockito.anyList(), Mockito.any()
		);
	}

	@Test
	public void testReceiveWarnsWhenLineEndDateDiffersFromContractEndDate()
		throws Exception {

		Mockito.when(
			_contractService.fetchLatestContractByOpportunityId(
				Mockito.anyString())
		).thenReturn(
			new Contract(
				new JSONObject(
				).put(
					"endDate", "2026-01-01T00:00:00Z"
				).put(
					"id", 999L
				))
		);

		JSONObject opportunityJSONObject =
			SalesforceModelTestUtil.createOpportunityJSONObject(
				_ACCOUNT_ID_SF, "", false, _OPPORTUNITY_ID, "", "E", "", "",
				"Closed Won", OpportunityConstants.TYPE_NEW_BUSINESS);

		JSONObject lineItemJSONObject =
			SalesforceModelTestUtil.createOpportunityLineItemJSONObject(
				"USD", "2025-06-01", "LINE-1", "PROD-1", "Widget",
				"Subscription", 5, null);

		JSONObject recordJSONObject =
			SalesforceModelTestUtil.createOpportunityRecordJSONObject(
				SalesforceModelTestUtil.createAccountJSONObject(
					_ACCOUNT_ID_SF, "Test Salesforce Account"),
				opportunityJSONObject,
				new JSONArray(
				).put(
					lineItemJSONObject
				),
				new JSONArray(), null);

		_receiveOpportunityMessage(recordJSONObject);

		ArgumentCaptor<List<String>> warningMessagesArgumentCaptor =
			ArgumentCaptor.forClass(List.class);

		Mockito.verify(
			_provisioningIssueService
		).addOpportunityInvoicedIssue(
			Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
			warningMessagesArgumentCaptor.capture()
		);

		Assertions.assertTrue(
			_containsSubstring(
				"differs from the end date of contract",
				warningMessagesArgumentCaptor.getValue()));
	}

	@Test
	public void testReceiveWarnsWhenOpportunityOwnerNotFound()
		throws Exception {

		JSONObject opportunityJSONObject =
			SalesforceModelTestUtil.createOpportunityJSONObject(
				_ACCOUNT_ID_SF, "", false, _OPPORTUNITY_ID, "owner@example.com",
				"E", "", "", "Closed Won",
				OpportunityConstants.TYPE_NEW_BUSINESS);

		JSONObject lineItemJSONObject =
			SalesforceModelTestUtil.createOpportunityLineItemJSONObject(
				"USD", null, "LINE-1", "PROD-1", "Widget", "Subscription", 5,
				null);

		JSONObject recordJSONObject =
			SalesforceModelTestUtil.createOpportunityRecordJSONObject(
				SalesforceModelTestUtil.createAccountJSONObject(
					_ACCOUNT_ID_SF, "Test Salesforce Account"),
				opportunityJSONObject,
				new JSONArray(
				).put(
					lineItemJSONObject
				),
				new JSONArray(), null);

		_receiveOpportunityMessage(recordJSONObject);

		ArgumentCaptor<List<String>> warningMessagesArgumentCaptor =
			ArgumentCaptor.forClass(List.class);

		Mockito.verify(
			_provisioningIssueService
		).addOpportunityInvoicedIssue(
			Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
			warningMessagesArgumentCaptor.capture()
		);

		Assertions.assertTrue(
			_containsSubstring(
				"Unable to find portal user",
				warningMessagesArgumentCaptor.getValue()));
	}

	@Test
	public void testReceiveWarnsWhenProjectAlreadyExistsForNewBusiness()
		throws Exception {

		Mockito.when(
			_projectService.fetchProject(_PROJECT_ID)
		).thenReturn(
			new Project(new JSONObject())
		);

		_receiveOpportunityMessage(
			_createOpportunityRecordJSONObject(
				_OPPORTUNITY_ID, "E", _PROJECT_ID, "Closed Won",
				OpportunityConstants.TYPE_NEW_BUSINESS));

		ArgumentCaptor<List<String>> warningMessagesArgumentCaptor =
			ArgumentCaptor.forClass(List.class);

		Mockito.verify(
			_provisioningIssueService
		).addOpportunityInvoicedIssue(
			Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
			warningMessagesArgumentCaptor.capture()
		);

		Assertions.assertTrue(
			_containsSubstring(
				"project already exists",
				warningMessagesArgumentCaptor.getValue()));
	}

	@Test
	public void testReceiveWarnsWhenProjectAlreadyExistsForNewProjectExistingBusiness()
		throws Exception {

		Mockito.when(
			_projectService.fetchProject(Mockito.anyString())
		).thenReturn(
			new Project(new JSONObject())
		);

		JSONObject recordJSONObject = _createOpportunityRecordJSONObject(
			_OPPORTUNITY_ID, "E", _PROJECT_ID, "Closed Won",
			OpportunityConstants.TYPE_NEW_PROJECT_EXISTING_BUSINESS);

		_receiveOpportunityMessage(recordJSONObject);

		ArgumentCaptor<List<String>> warningMessagesArgumentCaptor =
			ArgumentCaptor.forClass(List.class);

		Mockito.verify(
			_provisioningIssueService
		).addOpportunityInvoicedIssue(
			Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
			warningMessagesArgumentCaptor.capture()
		);

		Assertions.assertTrue(
			_containsSubstring(
				StringBundler.concat(
					"The opportunity type is ",
					OpportunityConstants.TYPE_NEW_PROJECT_EXISTING_BUSINESS,
					" and the project already exists"),
				warningMessagesArgumentCaptor.getValue()));
	}

	@Test
	public void testReceiveWarnsWhenProjectDoesNotExistForExistingBusiness()
		throws Exception {

		JSONObject recordJSONObject = _createOpportunityRecordJSONObject(
			_OPPORTUNITY_ID, "E", _PROJECT_ID, "Closed Won",
			OpportunityConstants.TYPE_EXISTING_BUSINESS);

		_receiveOpportunityMessage(recordJSONObject);

		ArgumentCaptor<List<String>> warningMessagesArgumentCaptor =
			ArgumentCaptor.forClass(List.class);

		Mockito.verify(
			_provisioningIssueService
		).addOpportunityInvoicedIssue(
			Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
			warningMessagesArgumentCaptor.capture()
		);

		Assertions.assertTrue(
			_containsSubstring(
				"project does not exist",
				warningMessagesArgumentCaptor.getValue()));
	}

	private boolean _containsSubstring(
		String substring, List<String> warningMessages) {

		for (String warningMessage : warningMessages) {
			if (warningMessage.contains(substring)) {
				return true;
			}
		}

		return false;
	}

	private JSONObject _createNewBusinessRecordJSONObject() {
		return _createNewBusinessRecordJSONObject(_OPPORTUNITY_ID);
	}

	private JSONObject _createNewBusinessRecordJSONObject(
		String opportunityId) {

		return _createOpportunityRecordJSONObject(
			opportunityId, "E", "", "Closed Won",
			OpportunityConstants.TYPE_NEW_BUSINESS);
	}

	private JSONObject _createOpportunityRecordJSONObject(
		String opportunityId, String productFamily, String projectId,
		String stageName, String type) {

		JSONObject opportunityJSONObject =
			SalesforceModelTestUtil.createOpportunityJSONObject(
				_ACCOUNT_ID_SF, "", false, opportunityId, "", productFamily,
				projectId, "", stageName, type);

		JSONObject lineItemJSONObject =
			SalesforceModelTestUtil.createOpportunityLineItemJSONObject(
				"USD", null, "LINE-1", "PROD-1", "Widget", "Subscription", 5,
				null);

		return SalesforceModelTestUtil.createOpportunityRecordJSONObject(
			SalesforceModelTestUtil.createAccountJSONObject(
				_ACCOUNT_ID_SF, "Test Salesforce Account"),
			opportunityJSONObject,
			new JSONArray(
			).put(
				lineItemJSONObject
			),
			new JSONArray(), null);
	}

	private void _receiveOpportunityMessage(JSONObject recordJSONObject)
		throws Exception {

		Message message = new Message(
			Collections.emptyMap(),
			SalesforceModelTestUtil.createOpportunityMessagePayload(
				recordJSONObject
			).toString(),
			"test-topic");

		_subscriber.receive(message);
	}

	private void _verifyNoProvisioningInteractions() {
		Mockito.verifyNoInteractions(
			_accountService, _commerceOrderItemService, _commerceOrderService,
			_commerceSkuService, _contractService, _entitlementService,
			_projectService, _provisioningContactService,
			_provisioningEmailService, _provisioningIssueService,
			_provisioningOrderService, _provisioningSubdomainService,
			_userAccountService);
	}

	private static final long _ACCOUNT_ID = 1000L;

	private static final String _ACCOUNT_ID_SF = "SF-ACCOUNT-1";

	private static final long _EXISTING_ORDER_ITEM_ID = 555L;

	private static final long _NEW_ORDER_ID = 4000L;

	private static final String _OPPORTUNITY_ID = "OPP-1";

	private static final long _ORDER_ITEM_ID = 3000L;

	private static final String _PARENT_OPPORTUNITY_ID = "OPP-PARENT";

	private static final String _PROJECT_ID = "PROJECT-1";

	private static final long _SKU_ID = 2000L;

	private Account _account;
	private AccountService _accountService;
	private CommerceOrderItemService _commerceOrderItemService;
	private CommerceOrderService _commerceOrderService;
	private CommerceSkuService _commerceSkuService;
	private ContractService _contractService;
	private EntitlementService _entitlementService;
	private ProjectService _projectService;
	private ProvisioningContactService _provisioningContactService;
	private ProvisioningEmailService _provisioningEmailService;
	private ProvisioningIssueService _provisioningIssueService;
	private ProvisioningOrderService _provisioningOrderService;
	private ProvisioningSubdomainService _provisioningSubdomainService;
	private SalesforceOpportunityPubsubSubscriber _subscriber;
	private UserAccountService _userAccountService;

}