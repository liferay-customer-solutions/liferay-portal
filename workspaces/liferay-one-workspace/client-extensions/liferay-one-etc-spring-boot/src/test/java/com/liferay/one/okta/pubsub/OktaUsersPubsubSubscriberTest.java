/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.okta.pubsub;

import com.liferay.headless.admin.user.client.custom.field.CustomField;
import com.liferay.headless.admin.user.client.custom.field.CustomValue;
import com.liferay.headless.admin.user.client.dto.v1_0.AccountBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.one.pubsub.Message;
import com.liferay.one.service.AccountService;
import com.liferay.one.service.ProvisioningAssignmentService;
import com.liferay.one.service.ProvisioningEmailService;
import com.liferay.one.service.UserAccountService;

import java.util.Collections;

import org.json.JSONObject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.InOrder;
import org.mockito.Mockito;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Felipe Franca
 */
public class OktaUsersPubsubSubscriberTest {

	@BeforeEach
	public void setUp() {
		_subscriber = new OktaUsersPubsubSubscriber();

		_accountService = Mockito.mock(AccountService.class);
		_provisioningAssignmentService = Mockito.mock(
			ProvisioningAssignmentService.class);
		_provisioningEmailService = Mockito.mock(
			ProvisioningEmailService.class);
		_userAccountService = Mockito.mock(UserAccountService.class);

		ReflectionTestUtils.setField(
			_subscriber, "_accountService", _accountService);
		ReflectionTestUtils.setField(_subscriber, "_projectId", "test-project");
		ReflectionTestUtils.setField(
			_subscriber, "_provisioningAssignmentService",
			_provisioningAssignmentService);
		ReflectionTestUtils.setField(
			_subscriber, "_provisioningEmailService",
			_provisioningEmailService);
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
	public void testReceiveDoesNotSendSecondWelcomeEmailWhenAlreadyVerified()
		throws Exception {

		UserAccount userAccount = _createUserAccount(
			_EMAIL_ADDRESS, _USER_ID, true);

		Mockito.when(
			_userAccountService.fetchUserAccountByEmailAddress(_EMAIL_ADDRESS)
		).thenReturn(
			userAccount
		);

		_receiveMessage(
			_EMAIL_ADDRESS, "user.account.update_profile", "ACTIVE");

		Mockito.verify(
			_userAccountService, Mockito.never()
		).setVerified(
			Mockito.anyLong()
		);

		Mockito.verifyNoInteractions(_provisioningEmailService);
	}

	@Test
	public void testReceiveDoesNotSendWelcomeEmailWhenOktaStatusIsNotVerified()
		throws Exception {

		UserAccount userAccount = _createUserAccount(
			_EMAIL_ADDRESS, _USER_ID, false);

		Mockito.when(
			_userAccountService.fetchUserAccountByEmailAddress(_EMAIL_ADDRESS)
		).thenReturn(
			userAccount
		);

		_receiveMessage(
			_EMAIL_ADDRESS, "user.account.update_profile", "PROVISIONED");

		Mockito.verify(
			_userAccountService, Mockito.never()
		).setVerified(
			Mockito.anyLong()
		);

		Mockito.verifyNoInteractions(_provisioningEmailService);
	}

	@Test
	public void testReceiveDoesNotSetVerifiedWhenAlreadyVerified()
		throws Exception {

		UserAccount userAccount = _createUserAccount(
			_EMAIL_ADDRESS, _USER_ID, true);

		Mockito.when(
			_userAccountService.fetchUserAccountByEmailAddress(_EMAIL_ADDRESS)
		).thenReturn(
			userAccount
		);

		_receiveMessage(_EMAIL_ADDRESS, "user.lifecycle.activate", "ACTIVE");

		Mockito.verify(
			_userAccountService, Mockito.never()
		).setVerified(
			Mockito.anyLong()
		);
	}

	@Test
	public void testReceiveDoesNotSetVerifiedWhenOktaStatusIsNotVerified()
		throws Exception {

		UserAccount userAccount = _createUserAccount(
			_EMAIL_ADDRESS, _USER_ID, false);

		Mockito.when(
			_userAccountService.fetchUserAccountByEmailAddress(_EMAIL_ADDRESS)
		).thenReturn(
			userAccount
		);

		_receiveMessage(
			_EMAIL_ADDRESS, "user.lifecycle.activate", "PROVISIONED");

		Mockito.verify(
			_userAccountService, Mockito.never()
		).setVerified(
			Mockito.anyLong()
		);
	}

	@Test
	public void testReceiveIgnoresAbsentEventType() throws Exception {
		Message message = new Message(
			Collections.emptyMap(),
			new JSONObject(
			).put(
				"user", _createUserJSONObject(_EMAIL_ADDRESS, "ACTIVE")
			).toString(),
			"test-topic");

		Assertions.assertDoesNotThrow(() -> _subscriber.receive(message));

		Mockito.verifyNoInteractions(
			_accountService, _provisioningAssignmentService,
			_provisioningEmailService, _userAccountService);
	}

	@Test
	public void testReceiveIgnoresUnknownEventType() throws Exception {
		_receiveMessage(_EMAIL_ADDRESS, "user.unknown.event", "ACTIVE");

		Mockito.verifyNoInteractions(
			_accountService, _provisioningAssignmentService,
			_provisioningEmailService, _userAccountService);
	}

	@Test
	public void testReceiveRemovesAccountUserAccountBeforeUnassigningMembership()
		throws Exception {

		AccountBrief accountBrief = new AccountBrief();

		accountBrief.setId(_FIRST_ACCOUNT_ID);

		UserAccount userAccount = _createUserAccount(
			_EMAIL_ADDRESS, _USER_ID, true, accountBrief);

		Mockito.when(
			_userAccountService.fetchUserAccountByEmailAddress(_EMAIL_ADDRESS)
		).thenReturn(
			userAccount
		);

		_receiveMessage(
			_EMAIL_ADDRESS, "user.lifecycle.deactivate", "DEPROVISIONED");

		InOrder inOrder = Mockito.inOrder(
			_accountService, _provisioningAssignmentService);

		inOrder.verify(
			_accountService
		).removeAccountUserAccount(
			_FIRST_ACCOUNT_ID, _USER_ID
		);

		inOrder.verify(
			_provisioningAssignmentService
		).unassignAccountMembership(
			_FIRST_ACCOUNT_ID, _USER_ID
		);
	}

	@Test
	public void testReceiveRethrowsOnMalformedPayload() {
		Message message = new Message(
			Collections.emptyMap(), "not json", "test-topic");

		Assertions.assertThrows(
			Exception.class, () -> _subscriber.receive(message));
	}

	@Test
	public void testReceiveReturnsQuietlyWhenAccountBriefsAreNull()
		throws Exception {

		UserAccount userAccount = new UserAccount();

		userAccount.setEmailAddress(_EMAIL_ADDRESS);
		userAccount.setId(_USER_ID);

		Mockito.when(
			_userAccountService.fetchUserAccountByEmailAddress(_EMAIL_ADDRESS)
		).thenReturn(
			userAccount
		);

		Assertions.assertDoesNotThrow(
			() -> _receiveMessage(
				_EMAIL_ADDRESS, "user.lifecycle.deactivate", "DEPROVISIONED"));

		Mockito.verify(
			_accountService, Mockito.never()
		).removeAccountUserAccount(
			Mockito.anyLong(), Mockito.anyLong()
		);

		Mockito.verify(
			_provisioningAssignmentService, Mockito.never()
		).unassignAccountMembership(
			Mockito.anyLong(), Mockito.anyLong()
		);
	}

	@Test
	public void testReceiveSetsVerifiedOnActivateWhenUnverified()
		throws Exception {

		UserAccount userAccount = _createUserAccount(
			_EMAIL_ADDRESS, _USER_ID, false);

		Mockito.when(
			_userAccountService.fetchUserAccountByEmailAddress(_EMAIL_ADDRESS)
		).thenReturn(
			userAccount
		);

		_receiveMessage(_EMAIL_ADDRESS, "user.lifecycle.activate", "ACTIVE");

		Mockito.verify(
			_userAccountService
		).setVerified(
			_USER_ID
		);

		Mockito.verifyNoInteractions(_provisioningEmailService);
	}

	@Test
	public void testReceiveSetsVerifiedOnCreateWhenUnverified()
		throws Exception {

		UserAccount userAccount = _createUserAccount(
			_EMAIL_ADDRESS, _USER_ID, false);

		Mockito.when(
			_userAccountService.fetchUserAccountByEmailAddress(_EMAIL_ADDRESS)
		).thenReturn(
			userAccount
		);

		_receiveMessage(_EMAIL_ADDRESS, "user.lifecycle.create", "ACTIVE");

		Mockito.verify(
			_userAccountService
		).setVerified(
			_USER_ID
		);
	}

	@Test
	public void testReceiveSetsVerifiedThenSendsVerifiedWelcomeEmail()
		throws Exception {

		UserAccount userAccount = _createUserAccount(
			_EMAIL_ADDRESS, _USER_ID, false);

		Mockito.when(
			_userAccountService.fetchUserAccountByEmailAddress(_EMAIL_ADDRESS)
		).thenReturn(
			userAccount
		);

		_receiveMessage(
			_EMAIL_ADDRESS, "user.account.update_profile", "ACTIVE");

		InOrder inOrder = Mockito.inOrder(
			_provisioningEmailService, _userAccountService);

		inOrder.verify(
			_userAccountService
		).setVerified(
			_USER_ID
		);

		inOrder.verify(
			_provisioningEmailService
		).sendVerifiedWelcomeEmail(
			userAccount
		);
	}

	@Test
	public void testReceiveSetsVerifiedThenSendsVerifiedWelcomeEmailOnPasswordUpdate()
		throws Exception {

		UserAccount userAccount = _createUserAccount(
			_EMAIL_ADDRESS, _USER_ID, false);

		Mockito.when(
			_userAccountService.fetchUserAccountByEmailAddress(_EMAIL_ADDRESS)
		).thenReturn(
			userAccount
		);

		_receiveMessage(
			_EMAIL_ADDRESS, "user.account.update_password", "ACTIVE");

		InOrder inOrder = Mockito.inOrder(
			_provisioningEmailService, _userAccountService);

		inOrder.verify(
			_userAccountService
		).setVerified(
			_USER_ID
		);

		inOrder.verify(
			_provisioningEmailService
		).sendVerifiedWelcomeEmail(
			userAccount
		);
	}

	@Test
	public void testReceiveSkipsDeactivateWhenNoPortalUserExists()
		throws Exception {

		Mockito.when(
			_userAccountService.fetchUserAccountByEmailAddress(_EMAIL_ADDRESS)
		).thenReturn(
			null
		);

		Assertions.assertDoesNotThrow(
			() -> _receiveMessage(
				_EMAIL_ADDRESS, "user.lifecycle.deactivate", "DEPROVISIONED"));

		Mockito.verifyNoInteractions(
			_accountService, _provisioningAssignmentService);
	}

	@Test
	public void testReceiveSkipsSyncContactWhenNoPortalUserExists()
		throws Exception {

		Mockito.when(
			_userAccountService.fetchUserAccountByEmailAddress(_EMAIL_ADDRESS)
		).thenReturn(
			null
		);

		Assertions.assertDoesNotThrow(
			() -> _receiveMessage(
				_EMAIL_ADDRESS, "user.lifecycle.activate", "ACTIVE"));

		Mockito.verify(
			_userAccountService, Mockito.never()
		).setVerified(
			Mockito.anyLong()
		);

		Mockito.verifyNoInteractions(
			_accountService, _provisioningAssignmentService,
			_provisioningEmailService);
	}

	@Test
	public void testReceiveSkipsUpdateContactWhenNoPortalUserExists()
		throws Exception {

		Mockito.when(
			_userAccountService.fetchUserAccountByEmailAddress(_EMAIL_ADDRESS)
		).thenReturn(
			null
		);

		_receiveMessage(
			_EMAIL_ADDRESS, "user.account.update_profile", "ACTIVE");

		Mockito.verifyNoInteractions(_provisioningEmailService);

		Mockito.verify(
			_userAccountService, Mockito.never()
		).setVerified(
			Mockito.anyLong()
		);
	}

	@Test
	public void testReceiveSkipsWhenEmailAddressIsAbsent() throws Exception {
		_receiveMessage("", "user.lifecycle.activate", "ACTIVE");

		Mockito.verify(
			_userAccountService, Mockito.never()
		).fetchUserAccountByEmailAddress(
			Mockito.any()
		);
	}

	@Test
	public void testReceiveSkipsWhenUserKeyIsAbsent() throws Exception {
		Message message = new Message(
			Collections.emptyMap(),
			new JSONObject(
			).put(
				"eventType", "user.lifecycle.activate"
			).toString(),
			"test-topic");

		Assertions.assertDoesNotThrow(() -> _subscriber.receive(message));

		Mockito.verifyNoInteractions(
			_accountService, _provisioningAssignmentService,
			_provisioningEmailService);

		Mockito.verify(
			_userAccountService, Mockito.never()
		).fetchUserAccountByEmailAddress(
			Mockito.any()
		);
	}

	@Test
	public void testReceiveStopsProcessingRemainingMembershipsAfterFailure()
		throws Exception {

		AccountBrief firstAccountBrief = new AccountBrief();

		firstAccountBrief.setId(_FIRST_ACCOUNT_ID);

		AccountBrief secondAccountBrief = new AccountBrief();

		secondAccountBrief.setId(_SECOND_ACCOUNT_ID);

		UserAccount userAccount = _createUserAccount(
			_EMAIL_ADDRESS, _USER_ID, true, firstAccountBrief,
			secondAccountBrief);

		Mockito.when(
			_userAccountService.fetchUserAccountByEmailAddress(_EMAIL_ADDRESS)
		).thenReturn(
			userAccount
		);

		Mockito.doThrow(
			new RuntimeException("Unable to remove account user account")
		).when(
			_accountService
		).removeAccountUserAccount(
			_FIRST_ACCOUNT_ID, _USER_ID
		);

		Message message = new Message(
			Collections.emptyMap(),
			new JSONObject(
			).put(
				"eventType", "user.lifecycle.deactivate"
			).put(
				"user", _createUserJSONObject(_EMAIL_ADDRESS, "DEPROVISIONED")
			).toString(),
			"test-topic");

		Assertions.assertThrows(
			RuntimeException.class, () -> _subscriber.receive(message));

		Mockito.verify(
			_accountService, Mockito.never()
		).removeAccountUserAccount(
			_SECOND_ACCOUNT_ID, _USER_ID
		);

		Mockito.verify(
			_provisioningAssignmentService, Mockito.never()
		).unassignAccountMembership(
			_SECOND_ACCOUNT_ID, _USER_ID
		);
	}

	@Test
	public void testReceiveUnassignsAllMembershipsOnDeactivate()
		throws Exception {

		AccountBrief firstAccountBrief = new AccountBrief();

		firstAccountBrief.setId(_FIRST_ACCOUNT_ID);

		AccountBrief secondAccountBrief = new AccountBrief();

		secondAccountBrief.setId(_SECOND_ACCOUNT_ID);

		UserAccount userAccount = _createUserAccount(
			_EMAIL_ADDRESS, _USER_ID, true, firstAccountBrief,
			secondAccountBrief);

		Mockito.when(
			_userAccountService.fetchUserAccountByEmailAddress(_EMAIL_ADDRESS)
		).thenReturn(
			userAccount
		);

		_receiveMessage(
			_EMAIL_ADDRESS, "user.lifecycle.deactivate", "DEPROVISIONED");

		Mockito.verify(
			_accountService
		).removeAccountUserAccount(
			_FIRST_ACCOUNT_ID, _USER_ID
		);

		Mockito.verify(
			_accountService
		).removeAccountUserAccount(
			_SECOND_ACCOUNT_ID, _USER_ID
		);

		Mockito.verify(
			_provisioningAssignmentService
		).unassignAccountMembership(
			_FIRST_ACCOUNT_ID, _USER_ID
		);

		Mockito.verify(
			_provisioningAssignmentService
		).unassignAccountMembership(
			_SECOND_ACCOUNT_ID, _USER_ID
		);
	}

	private UserAccount _createUserAccount(
		String emailAddress, long id, boolean verified,
		AccountBrief... accountBriefs) {

		UserAccount userAccount = new UserAccount();

		userAccount.setAccountBriefs(accountBriefs);
		userAccount.setEmailAddress(emailAddress);
		userAccount.setId(id);

		CustomValue customValue = new CustomValue();

		customValue.setData(verified);

		CustomField customField = new CustomField();

		customField.setCustomValue(customValue);
		customField.setName("verified");

		userAccount.setCustomFields(new CustomField[] {customField});

		return userAccount;
	}

	private JSONObject _createUserJSONObject(String email, String status) {
		return new JSONObject(
		).put(
			"profile",
			new JSONObject(
			).put(
				"email", email
			)
		).put(
			"status", status
		);
	}

	private void _receiveMessage(String email, String eventType, String status)
		throws Exception {

		Message message = new Message(
			Collections.emptyMap(),
			new JSONObject(
			).put(
				"eventType", eventType
			).put(
				"user", _createUserJSONObject(email, status)
			).toString(),
			"test-topic");

		_subscriber.receive(message);
	}

	private static final String _EMAIL_ADDRESS = "contact@example.com";

	private static final long _FIRST_ACCOUNT_ID = 10L;

	private static final long _SECOND_ACCOUNT_ID = 20L;

	private static final long _USER_ID = 100L;

	private AccountService _accountService;
	private ProvisioningAssignmentService _provisioningAssignmentService;
	private ProvisioningEmailService _provisioningEmailService;
	private OktaUsersPubsubSubscriber _subscriber;
	private UserAccountService _userAccountService;

}