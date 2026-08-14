/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.liferay.headless.admin.user.client.dto.v1_0.Account;
import com.liferay.headless.admin.user.client.dto.v1_0.AccountBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.RoleBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.one.jira.service.AccountAssetService;
import com.liferay.one.jira.synchronizer.AccountSynchronizer;
import com.liferay.one.jira.synchronizer.AccountUserAccountRoleSynchronizer;
import com.liferay.one.jira.synchronizer.AccountUserAccountSynchronizer;
import com.liferay.one.model.AccountInvitation;
import com.liferay.one.model.Project;
import com.liferay.one.okta.model.OktaUser;
import com.liferay.one.okta.service.OktaService;
import com.liferay.one.permission.AccountPermission;
import com.liferay.one.permission.ProjectMembershipPermission;
import com.liferay.one.service.AccountInvitationEmailService;
import com.liferay.one.service.AccountInvitationService;
import com.liferay.one.service.AccountService;
import com.liferay.one.service.EmailAddressValidatorService;
import com.liferay.one.service.EntitlementService;
import com.liferay.one.service.ProjectService;
import com.liferay.one.service.ProvisioningAssignmentService;
import com.liferay.one.service.ProvisioningEmailService;
import com.liferay.one.service.UserAccountService;
import com.liferay.one.util.KeyedLock;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.security.permission.ActionKeys;

import java.lang.reflect.Field;

import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.Mockito;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author Ricardo Mariz
 */
public class AccountsRestControllerTest {

	@Test
	public void testDeleteInvitationsDeletesPendingInvitation()
		throws Exception {

		AccountsRestController accountsRestController = _createController();

		Mockito.when(
			_accountInvitationService.fetchAccountInvitation(
				_ACCOUNT_INVITATION_ID)
		).thenReturn(
			_createAccountInvitation(
				false, _EXTERNAL_REFERENCE_CODE, StringPool.BLANK)
		);

		accountsRestController.deleteInvitations(
			null, _EXTERNAL_REFERENCE_CODE, _ACCOUNT_INVITATION_ID);

		Mockito.verify(
			_accountPermission
		).check(
			_EXTERNAL_REFERENCE_CODE, ActionKeys.UPDATE, null
		);

		Mockito.verify(
			_accountInvitationService
		).deleteAccountInvitation(
			_ACCOUNT_INVITATION_ID
		);
	}

	@Test
	public void testDeleteInvitationsRejectsInvitationFromAnotherAccount()
		throws Exception {

		AccountsRestController accountsRestController = _createController();

		Mockito.when(
			_accountInvitationService.fetchAccountInvitation(
				_ACCOUNT_INVITATION_ID)
		).thenReturn(
			_createAccountInvitation(false, "ACC-OTHER", StringPool.BLANK)
		);

		ResponseStatusException responseStatusException =
			Assertions.assertThrows(
				ResponseStatusException.class,
				() -> accountsRestController.deleteInvitations(
					null, _EXTERNAL_REFERENCE_CODE, _ACCOUNT_INVITATION_ID));

		Assertions.assertEquals(
			HttpStatus.NOT_FOUND, responseStatusException.getStatusCode());

		Mockito.verifyNoInteractions(_accountPermission);

		Mockito.verify(
			_accountInvitationService, Mockito.never()
		).deleteAccountInvitation(
			ArgumentMatchers.anyLong()
		);
	}

	@Test
	public void testDeleteUserAccountsAccountRoleResolvesRoleNameBeforeRemoving()
		throws Exception {

		AccountsRestController accountsRestController = _createController();

		Account account = _createAccount();

		Mockito.when(
			_accountService.getAccount(_EXTERNAL_REFERENCE_CODE, null)
		).thenReturn(
			account
		);

		Mockito.when(
			_accountService.getAccountRoleName(_ACCOUNT_ID, _ACCOUNT_ROLE_ID)
		).thenReturn(
			"Partner Manager"
		);

		accountsRestController.deleteUserAccountsAccountRole(
			null, _EXTERNAL_REFERENCE_CODE, _USER_ID, _ACCOUNT_ROLE_ID);

		InOrder inOrder = Mockito.inOrder(_accountService);

		inOrder.verify(
			_accountService
		).getAccountRoleName(
			_ACCOUNT_ID, _ACCOUNT_ROLE_ID
		);

		inOrder.verify(
			_accountService
		).removeAccountUserAccountRole(
			_ACCOUNT_ROLE_ID, _EXTERNAL_REFERENCE_CODE, null, _USER_ID
		);

		Mockito.verify(
			_provisioningAssignmentService
		).unassignAccountRole(
			account, _USER_ID, "Partner Manager"
		);
	}

	@Test
	public void testDeleteUserAccountsAccountRoleSkipsSideEffectsForUnknownRole()
		throws Exception {

		AccountsRestController accountsRestController = _createController();

		Mockito.when(
			_accountService.getAccount(_EXTERNAL_REFERENCE_CODE, null)
		).thenReturn(
			_createAccount()
		);

		accountsRestController.deleteUserAccountsAccountRole(
			null, _EXTERNAL_REFERENCE_CODE, _USER_ID, _ACCOUNT_ROLE_ID);

		Mockito.verify(
			_accountService
		).removeAccountUserAccountRole(
			_ACCOUNT_ROLE_ID, _EXTERNAL_REFERENCE_CODE, null, _USER_ID
		);

		Mockito.verifyNoInteractions(_provisioningAssignmentService);
	}

	@Test
	public void testDeleteUserAccountsUnassignsAccountMembership()
		throws Exception {

		AccountsRestController accountsRestController = _createController();

		Mockito.when(
			_accountService.getAccount(_EXTERNAL_REFERENCE_CODE, null)
		).thenReturn(
			_createAccount()
		);

		Mockito.when(
			_userAccountService.getUserAccount(_USER_ID)
		).thenReturn(
			_createUserAccount()
		);

		accountsRestController.deleteUserAccounts(
			null, _EXTERNAL_REFERENCE_CODE, _USER_ID);

		Mockito.verify(
			_accountService
		).removeAccountUserAccount(
			_EXTERNAL_REFERENCE_CODE, null, _USER_ID
		);

		Mockito.verify(
			_provisioningAssignmentService
		).unassignAccountMembership(
			_ACCOUNT_ID, _USER_ID
		);
	}

	@Test
	public void testDeleteUserAccountsUnassignsContactRolesAndSyncsMembership()
		throws Exception {

		AccountsRestController accountsRestController = _createController();

		Account account = _createAccount();

		Mockito.when(
			_accountService.getAccount(_EXTERNAL_REFERENCE_CODE, null)
		).thenReturn(
			account
		);

		UserAccount userAccount = _createUserAccount(
			_ACCOUNT_ID, "Account Member");

		userAccount.setExternalReferenceCode("USER-ERC-1");

		AccountBrief accountBrief = userAccount.getAccountBriefs()[0];

		accountBrief.setExternalReferenceCode(_EXTERNAL_REFERENCE_CODE);

		RoleBrief roleBrief = accountBrief.getRoleBriefs()[0];

		roleBrief.setExternalReferenceCode("ROLE-ERC-1");

		Mockito.when(
			_userAccountService.getUserAccount(_USER_ID)
		).thenReturn(
			userAccount
		);

		accountsRestController.deleteUserAccounts(
			null, _EXTERNAL_REFERENCE_CODE, _USER_ID);

		Mockito.verify(
			_accountUserAccountRoleSynchronizer
		).syncUnassignRole(
			"ROLE-ERC-1", "USER-ERC-1", _EXTERNAL_REFERENCE_CODE
		);

		Mockito.verify(
			_accountUserAccountSynchronizer
		).syncAccountUserAccountMembership(
			account, userAccount
		);
	}

	@Test
	public void testGetInvitationsOmitsTokenAndResolvesRoleNames()
		throws Exception {

		AccountsRestController accountsRestController = _createController();

		Mockito.when(
			_accountService.getAccount(_EXTERNAL_REFERENCE_CODE, null)
		).thenReturn(
			_createAccount()
		);

		Mockito.when(
			_accountService.getAccountRoleNamesByExternalReferenceCode(
				_ACCOUNT_ID)
		).thenReturn(
			Map.of("L_ACCOUNT_ADMINISTRATOR", "Account Administrator")
		);

		Mockito.when(
			_accountInvitationService.getPendingAccountInvitations(
				_EXTERNAL_REFERENCE_CODE)
		).thenReturn(
			List.of(
				_createAccountInvitation(
					false, _EXTERNAL_REFERENCE_CODE, StringPool.BLANK,
					"L_ACCOUNT_ADMINISTRATOR", "C_UNKNOWN"))
		);

		ResponseEntity<String> responseEntity =
			accountsRestController.getInvitations(
				null, _EXTERNAL_REFERENCE_CODE);

		Mockito.verify(
			_accountPermission
		).check(
			_EXTERNAL_REFERENCE_CODE, ActionKeys.VIEW, null
		);

		JSONArray jsonArray = new JSONArray(responseEntity.getBody());

		Assertions.assertEquals(1, jsonArray.length());

		JSONObject jsonObject = jsonArray.getJSONObject(0);

		Assertions.assertFalse(jsonObject.has("token"));
		Assertions.assertEquals(
			_ACCOUNT_INVITATION_ID, jsonObject.getLong("id"));
		Assertions.assertEquals(
			_EMAIL_ADDRESS, jsonObject.getString("emailAddress"));

		JSONArray roleNamesJSONArray = jsonObject.getJSONArray("roleNames");

		Assertions.assertEquals(1, roleNamesJSONArray.length());
		Assertions.assertEquals(
			"Account Administrator", roleNamesJSONArray.getString(0));
	}

	@Test
	public void testPostInvitationsCreatesProjectInvitation() throws Exception {
		AccountsRestController accountsRestController = _createController();

		Account account = _createAccount();

		Mockito.when(
			_accountService.getAccount(_EXTERNAL_REFERENCE_CODE, null)
		).thenReturn(
			account
		);

		Mockito.when(
			_userAccountService.getMyUserAccount(null)
		).thenReturn(
			_createInviterUserAccount()
		);

		Mockito.when(
			_projectService.fetchProject(_PROJECT_EXTERNAL_REFERENCE_CODE)
		).thenReturn(
			_createProject(_EXTERNAL_REFERENCE_CODE)
		);

		AccountInvitation accountInvitation = _createAccountInvitation();

		Mockito.when(
			_accountInvitationService.addAccountInvitation(
				_EXTERNAL_REFERENCE_CODE, _EMAIL_ADDRESS, "Doe", "Jane",
				_PROJECT_EXTERNAL_REFERENCE_CODE, _PROJECT_ROLE_ERC, List.of())
		).thenReturn(
			accountInvitation
		);

		accountsRestController.postInvitations(
			null, _EXTERNAL_REFERENCE_CODE, _createProjectInvitationBodyJSON());

		Mockito.verify(
			_projectMembershipPermission
		).check(
			ActionKeys.UPDATE, null, _PROJECT_EXTERNAL_REFERENCE_CODE
		);

		Mockito.verifyNoInteractions(_accountPermission);

		Mockito.verify(
			_accountInvitationService
		).addAccountInvitation(
			_EXTERNAL_REFERENCE_CODE, _EMAIL_ADDRESS, "Doe", "Jane",
			_PROJECT_EXTERNAL_REFERENCE_CODE, _PROJECT_ROLE_ERC, List.of()
		);

		Mockito.verify(
			_accountInvitationEmailService
		).sendInvitationEmail(
			account, accountInvitation, "Inviter Name", "Project One"
		);
	}

	@Test
	public void testPostInvitationsLooksUpInviterBeforeCreatingInvitation()
		throws Exception {

		AccountsRestController accountsRestController = _createController();

		Account account = _createAccount();

		Mockito.when(
			_accountService.getAccount(_EXTERNAL_REFERENCE_CODE, null)
		).thenReturn(
			account
		);

		Mockito.when(
			_userAccountService.getMyUserAccount(null)
		).thenReturn(
			_createInviterUserAccount()
		);

		AccountInvitation accountInvitation = _createAccountInvitation();

		Mockito.when(
			_accountInvitationService.addAccountInvitation(
				_EXTERNAL_REFERENCE_CODE, _EMAIL_ADDRESS, "Doe", "Jane", "", "",
				List.of())
		).thenReturn(
			accountInvitation
		);

		accountsRestController.postInvitations(
			null, _EXTERNAL_REFERENCE_CODE, _createInvitationBodyJSON());

		InOrder inOrder = Mockito.inOrder(
			_userAccountService, _accountInvitationService,
			_accountInvitationEmailService);

		inOrder.verify(
			_userAccountService
		).getMyUserAccount(
			null
		);

		inOrder.verify(
			_accountInvitationService
		).addAccountInvitation(
			_EXTERNAL_REFERENCE_CODE, _EMAIL_ADDRESS, "Doe", "Jane", "", "",
			List.of()
		);

		inOrder.verify(
			_accountInvitationEmailService
		).sendInvitationEmail(
			account, accountInvitation, "Inviter Name", null
		);
	}

	@Test
	public void testPostInvitationsRejectsExistingMember() throws Exception {
		AccountsRestController accountsRestController = _createController();

		Mockito.when(
			_accountService.getAccount(_EXTERNAL_REFERENCE_CODE, null)
		).thenReturn(
			_createAccount()
		);

		Mockito.when(
			_userAccountService.fetchUserAccountByEmailAddress(_EMAIL_ADDRESS)
		).thenReturn(
			_createUserAccount(_ACCOUNT_ID, "Account Member")
		);

		ResponseStatusException responseStatusException =
			Assertions.assertThrows(
				ResponseStatusException.class,
				() -> accountsRestController.postInvitations(
					null, _EXTERNAL_REFERENCE_CODE,
					_createInvitationBodyJSON()));

		Assertions.assertEquals(
			HttpStatus.CONFLICT, responseStatusException.getStatusCode());

		Mockito.verifyNoInteractions(_accountInvitationService);
	}

	@Test
	public void testPostInvitationsRejectsInvalidEmailAddress()
		throws Exception {

		AccountsRestController accountsRestController = _createController();

		ResponseStatusException responseStatusException =
			Assertions.assertThrows(
				ResponseStatusException.class,
				() -> accountsRestController.postInvitations(
					null, _EXTERNAL_REFERENCE_CODE,
					new JSONObject(
					).put(
						"emailAddress", "jane"
					).put(
						"familyName", "Doe"
					).put(
						"givenName", "Jane"
					).toString()));

		Assertions.assertEquals(
			HttpStatus.BAD_REQUEST, responseStatusException.getStatusCode());

		Mockito.verifyNoInteractions(_accountInvitationService);
	}

	@Test
	public void testPostInvitationsRejectsMalformedBody() throws Exception {
		AccountsRestController accountsRestController = _createController();

		ResponseStatusException responseStatusException =
			Assertions.assertThrows(
				ResponseStatusException.class,
				() -> accountsRestController.postInvitations(
					null, _EXTERNAL_REFERENCE_CODE, "not json"));

		Assertions.assertEquals(
			HttpStatus.BAD_REQUEST, responseStatusException.getStatusCode());

		Mockito.verifyNoInteractions(_accountInvitationService);
	}

	@Test
	public void testPostInvitationsRejectsProjectFromAnotherAccount()
		throws Exception {

		AccountsRestController accountsRestController = _createController();

		Mockito.when(
			_accountService.getAccount(_EXTERNAL_REFERENCE_CODE, null)
		).thenReturn(
			_createAccount()
		);

		Mockito.when(
			_projectService.fetchProject(_PROJECT_EXTERNAL_REFERENCE_CODE)
		).thenReturn(
			_createProject("ACC-OTHER")
		);

		ResponseStatusException responseStatusException =
			Assertions.assertThrows(
				ResponseStatusException.class,
				() -> accountsRestController.postInvitations(
					null, _EXTERNAL_REFERENCE_CODE,
					_createProjectInvitationBodyJSON()));

		Assertions.assertEquals(
			HttpStatus.BAD_REQUEST, responseStatusException.getStatusCode());

		Mockito.verifyNoInteractions(_accountInvitationService);
	}

	@Test
	public void testPostInvitationsRejectsUnknownRoleExternalReferenceCode()
		throws Exception {

		AccountsRestController accountsRestController = _createController();

		Mockito.when(
			_accountService.getAccount(_EXTERNAL_REFERENCE_CODE, null)
		).thenReturn(
			_createAccount()
		);

		Mockito.when(
			_accountService.getAccountRoleNamesByExternalReferenceCode(
				_ACCOUNT_ID)
		).thenReturn(
			Map.of("L_ACCOUNT_ADMINISTRATOR", "Account Administrator")
		);

		ResponseStatusException responseStatusException =
			Assertions.assertThrows(
				ResponseStatusException.class,
				() -> accountsRestController.postInvitations(
					null, _EXTERNAL_REFERENCE_CODE,
					_createInvitationBodyJSON("C_NOT_A_ROLE")));

		Assertions.assertEquals(
			HttpStatus.BAD_REQUEST, responseStatusException.getStatusCode());

		Mockito.verifyNoInteractions(_accountInvitationService);
	}

	@Test
	public void testPostInvitationsResendChecksProjectPermission()
		throws Exception {

		AccountsRestController accountsRestController = _createController();

		Mockito.when(
			_accountInvitationService.fetchAccountInvitation(
				_ACCOUNT_INVITATION_ID)
		).thenReturn(
			_createAccountInvitation(
				false, _EXTERNAL_REFERENCE_CODE,
				_PROJECT_EXTERNAL_REFERENCE_CODE)
		);

		Mockito.when(
			_accountService.getAccount(_EXTERNAL_REFERENCE_CODE, null)
		).thenReturn(
			_createAccount()
		);

		Mockito.when(
			_userAccountService.getMyUserAccount(null)
		).thenReturn(
			_createInviterUserAccount()
		);

		Mockito.when(
			_projectService.fetchProject(_PROJECT_EXTERNAL_REFERENCE_CODE)
		).thenReturn(
			_createProject(_EXTERNAL_REFERENCE_CODE)
		);

		Mockito.when(
			_accountInvitationService.renewAccountInvitation(
				_ACCOUNT_INVITATION_ID)
		).thenReturn(
			_createAccountInvitation()
		);

		accountsRestController.postInvitationsResend(
			null, _EXTERNAL_REFERENCE_CODE, _ACCOUNT_INVITATION_ID);

		Mockito.verify(
			_projectMembershipPermission
		).check(
			ActionKeys.UPDATE, null, _PROJECT_EXTERNAL_REFERENCE_CODE
		);

		Mockito.verifyNoInteractions(_accountPermission);

		Mockito.verify(
			_accountInvitationEmailService
		).sendInvitationEmail(
			ArgumentMatchers.any(), ArgumentMatchers.any(),
			ArgumentMatchers.eq("Inviter Name"),
			ArgumentMatchers.eq("Project One")
		);
	}

	@Test
	public void testPostInvitationsResendRejectsAcceptedInvitation()
		throws Exception {

		AccountsRestController accountsRestController = _createController();

		Mockito.when(
			_accountInvitationService.fetchAccountInvitation(
				_ACCOUNT_INVITATION_ID)
		).thenReturn(
			_createAccountInvitation(
				true, _EXTERNAL_REFERENCE_CODE, StringPool.BLANK)
		);

		ResponseStatusException responseStatusException =
			Assertions.assertThrows(
				ResponseStatusException.class,
				() -> accountsRestController.postInvitationsResend(
					null, _EXTERNAL_REFERENCE_CODE, _ACCOUNT_INVITATION_ID));

		Assertions.assertEquals(
			HttpStatus.NOT_FOUND, responseStatusException.getStatusCode());

		Mockito.verify(
			_accountInvitationService, Mockito.never()
		).renewAccountInvitation(
			ArgumentMatchers.anyLong()
		);

		Mockito.verifyNoInteractions(_accountInvitationEmailService);
	}

	@Test
	public void testPostInvitationsResendRenewsTokenAndSendsEmail()
		throws Exception {

		AccountsRestController accountsRestController = _createController();

		Mockito.when(
			_accountInvitationService.fetchAccountInvitation(
				_ACCOUNT_INVITATION_ID)
		).thenReturn(
			_createAccountInvitation(
				false, _EXTERNAL_REFERENCE_CODE, StringPool.BLANK)
		);

		Account account = _createAccount();

		Mockito.when(
			_accountService.getAccount(_EXTERNAL_REFERENCE_CODE, null)
		).thenReturn(
			account
		);

		Mockito.when(
			_userAccountService.getMyUserAccount(null)
		).thenReturn(
			_createInviterUserAccount()
		);

		AccountInvitation renewedAccountInvitation = _createAccountInvitation();

		Mockito.when(
			_accountInvitationService.renewAccountInvitation(
				_ACCOUNT_INVITATION_ID)
		).thenReturn(
			renewedAccountInvitation
		);

		accountsRestController.postInvitationsResend(
			null, _EXTERNAL_REFERENCE_CODE, _ACCOUNT_INVITATION_ID);

		Mockito.verify(
			_accountPermission
		).check(
			_EXTERNAL_REFERENCE_CODE, ActionKeys.UPDATE, null
		);

		InOrder inOrder = Mockito.inOrder(
			_accountInvitationService, _accountInvitationEmailService);

		inOrder.verify(
			_accountInvitationService
		).renewAccountInvitation(
			_ACCOUNT_INVITATION_ID
		);

		inOrder.verify(
			_accountInvitationEmailService
		).sendInvitationEmail(
			account, renewedAccountInvitation, "Inviter Name", null
		);
	}

	@Test
	public void testPostInvitationsReusesPendingInvitation() throws Exception {
		AccountsRestController accountsRestController = _createController();

		Account account = _createAccount();

		Mockito.when(
			_accountService.getAccount(_EXTERNAL_REFERENCE_CODE, null)
		).thenReturn(
			account
		);

		Mockito.when(
			_userAccountService.getMyUserAccount(null)
		).thenReturn(
			_createInviterUserAccount()
		);

		AccountInvitation accountInvitation = _createAccountInvitation();

		Mockito.when(
			_accountInvitationService.fetchPendingAccountInvitation(
				_EXTERNAL_REFERENCE_CODE, _EMAIL_ADDRESS, "")
		).thenReturn(
			accountInvitation
		);

		Mockito.when(
			_accountInvitationService.updateAccountInvitation(
				_ACCOUNT_INVITATION_ID, "Doe", "Jane", "", List.of())
		).thenReturn(
			accountInvitation
		);

		accountsRestController.postInvitations(
			null, _EXTERNAL_REFERENCE_CODE, _createInvitationBodyJSON());

		Mockito.verify(
			_accountInvitationService
		).updateAccountInvitation(
			_ACCOUNT_INVITATION_ID, "Doe", "Jane", "", List.of()
		);

		Mockito.verify(
			_accountInvitationService, Mockito.never()
		).addAccountInvitation(
			ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
			ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
			ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
			ArgumentMatchers.anyList()
		);

		Mockito.verify(
			_accountInvitationEmailService
		).sendInvitationEmail(
			account, accountInvitation, "Inviter Name", null
		);
	}

	@Test
	public void testPostInvitationsStoresRoleExternalReferenceCodes()
		throws Exception {

		AccountsRestController accountsRestController = _createController();

		Mockito.when(
			_accountService.getAccount(_EXTERNAL_REFERENCE_CODE, null)
		).thenReturn(
			_createAccount()
		);

		Mockito.when(
			_userAccountService.getMyUserAccount(null)
		).thenReturn(
			_createInviterUserAccount()
		);

		Mockito.when(
			_accountService.getAccountRoleNamesByExternalReferenceCode(
				_ACCOUNT_ID)
		).thenReturn(
			Map.of("L_ACCOUNT_ADMINISTRATOR", "Account Administrator")
		);

		Mockito.when(
			_accountInvitationService.addAccountInvitation(
				_EXTERNAL_REFERENCE_CODE, _EMAIL_ADDRESS, "Doe", "Jane", "", "",
				List.of("L_ACCOUNT_ADMINISTRATOR"))
		).thenReturn(
			_createAccountInvitation()
		);

		accountsRestController.postInvitations(
			null, _EXTERNAL_REFERENCE_CODE,
			_createInvitationBodyJSON("L_ACCOUNT_ADMINISTRATOR"));

		Mockito.verify(
			_accountInvitationService
		).addAccountInvitation(
			_EXTERNAL_REFERENCE_CODE, _EMAIL_ADDRESS, "Doe", "Jane", "", "",
			List.of("L_ACCOUNT_ADMINISTRATOR")
		);
	}

	@Test
	public void testPostUserAccountsAccountRoleAssignsAccountRole()
		throws Exception {

		AccountsRestController accountsRestController = _createController();

		Account account = _createAccount();

		Mockito.when(
			_accountService.getAccount(_EXTERNAL_REFERENCE_CODE, null)
		).thenReturn(
			account
		);

		Mockito.when(
			_accountService.getAccountRoleName(_ACCOUNT_ID, _ACCOUNT_ROLE_ID)
		).thenReturn(
			"Support Administrator"
		);

		accountsRestController.postUserAccountsAccountRole(
			null, _EXTERNAL_REFERENCE_CODE, _USER_ID, _ACCOUNT_ROLE_ID);

		Mockito.verify(
			_accountService
		).addAccountUserAccountRole(
			_ACCOUNT_ROLE_ID, _EXTERNAL_REFERENCE_CODE, null, _USER_ID
		);

		Mockito.verify(
			_provisioningAssignmentService
		).assignAccountRole(
			account, _USER_ID, "Support Administrator"
		);
	}

	@Test
	public void testPostUserAccountsAccountRoleSyncsMembershipToJSM()
		throws Exception {

		AccountsRestController accountsRestController = _createController();

		Account account = _createAccount();

		Mockito.when(
			_accountService.getAccount(_EXTERNAL_REFERENCE_CODE, null)
		).thenReturn(
			account
		);

		UserAccount userAccount = _createUserAccount();

		Mockito.when(
			_userAccountService.getUserAccount(_USER_ID)
		).thenReturn(
			userAccount
		);

		accountsRestController.postUserAccountsAccountRole(
			null, _EXTERNAL_REFERENCE_CODE, _USER_ID, _ACCOUNT_ROLE_ID);

		Mockito.verify(
			_accountUserAccountSynchronizer
		).syncAccountUserAccountMembership(
			account, userAccount
		);
	}

	@Test
	public void testPostUserAccountsByEmailAddressAccountRolesAssignsCustomerGroup()
		throws Exception {

		AccountsRestController accountsRestController = _createController();

		Account account = _createAccount();

		Mockito.when(
			_accountService.getAccount(_EXTERNAL_REFERENCE_CODE, null)
		).thenReturn(
			account
		);

		Mockito.when(
			_userAccountService.fetchUserAccountByEmailAddress(_EMAIL_ADDRESS)
		).thenReturn(
			_createUserAccount()
		);

		Mockito.when(
			_oktaService.fetchContactByEmailAddress(_EMAIL_ADDRESS)
		).thenReturn(
			Mockito.mock(OktaUser.class)
		);

		accountsRestController.postUserAccountsByEmailAddressAccountRoles(
			null, _EXTERNAL_REFERENCE_CODE, _EMAIL_ADDRESS, "{}");

		Mockito.verify(
			_accountService
		).addAccountUserAccountByEmailAddress(
			_ACCOUNT_ID, _EMAIL_ADDRESS, null
		);

		Mockito.verify(
			_provisioningAssignmentService
		).assignCustomerGroup(
			_USER_ID
		);

		Mockito.verify(
			_provisioningEmailService
		).sendAssignedWelcomeEmail(
			account, _USER_ID
		);
	}

	@Test
	public void testPostUserAccountsByEmailAddressAccountRolesCreatesOktaUser()
		throws Exception {

		AccountsRestController accountsRestController = _createController();

		Account account = _createAccount();

		Mockito.when(
			_accountService.getAccount(_EXTERNAL_REFERENCE_CODE, null)
		).thenReturn(
			account
		);

		Mockito.when(
			_accountService.getAccountRoleNames(_ACCOUNT_ID)
		).thenReturn(
			Map.of(_ACCOUNT_ROLE_ID, "Support Administrator")
		);

		UserAccount userAccount = _createUserAccount();

		Mockito.when(
			_userAccountService.fetchUserAccountByEmailAddress(_EMAIL_ADDRESS)
		).thenReturn(
			null, userAccount
		);

		Mockito.when(
			_entitlementService.hasEntitlement(
				Mockito.eq(_ACCOUNT_ID), ArgumentMatchers.<String>any())
		).thenReturn(
			true
		);

		accountsRestController.postUserAccountsByEmailAddressAccountRoles(
			null, _EXTERNAL_REFERENCE_CODE, _EMAIL_ADDRESS,
			_createBodyJSON(_ACCOUNT_ROLE_ID, "Jane", "Doe"));

		Mockito.verify(
			_oktaService
		).createContact(
			_EMAIL_ADDRESS, "Jane", StringPool.BLANK, "Doe"
		);

		Mockito.verify(
			_accountService
		).addAccountUserAccountByEmailAddress(
			_ACCOUNT_ID, _EMAIL_ADDRESS, null
		);

		Mockito.verify(
			_accountService
		).addAccountUserAccountRole(
			_ACCOUNT_ROLE_ID, _EXTERNAL_REFERENCE_CODE, null, _USER_ID
		);

		Mockito.verify(
			_provisioningAssignmentService
		).assignAccountRole(
			account, _USER_ID, "Support Administrator"
		);

		Mockito.verify(
			_provisioningEmailService
		).sendAssignedWelcomeEmail(
			account, _USER_ID
		);
	}

	@Test
	public void testPostUserAccountsByEmailAddressAccountRolesRejectsAssignedRole()
		throws Exception {

		AccountsRestController accountsRestController = _createController();

		Mockito.when(
			_accountService.getAccount(_EXTERNAL_REFERENCE_CODE, null)
		).thenReturn(
			_createAccount()
		);

		Mockito.when(
			_accountService.getAccountRoleNames(_ACCOUNT_ID)
		).thenReturn(
			Map.of(_ACCOUNT_ROLE_ID, "Account Member")
		);

		Mockito.when(
			_userAccountService.fetchUserAccountByEmailAddress(_EMAIL_ADDRESS)
		).thenReturn(
			_createUserAccount(_ACCOUNT_ID, "Account Member")
		);

		ResponseStatusException responseStatusException =
			Assertions.assertThrows(
				ResponseStatusException.class,
				() ->
					accountsRestController.
						postUserAccountsByEmailAddressAccountRoles(
							null, _EXTERNAL_REFERENCE_CODE, _EMAIL_ADDRESS,
							_createBodyJSON(_ACCOUNT_ROLE_ID, null, null)));

		Assertions.assertEquals(
			HttpStatus.CONFLICT, responseStatusException.getStatusCode());

		Mockito.verifyNoInteractions(_oktaService);
	}

	@Test
	public void testPostUserAccountsByEmailAddressAccountRolesRejectsMalformedBody()
		throws Exception {

		AccountsRestController accountsRestController = _createController();

		Mockito.when(
			_accountService.getAccount(_EXTERNAL_REFERENCE_CODE, null)
		).thenReturn(
			_createAccount()
		);

		ResponseStatusException responseStatusException =
			Assertions.assertThrows(
				ResponseStatusException.class,
				() ->
					accountsRestController.
						postUserAccountsByEmailAddressAccountRoles(
							null, _EXTERNAL_REFERENCE_CODE, _EMAIL_ADDRESS,
							"not json"));

		Assertions.assertEquals(
			HttpStatus.BAD_REQUEST, responseStatusException.getStatusCode());

		Mockito.verifyNoInteractions(_oktaService);
	}

	@Test
	public void testPostUserAccountsByEmailAddressAccountRolesRejectsReservedDomain()
		throws Exception {

		AccountsRestController accountsRestController = _createController();

		Mockito.when(
			_emailAddressValidatorService.isLiferayDomain(_EMAIL_ADDRESS)
		).thenReturn(
			true
		);

		ResponseStatusException responseStatusException =
			Assertions.assertThrows(
				ResponseStatusException.class,
				() ->
					accountsRestController.
						postUserAccountsByEmailAddressAccountRoles(
							null, _EXTERNAL_REFERENCE_CODE, _EMAIL_ADDRESS,
							"{}"));

		Assertions.assertEquals(
			HttpStatus.BAD_REQUEST, responseStatusException.getStatusCode());

		Mockito.verifyNoInteractions(_accountService);
	}

	@Test
	public void testPostUserAccountsByEmailAddressAccountRolesRequiresEntitlement()
		throws Exception {

		AccountsRestController accountsRestController = _createController();

		Mockito.when(
			_accountService.getAccount(_EXTERNAL_REFERENCE_CODE, null)
		).thenReturn(
			_createAccount()
		);

		ResponseStatusException responseStatusException =
			Assertions.assertThrows(
				ResponseStatusException.class,
				() ->
					accountsRestController.
						postUserAccountsByEmailAddressAccountRoles(
							null, _EXTERNAL_REFERENCE_CODE, _EMAIL_ADDRESS,
							"{}"));

		Assertions.assertEquals(
			HttpStatus.UNPROCESSABLE_ENTITY,
			responseStatusException.getStatusCode());

		Mockito.verify(
			_oktaService, Mockito.never()
		).createContact(
			Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
			Mockito.anyString()
		);
	}

	@Test
	public void testPostUserAccountsByEmailAddressAccountRolesSkipsEmailForMember()
		throws Exception {

		AccountsRestController accountsRestController = _createController();

		Account account = _createAccount();

		Mockito.when(
			_accountService.getAccount(_EXTERNAL_REFERENCE_CODE, null)
		).thenReturn(
			account
		);

		Mockito.when(
			_accountService.getAccountRoleNames(_ACCOUNT_ID)
		).thenReturn(
			Map.of(_ACCOUNT_ROLE_ID, "Support Administrator")
		);

		Mockito.when(
			_userAccountService.fetchUserAccountByEmailAddress(_EMAIL_ADDRESS)
		).thenReturn(
			_createUserAccount(_ACCOUNT_ID, "Account Member")
		);

		Mockito.when(
			_oktaService.fetchContactByEmailAddress(_EMAIL_ADDRESS)
		).thenReturn(
			Mockito.mock(OktaUser.class)
		);

		accountsRestController.postUserAccountsByEmailAddressAccountRoles(
			null, _EXTERNAL_REFERENCE_CODE, _EMAIL_ADDRESS,
			_createBodyJSON(_ACCOUNT_ROLE_ID, null, null));

		Mockito.verify(
			_accountService, Mockito.never()
		).addAccountUserAccountByEmailAddress(
			Mockito.anyLong(), Mockito.anyString(), Mockito.any()
		);

		Mockito.verify(
			_provisioningAssignmentService
		).assignAccountRole(
			account, _USER_ID, "Support Administrator"
		);

		Mockito.verifyNoInteractions(_provisioningEmailService);
	}

	@Test
	public void testPostUserAccountsSendsWelcomeEmailForNewMember()
		throws Exception {

		AccountsRestController accountsRestController = _createController();

		Account account = _createAccount();

		Mockito.when(
			_accountService.getAccount(_EXTERNAL_REFERENCE_CODE, null)
		).thenReturn(
			account
		);

		accountsRestController.postUserAccounts(
			null, _EXTERNAL_REFERENCE_CODE, _USER_ID);

		Mockito.verify(
			_accountService
		).addAccountUserAccount(
			_ACCOUNT_ID, null, _USER_ID
		);

		Mockito.verify(
			_provisioningAssignmentService
		).assignCustomerGroup(
			_USER_ID
		);

		Mockito.verify(
			_provisioningEmailService
		).sendAssignedWelcomeEmail(
			account, _USER_ID
		);
	}

	@Test
	public void testPostUserAccountsSkipsWelcomeEmailForExistingMember()
		throws Exception {

		AccountsRestController accountsRestController = _createController();

		Mockito.when(
			_accountService.getAccount(_EXTERNAL_REFERENCE_CODE, null)
		).thenReturn(
			_createAccount()
		);

		Mockito.when(
			_userAccountService.hasAccountUserAccount(_ACCOUNT_ID, _USER_ID)
		).thenReturn(
			true
		);

		accountsRestController.postUserAccounts(
			null, _EXTERNAL_REFERENCE_CODE, _USER_ID);

		Mockito.verify(
			_provisioningAssignmentService
		).assignCustomerGroup(
			_USER_ID
		);

		Mockito.verifyNoInteractions(_provisioningEmailService);
	}

	@Test
	public void testPostUserAccountsSyncsMembershipToJSM() throws Exception {
		AccountsRestController accountsRestController = _createController();

		Account account = _createAccount();

		Mockito.when(
			_accountService.getAccount(_EXTERNAL_REFERENCE_CODE, null)
		).thenReturn(
			account
		);

		UserAccount userAccount = _createUserAccount();

		Mockito.when(
			_userAccountService.getUserAccount(_USER_ID)
		).thenReturn(
			userAccount
		);

		accountsRestController.postUserAccounts(
			null, _EXTERNAL_REFERENCE_CODE, _USER_ID);

		Mockito.verify(
			_accountUserAccountSynchronizer
		).syncAccountUserAccountMembership(
			account, userAccount
		);
	}

	private Account _createAccount() {
		Account account = new Account();

		account.setExternalReferenceCode(_EXTERNAL_REFERENCE_CODE);
		account.setId(_ACCOUNT_ID);

		return account;
	}

	private AccountInvitation _createAccountInvitation() {
		return new AccountInvitation(
			new JSONObject(
			).put(
				"emailAddress", _EMAIL_ADDRESS
			).put(
				"familyName", "Doe"
			).put(
				"givenName", "Jane"
			).put(
				"id", _ACCOUNT_INVITATION_ID
			));
	}

	private AccountInvitation _createAccountInvitation(
		boolean accepted, String accountExternalReferenceCode,
		String projectExternalReferenceCode,
		String... roleExternalReferenceCodes) {

		return new AccountInvitation(
			new JSONObject(
			).put(
				"accepted", accepted
			).put(
				"accountExternalReferenceCode", accountExternalReferenceCode
			).put(
				"emailAddress", _EMAIL_ADDRESS
			).put(
				"familyName", "Doe"
			).put(
				"givenName", "Jane"
			).put(
				"id", _ACCOUNT_INVITATION_ID
			).put(
				"projectExternalReferenceCode", projectExternalReferenceCode
			).put(
				"roleExternalReferenceCodes",
				new JSONArray(
					List.of(roleExternalReferenceCodes)
				).toString()
			));
	}

	private String _createBodyJSON(
		long accountRoleId, String firstName, String lastName) {

		JSONObject jsonObject = new JSONObject(
		).put(
			"accountRoleIds",
			new JSONArray(
			).put(
				accountRoleId
			)
		);

		if (firstName != null) {
			jsonObject.put("firstName", firstName);
		}

		if (lastName != null) {
			jsonObject.put("lastName", lastName);
		}

		return jsonObject.toString();
	}

	private AccountsRestController _createController() throws Exception {
		AccountsRestController accountsRestController =
			new AccountsRestController();

		Field field = OneBaseRestController.class.getDeclaredField(
			"_userAccountService");

		field.setAccessible(true);

		field.set(accountsRestController, _userAccountService);

		ReflectionTestUtils.setField(
			accountsRestController, "_accountAssetService",
			_accountAssetService);
		ReflectionTestUtils.setField(
			accountsRestController, "_accountInvitationEmailService",
			_accountInvitationEmailService);
		ReflectionTestUtils.setField(
			accountsRestController, "_accountInvitationService",
			_accountInvitationService);
		ReflectionTestUtils.setField(
			accountsRestController, "_accountPermission", _accountPermission);
		ReflectionTestUtils.setField(
			accountsRestController, "_accountService", _accountService);
		ReflectionTestUtils.setField(
			accountsRestController, "_accountSynchronizer",
			_accountSynchronizer);
		ReflectionTestUtils.setField(
			accountsRestController, "_accountUserAccountRoleSynchronizer",
			_accountUserAccountRoleSynchronizer);
		ReflectionTestUtils.setField(
			accountsRestController, "_accountUserAccountSynchronizer",
			_accountUserAccountSynchronizer);
		ReflectionTestUtils.setField(
			accountsRestController, "_emailAddressValidatorService",
			_emailAddressValidatorService);
		ReflectionTestUtils.setField(
			accountsRestController, "_entitlementService", _entitlementService);
		ReflectionTestUtils.setField(
			accountsRestController, "_keyedLock", new KeyedLock());
		ReflectionTestUtils.setField(
			accountsRestController, "_oktaService", _oktaService);
		ReflectionTestUtils.setField(
			accountsRestController, "_projectMembershipPermission",
			_projectMembershipPermission);
		ReflectionTestUtils.setField(
			accountsRestController, "_projectService", _projectService);
		ReflectionTestUtils.setField(
			accountsRestController, "_provisioningAssignmentService",
			_provisioningAssignmentService);
		ReflectionTestUtils.setField(
			accountsRestController, "_provisioningEmailService",
			_provisioningEmailService);
		ReflectionTestUtils.setField(
			accountsRestController, "_userAccountService", _userAccountService);

		return accountsRestController;
	}

	private String _createInvitationBodyJSON(
		String... roleExternalReferenceCodes) {

		return new JSONObject(
		).put(
			"emailAddress", _EMAIL_ADDRESS
		).put(
			"familyName", "Doe"
		).put(
			"givenName", "Jane"
		).put(
			"roleExternalReferenceCodes",
			new JSONArray(List.of(roleExternalReferenceCodes))
		).toString();
	}

	private UserAccount _createInviterUserAccount() {
		UserAccount userAccount = new UserAccount();

		userAccount.setName("Inviter Name");

		return userAccount;
	}

	private Project _createProject(String accountExternalReferenceCode) {
		return new Project(
			new JSONObject(
			).put(
				"externalReferenceCode", _PROJECT_EXTERNAL_REFERENCE_CODE
			).put(
				"name", "Project One"
			).put(
				"r_accountEntryToProject_accountEntryERC",
				accountExternalReferenceCode
			));
	}

	private String _createProjectInvitationBodyJSON() {
		return new JSONObject(
		).put(
			"emailAddress", _EMAIL_ADDRESS
		).put(
			"familyName", "Doe"
		).put(
			"givenName", "Jane"
		).put(
			"projectExternalReferenceCode", _PROJECT_EXTERNAL_REFERENCE_CODE
		).put(
			"projectRoleExternalReferenceCode", _PROJECT_ROLE_ERC
		).put(
			"roleExternalReferenceCodes", new JSONArray()
		).toString();
	}

	private UserAccount _createUserAccount() {
		UserAccount userAccount = new UserAccount();

		userAccount.setEmailAddress(_EMAIL_ADDRESS);
		userAccount.setId(_USER_ID);

		return userAccount;
	}

	private UserAccount _createUserAccount(long accountId, String roleName) {
		UserAccount userAccount = _createUserAccount();

		AccountBrief accountBrief = new AccountBrief();

		accountBrief.setId(accountId);

		RoleBrief roleBrief = new RoleBrief();

		roleBrief.setName(roleName);

		accountBrief.setRoleBriefs(new RoleBrief[] {roleBrief});

		userAccount.setAccountBriefs(new AccountBrief[] {accountBrief});

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

	private final AccountAssetService _accountAssetService = Mockito.mock(
		AccountAssetService.class);
	private final AccountInvitationEmailService _accountInvitationEmailService =
		Mockito.mock(AccountInvitationEmailService.class);
	private final AccountInvitationService _accountInvitationService =
		Mockito.mock(AccountInvitationService.class);
	private final AccountPermission _accountPermission = Mockito.mock(
		AccountPermission.class);
	private final AccountService _accountService = Mockito.mock(
		AccountService.class);
	private final AccountSynchronizer _accountSynchronizer = Mockito.mock(
		AccountSynchronizer.class);
	private final AccountUserAccountRoleSynchronizer
		_accountUserAccountRoleSynchronizer = Mockito.mock(
			AccountUserAccountRoleSynchronizer.class);
	private final AccountUserAccountSynchronizer
		_accountUserAccountSynchronizer = Mockito.mock(
			AccountUserAccountSynchronizer.class);
	private final EmailAddressValidatorService _emailAddressValidatorService =
		Mockito.mock(EmailAddressValidatorService.class);
	private final EntitlementService _entitlementService = Mockito.mock(
		EntitlementService.class);
	private final OktaService _oktaService = Mockito.mock(OktaService.class);
	private final ProjectMembershipPermission _projectMembershipPermission =
		Mockito.mock(ProjectMembershipPermission.class);
	private final ProjectService _projectService = Mockito.mock(
		ProjectService.class);
	private final ProvisioningAssignmentService _provisioningAssignmentService =
		Mockito.mock(ProvisioningAssignmentService.class);
	private final ProvisioningEmailService _provisioningEmailService =
		Mockito.mock(ProvisioningEmailService.class);
	private final UserAccountService _userAccountService = Mockito.mock(
		UserAccountService.class);

}