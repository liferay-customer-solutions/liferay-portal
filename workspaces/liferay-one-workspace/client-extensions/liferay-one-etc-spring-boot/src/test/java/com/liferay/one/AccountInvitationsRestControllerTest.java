/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.liferay.headless.admin.user.client.dto.v1_0.Account;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.one.jira.util.JiraSyncLock;
import com.liferay.one.model.AccountInvitation;
import com.liferay.one.service.AccountInvitationService;
import com.liferay.one.service.AccountService;
import com.liferay.one.service.UserAccountService;

import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Pedro Oliveira
 */
public class AccountInvitationsRestControllerTest {

	@Test
	public void testGetAcceptCreatesUserAccountWithInvitedName()
		throws Exception {

		AccountInvitationsRestController accountInvitationsRestController =
			_createController();

		Mockito.when(
			_accountInvitationService.fetchAccountInvitationByToken(_TOKEN)
		).thenReturn(
			_createAccountInvitation(false, List.of())
		);

		Mockito.when(
			_accountService.getAccount(_EXTERNAL_REFERENCE_CODE)
		).thenReturn(
			_createAccount()
		);

		Mockito.when(
			_userAccountService.fetchUserAccountByEmailAddress(_EMAIL_ADDRESS)
		).thenReturn(
			null
		);

		Mockito.when(
			_userAccountService.addUserAccount(_EMAIL_ADDRESS, "Doe", "Jane")
		).thenReturn(
			_createUserAccount()
		);

		ResponseEntity<Void> responseEntity =
			accountInvitationsRestController.getAccept(_TOKEN);

		Assertions.assertEquals(
			HttpStatus.FOUND, responseEntity.getStatusCode());

		Mockito.verify(
			_userAccountService
		).addUserAccount(
			_EMAIL_ADDRESS, "Doe", "Jane"
		);

		Mockito.verify(
			_accountInvitationService
		).updateAccepted(
			_ACCOUNT_INVITATION_ID
		);
	}

	@Test
	public void testGetAcceptRedirectsErrorWhenAcceptFails() throws Exception {
		AccountInvitationsRestController accountInvitationsRestController =
			_createController();

		Mockito.when(
			_accountInvitationService.fetchAccountInvitationByToken(_TOKEN)
		).thenReturn(
			_createAccountInvitation(false, List.of())
		);

		Mockito.when(
			_accountService.getAccount(_EXTERNAL_REFERENCE_CODE)
		).thenThrow(
			new IllegalStateException("Unable to find the account")
		);

		ResponseEntity<Void> responseEntity =
			accountInvitationsRestController.getAccept(_TOKEN);

		Assertions.assertEquals(
			HttpStatus.FOUND, responseEntity.getStatusCode());
		Assertions.assertEquals(
			_PORTAL_URL + "/?invitation=error",
			String.valueOf(
				responseEntity.getHeaders(
				).getLocation()));

		Mockito.verify(
			_accountInvitationService, Mockito.never()
		).updateAccepted(
			ArgumentMatchers.anyLong()
		);
	}

	@Test
	public void testGetAcceptRedirectsInvalidTokenWithoutLookup()
		throws Exception {

		AccountInvitationsRestController accountInvitationsRestController =
			_createController();

		ResponseEntity<Void> responseEntity =
			accountInvitationsRestController.getAccept("x' or 'a' eq 'a");

		Assertions.assertEquals(
			HttpStatus.FOUND, responseEntity.getStatusCode());
		Assertions.assertEquals(
			_PORTAL_URL + "/?invitation=invalid",
			String.valueOf(
				responseEntity.getHeaders(
				).getLocation()));

		Mockito.verifyNoInteractions(_accountInvitationService);
	}

	@Test
	public void testGetAcceptResolvesRolesWithOneLookup() throws Exception {
		AccountInvitationsRestController accountInvitationsRestController =
			_createController();

		Mockito.when(
			_accountInvitationService.fetchAccountInvitationByToken(_TOKEN)
		).thenReturn(
			_createAccountInvitation(
				false, List.of("Account Administrator", "Account Member"))
		);

		Mockito.when(
			_accountService.getAccount(_EXTERNAL_REFERENCE_CODE)
		).thenReturn(
			_createAccount()
		);

		Mockito.when(
			_userAccountService.fetchUserAccountByEmailAddress(_EMAIL_ADDRESS)
		).thenReturn(
			_createUserAccount()
		);

		Mockito.when(
			_accountService.getAccountRoleNames(_ACCOUNT_ID)
		).thenReturn(
			Map.of(
				_ACCOUNT_ROLE_ID, "Account Administrator", 55555L,
				"Account Member")
		);

		accountInvitationsRestController.getAccept(_TOKEN);

		Mockito.verify(
			_accountService, Mockito.times(1)
		).getAccountRoleNames(
			_ACCOUNT_ID
		);

		Mockito.verify(
			_accountService, Mockito.never()
		).fetchAccountRoleId(
			ArgumentMatchers.anyLong(), ArgumentMatchers.anyString()
		);

		Mockito.verify(
			_accountService
		).addAccountUserAccountRole(
			_ACCOUNT_ID, _ACCOUNT_ROLE_ID, _USER_ID
		);

		Mockito.verify(
			_accountService
		).addAccountUserAccountRole(
			_ACCOUNT_ID, 55555L, _USER_ID
		);
	}

	@Test
	public void testGetAcceptSkipsAcceptedInvitation() throws Exception {
		AccountInvitationsRestController accountInvitationsRestController =
			_createController();

		Mockito.when(
			_accountInvitationService.fetchAccountInvitationByToken(_TOKEN)
		).thenReturn(
			_createAccountInvitation(true, List.of())
		);

		ResponseEntity<Void> responseEntity =
			accountInvitationsRestController.getAccept(_TOKEN);

		Assertions.assertEquals(
			HttpStatus.FOUND, responseEntity.getStatusCode());
		Assertions.assertEquals(
			_PORTAL_URL + "/?invitation=accepted",
			String.valueOf(
				responseEntity.getHeaders(
				).getLocation()));

		Mockito.verifyNoInteractions(_accountService);
	}

	private Account _createAccount() {
		Account account = new Account();

		account.setExternalReferenceCode(_EXTERNAL_REFERENCE_CODE);
		account.setId(_ACCOUNT_ID);

		return account;
	}

	private AccountInvitation _createAccountInvitation(
		boolean accepted, List<String> roleNames) {

		return new AccountInvitation(
			new JSONObject(
			).put(
				"accepted", accepted
			).put(
				"accountExternalReferenceCode", _EXTERNAL_REFERENCE_CODE
			).put(
				"emailAddress", _EMAIL_ADDRESS
			).put(
				"externalReferenceCode", "INV-1"
			).put(
				"familyName", "Doe"
			).put(
				"givenName", "Jane"
			).put(
				"id", _ACCOUNT_INVITATION_ID
			).put(
				"roleNames",
				new JSONArray(
					roleNames
				).toString()
			).put(
				"token", _TOKEN
			));
	}

	private AccountInvitationsRestController _createController() {
		AccountInvitationsRestController accountInvitationsRestController =
			new AccountInvitationsRestController();

		ReflectionTestUtils.setField(
			accountInvitationsRestController, "_accountInvitationService",
			_accountInvitationService);
		ReflectionTestUtils.setField(
			accountInvitationsRestController, "_accountService",
			_accountService);
		ReflectionTestUtils.setField(
			accountInvitationsRestController, "_jiraSyncLock",
			new JiraSyncLock());
		ReflectionTestUtils.setField(
			accountInvitationsRestController, "_portalURL", _PORTAL_URL);
		ReflectionTestUtils.setField(
			accountInvitationsRestController, "_userAccountService",
			_userAccountService);

		return accountInvitationsRestController;
	}

	private UserAccount _createUserAccount() {
		UserAccount userAccount = new UserAccount();

		userAccount.setEmailAddress(_EMAIL_ADDRESS);
		userAccount.setId(_USER_ID);

		return userAccount;
	}

	private static final long _ACCOUNT_ID = 11111;

	private static final long _ACCOUNT_INVITATION_ID = 44444;

	private static final long _ACCOUNT_ROLE_ID = 33333;

	private static final String _EMAIL_ADDRESS = "jane@example.com";

	private static final String _EXTERNAL_REFERENCE_CODE = "ACC-1";

	private static final String _PORTAL_URL = "http://localhost:8080";

	private static final String _TOKEN = "11111111-2222-3333-4444-555555555555";

	private static final long _USER_ID = 22222;

	private final AccountInvitationService _accountInvitationService =
		Mockito.mock(AccountInvitationService.class);
	private final AccountService _accountService = Mockito.mock(
		AccountService.class);
	private final UserAccountService _userAccountService = Mockito.mock(
		UserAccountService.class);

}