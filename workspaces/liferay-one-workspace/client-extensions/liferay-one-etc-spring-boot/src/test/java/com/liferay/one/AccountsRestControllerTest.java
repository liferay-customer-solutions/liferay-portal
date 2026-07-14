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
import com.liferay.one.okta.model.OktaUser;
import com.liferay.one.okta.service.OktaService;
import com.liferay.one.permission.AccountPermission;
import com.liferay.one.service.AccountService;
import com.liferay.one.service.EmailAddressValidatorService;
import com.liferay.one.service.EntitlementService;
import com.liferay.one.service.ProvisioningAssignmentService;
import com.liferay.one.service.ProvisioningEmailService;
import com.liferay.one.service.UserAccountService;
import com.liferay.petra.string.StringPool;

import org.json.JSONArray;
import org.json.JSONObject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.Mockito;

import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author Ricardo Mariz
 */
public class AccountsRestControllerTest {

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
			_accountService.getAccountRoleName(_ACCOUNT_ID, _ACCOUNT_ROLE_ID)
		).thenReturn(
			"Support Administrator"
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
			_USER_ID, account
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
			_accountService.getAccountRoleName(_ACCOUNT_ID, _ACCOUNT_ROLE_ID)
		).thenReturn(
			"Account Member"
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
			_accountService.getAccountRoleName(_ACCOUNT_ID, _ACCOUNT_ROLE_ID)
		).thenReturn(
			"Support Administrator"
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
			_USER_ID, account
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

	private Account _createAccount() {
		Account account = new Account();

		account.setId(_ACCOUNT_ID);

		return account;
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

	private AccountsRestController _createController() {
		AccountsRestController accountsRestController =
			new AccountsRestController();

		ReflectionTestUtils.setField(
			accountsRestController, "_accountAssetService",
			_accountAssetService);
		ReflectionTestUtils.setField(
			accountsRestController, "_accountPermission", _accountPermission);
		ReflectionTestUtils.setField(
			accountsRestController, "_accountService", _accountService);
		ReflectionTestUtils.setField(
			accountsRestController, "_emailAddressValidatorService",
			_emailAddressValidatorService);
		ReflectionTestUtils.setField(
			accountsRestController, "_entitlementService", _entitlementService);
		ReflectionTestUtils.setField(
			accountsRestController, "_oktaService", _oktaService);
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

	private static final long _ACCOUNT_ROLE_ID = 33333;

	private static final String _EMAIL_ADDRESS = "jane@example.com";

	private static final String _EXTERNAL_REFERENCE_CODE = "ACC-1";

	private static final long _USER_ID = 22222;

	private final AccountAssetService _accountAssetService = Mockito.mock(
		AccountAssetService.class);
	private final AccountPermission _accountPermission = Mockito.mock(
		AccountPermission.class);
	private final AccountService _accountService = Mockito.mock(
		AccountService.class);
	private final EmailAddressValidatorService _emailAddressValidatorService =
		Mockito.mock(EmailAddressValidatorService.class);
	private final EntitlementService _entitlementService = Mockito.mock(
		EntitlementService.class);
	private final OktaService _oktaService = Mockito.mock(OktaService.class);
	private final ProvisioningAssignmentService _provisioningAssignmentService =
		Mockito.mock(ProvisioningAssignmentService.class);
	private final ProvisioningEmailService _provisioningEmailService =
		Mockito.mock(ProvisioningEmailService.class);
	private final UserAccountService _userAccountService = Mockito.mock(
		UserAccountService.class);

}