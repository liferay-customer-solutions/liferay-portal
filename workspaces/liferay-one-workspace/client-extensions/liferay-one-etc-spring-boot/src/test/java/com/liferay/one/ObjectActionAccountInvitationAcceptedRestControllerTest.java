/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.liferay.headless.admin.user.client.dto.v1_0.Account;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.one.model.AccountInvitation;
import com.liferay.one.service.AccountInvitationAcceptanceService;
import com.liferay.one.service.AccountInvitationService;
import com.liferay.one.service.AccountService;
import com.liferay.one.service.ProjectMembershipService;
import com.liferay.one.service.UserAccountService;

import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import org.junit.jupiter.api.Test;

import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Pedro Oliveira
 */
public class ObjectActionAccountInvitationAcceptedRestControllerTest {

	@Test
	public void testPostAddsProjectMembershipForProjectInvitation()
		throws Exception {

		ObjectActionAccountInvitationAcceptedRestController
			objectActionAccountInvitationAcceptedRestController =
				_createController();

		Mockito.when(
			_accountInvitationService.fetchAccountInvitation(
				_ACCOUNT_INVITATION_ID)
		).thenReturn(
			_createAccountInvitation(
				true, _PROJECT_EXTERNAL_REFERENCE_CODE, List.of())
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

		objectActionAccountInvitationAcceptedRestController.post(
			null, _createPayload());

		Mockito.verify(
			_projectMembershipService
		).addProjectMembership(
			_PROJECT_EXTERNAL_REFERENCE_CODE, _PROJECT_ROLE_ERC, _USER_ID
		);
	}

	@Test
	public void testPostCreatesUserAccountWithInvitedName() throws Exception {
		ObjectActionAccountInvitationAcceptedRestController
			objectActionAccountInvitationAcceptedRestController =
				_createController();

		Mockito.when(
			_accountInvitationService.fetchAccountInvitation(
				_ACCOUNT_INVITATION_ID)
		).thenReturn(
			_createAccountInvitation(true, "", List.of())
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

		objectActionAccountInvitationAcceptedRestController.post(
			null, _createPayload());

		Mockito.verify(
			_userAccountService
		).addUserAccount(
			_EMAIL_ADDRESS, "Doe", "Jane"
		);

		Mockito.verify(
			_accountService
		).addAccountUserAccountByEmailAddress(
			_ACCOUNT_ID, _EMAIL_ADDRESS, null
		);
	}

	@Test
	public void testPostResolvesRolesWithOneLookup() throws Exception {
		ObjectActionAccountInvitationAcceptedRestController
			objectActionAccountInvitationAcceptedRestController =
				_createController();

		Mockito.when(
			_accountInvitationService.fetchAccountInvitation(
				_ACCOUNT_INVITATION_ID)
		).thenReturn(
			_createAccountInvitation(
				true, "", List.of("L_ACCOUNT_ADMINISTRATOR", "C_ACCOUNT_BUYER"))
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
			_accountService.getAccountRoleIdsByExternalReferenceCode(
				_ACCOUNT_ID)
		).thenReturn(
			Map.of(
				"C_ACCOUNT_BUYER", 55555L, "L_ACCOUNT_ADMINISTRATOR",
				_ACCOUNT_ROLE_ID)
		);

		objectActionAccountInvitationAcceptedRestController.post(
			null, _createPayload());

		Mockito.verify(
			_accountService, Mockito.times(1)
		).getAccountRoleIdsByExternalReferenceCode(
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
	public void testPostSkipsMissingInvitation() throws Exception {
		ObjectActionAccountInvitationAcceptedRestController
			objectActionAccountInvitationAcceptedRestController =
				_createController();

		Mockito.when(
			_accountInvitationService.fetchAccountInvitation(
				_ACCOUNT_INVITATION_ID)
		).thenReturn(
			null
		);

		objectActionAccountInvitationAcceptedRestController.post(
			null, _createPayload());

		Mockito.verifyNoInteractions(_accountService);
		Mockito.verifyNoInteractions(_userAccountService);
	}

	@Test
	public void testPostSkipsProjectMembershipForAccountInvitation()
		throws Exception {

		ObjectActionAccountInvitationAcceptedRestController
			objectActionAccountInvitationAcceptedRestController =
				_createController();

		Mockito.when(
			_accountInvitationService.fetchAccountInvitation(
				_ACCOUNT_INVITATION_ID)
		).thenReturn(
			_createAccountInvitation(true, "", List.of())
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

		objectActionAccountInvitationAcceptedRestController.post(
			null, _createPayload());

		Mockito.verifyNoInteractions(_projectMembershipService);
	}

	@Test
	public void testPostSkipsUnacceptedInvitation() throws Exception {
		ObjectActionAccountInvitationAcceptedRestController
			objectActionAccountInvitationAcceptedRestController =
				_createController();

		Mockito.when(
			_accountInvitationService.fetchAccountInvitation(
				_ACCOUNT_INVITATION_ID)
		).thenReturn(
			_createAccountInvitation(false, "", List.of())
		);

		objectActionAccountInvitationAcceptedRestController.post(
			null, _createPayload());

		Mockito.verifyNoInteractions(_accountService);
		Mockito.verifyNoInteractions(_userAccountService);
	}

	private Account _createAccount() {
		Account account = new Account();

		account.setExternalReferenceCode(_EXTERNAL_REFERENCE_CODE);
		account.setId(_ACCOUNT_ID);

		return account;
	}

	private AccountInvitation _createAccountInvitation(
		boolean accepted, String projectExternalReferenceCode,
		List<String> roleExternalReferenceCodes) {

		return new AccountInvitation(
			new JSONObject(
			).put(
				"accepted", accepted
			).put(
				"accountExternalReferenceCode", _EXTERNAL_REFERENCE_CODE
			).put(
				"customExpirationDate", "2999-01-01T00:00:00Z"
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
				"projectExternalReferenceCode", projectExternalReferenceCode
			).put(
				"projectRoleExternalReferenceCode", _PROJECT_ROLE_ERC
			).put(
				"roleExternalReferenceCodes",
				new JSONArray(
					roleExternalReferenceCodes
				).toString()
			).put(
				"token", "11111111-2222-3333-4444-555555555555"
			));
	}

	private ObjectActionAccountInvitationAcceptedRestController
		_createController() {

		AccountInvitationAcceptanceService accountInvitationAcceptanceService =
			new AccountInvitationAcceptanceService();

		ReflectionTestUtils.setField(
			accountInvitationAcceptanceService, "_accountService",
			_accountService);
		ReflectionTestUtils.setField(
			accountInvitationAcceptanceService, "_projectMembershipService",
			_projectMembershipService);
		ReflectionTestUtils.setField(
			accountInvitationAcceptanceService, "_userAccountService",
			_userAccountService);

		ObjectActionAccountInvitationAcceptedRestController
			objectActionAccountInvitationAcceptedRestController =
				new ObjectActionAccountInvitationAcceptedRestController();

		ReflectionTestUtils.setField(
			objectActionAccountInvitationAcceptedRestController,
			"_accountInvitationAcceptanceService",
			accountInvitationAcceptanceService);
		ReflectionTestUtils.setField(
			objectActionAccountInvitationAcceptedRestController,
			"_accountInvitationService", _accountInvitationService);

		return objectActionAccountInvitationAcceptedRestController;
	}

	private String _createPayload() {
		JSONObject jsonObject = new JSONObject();

		jsonObject.put(
			"objectEntry",
			new JSONObject(
			).put(
				"id", _ACCOUNT_INVITATION_ID
			));

		return jsonObject.toString();
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

	private static final String _PROJECT_EXTERNAL_REFERENCE_CODE = "PRJCT-1";

	private static final String _PROJECT_ROLE_ERC = "C_PROJECT_ADMIN";

	private static final long _USER_ID = 22222;

	private final AccountInvitationService _accountInvitationService =
		Mockito.mock(AccountInvitationService.class);
	private final AccountService _accountService = Mockito.mock(
		AccountService.class);
	private final ProjectMembershipService _projectMembershipService =
		Mockito.mock(ProjectMembershipService.class);
	private final UserAccountService _userAccountService = Mockito.mock(
		UserAccountService.class);

}