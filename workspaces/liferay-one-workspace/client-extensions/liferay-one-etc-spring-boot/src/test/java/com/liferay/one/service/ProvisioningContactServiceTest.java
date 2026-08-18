/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.admin.user.client.dto.v1_0.Account;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.one.constants.RoleConstants;
import com.liferay.one.okta.service.OktaService;
import com.liferay.one.salesforce.model.SalesforceModelTestUtil;
import com.liferay.one.salesforce.model.SalesforceProject;
import com.liferay.one.salesforce.model.SalesforceProjectContactRole;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Felipe Franca
 */
public class ProvisioningContactServiceTest {

	@BeforeEach
	public void setUp() throws Exception {
		_provisioningContactService = new ProvisioningContactService();

		_accountService = Mockito.mock(AccountService.class);
		_emailAddressValidatorService = Mockito.mock(
			EmailAddressValidatorService.class);
		_oktaService = Mockito.mock(OktaService.class);
		_projectMembershipService = Mockito.mock(
			ProjectMembershipService.class);
		_provisioningAssignmentService = Mockito.mock(
			ProvisioningAssignmentService.class);
		_userAccountService = Mockito.mock(UserAccountService.class);

		_account = new Account();

		_account.setId(_ACCOUNT_ID);

		Mockito.when(
			_userAccountService.hasUserAccounts(_ACCOUNT_ID)
		).thenReturn(
			true
		);

		ReflectionTestUtils.setField(
			_provisioningContactService, "_accountService", _accountService);
		ReflectionTestUtils.setField(
			_provisioningContactService, "_emailAddressValidatorService",
			_emailAddressValidatorService);
		ReflectionTestUtils.setField(
			_provisioningContactService, "_oktaService", _oktaService);
		ReflectionTestUtils.setField(
			_provisioningContactService, "_projectMembershipService",
			_projectMembershipService);
		ReflectionTestUtils.setField(
			_provisioningContactService, "_provisioningAssignmentService",
			_provisioningAssignmentService);
		ReflectionTestUtils.setField(
			_provisioningContactService, "_userAccountService",
			_userAccountService);
	}

	@Test
	public void testAddProjectContactsAddsProjectMembershipWhenProjectPresent()
		throws Exception {

		UserAccount newUserAccount = new UserAccount();

		newUserAccount.setId(_USER_ID);

		Mockito.when(
			_userAccountService.addUserAccount(
				_EMAIL_ADDRESS, _LAST_NAME, _FIRST_NAME)
		).thenReturn(
			newUserAccount
		);

		Mockito.when(
			_accountService.fetchAccountRoleId(_ACCOUNT_ID, _CONTACT_ROLE)
		).thenReturn(
			_ACCOUNT_ROLE_ID
		);

		SalesforceProject salesforceProject = new SalesforceProject(
			new JSONObject(
			).put(
				"Id", "SF-PROJ-1"
			));

		_provisioningContactService.addProjectContacts(
			_account, List.of(_createContactRole(_CONTACT_ROLE)),
			salesforceProject, new ArrayList<>());

		Mockito.verify(
			_projectMembershipService
		).addProjectMembership(
			"SF-PROJ-1", _USER_ID
		);
	}

	@Test
	public void testAddProjectContactsCreatesNewContact() throws Exception {
		UserAccount newUserAccount = new UserAccount();

		newUserAccount.setId(_USER_ID);

		Mockito.when(
			_userAccountService.addUserAccount(
				_EMAIL_ADDRESS, _LAST_NAME, _FIRST_NAME)
		).thenReturn(
			newUserAccount
		);

		Mockito.when(
			_accountService.fetchAccountRoleId(_ACCOUNT_ID, _CONTACT_ROLE)
		).thenReturn(
			_ACCOUNT_ROLE_ID
		);

		List<String> warningMessages = new ArrayList<>();

		List<Long> userIds = _provisioningContactService.addProjectContacts(
			_account, List.of(_createContactRole(_CONTACT_ROLE)), null,
			warningMessages);

		Mockito.verify(
			_userAccountService
		).addUserAccount(
			_EMAIL_ADDRESS, _LAST_NAME, _FIRST_NAME
		);

		Mockito.verify(
			_oktaService
		).createContact(
			_EMAIL_ADDRESS, _FIRST_NAME, null, _LAST_NAME
		);

		Mockito.verify(
			_accountService
		).addAccountUserAccount(
			_ACCOUNT_ID, _ACCOUNT_ROLE_ID, _USER_ID
		);

		Mockito.verify(
			_provisioningAssignmentService
		).assignAccountRole(
			_account, _USER_ID, _CONTACT_ROLE
		);

		Assertions.assertEquals(List.of(_USER_ID), userIds);
	}

	@Test
	public void testAddProjectContactsIsolatesPerContactFailures()
		throws Exception {

		Mockito.when(
			_userAccountService.fetchUserAccountByEmailAddress(_EMAIL_ADDRESS)
		).thenThrow(
			new RuntimeException("Unable to fetch user account")
		);

		UserAccount secondUserAccount = new UserAccount();

		secondUserAccount.setId(_SECOND_USER_ID);

		Mockito.when(
			_userAccountService.fetchUserAccountByEmailAddress(
				_SECOND_EMAIL_ADDRESS)
		).thenReturn(
			secondUserAccount
		);

		Mockito.when(
			_accountService.fetchAccountRoleId(_ACCOUNT_ID, _CONTACT_ROLE)
		).thenReturn(
			_ACCOUNT_ROLE_ID
		);

		List<String> warningMessages = new ArrayList<>();

		List<Long> userIds = _provisioningContactService.addProjectContacts(
			_account,
			List.of(
				_createContactRole(_CONTACT_ROLE),
				_createContactRole(
					_CONTACT_ROLE, _SECOND_EMAIL_ADDRESS, _FIRST_NAME,
					_LAST_NAME)),
			null, warningMessages);

		Assertions.assertEquals(List.of(_SECOND_USER_ID), userIds);

		Assertions.assertEquals(1, warningMessages.size());
	}

	@Test
	public void testAddProjectContactsPromotesFirstUserToAdministrator()
		throws Exception {

		Mockito.when(
			_userAccountService.hasUserAccounts(_ACCOUNT_ID)
		).thenReturn(
			false
		);

		UserAccount newUserAccount = new UserAccount();

		newUserAccount.setId(_USER_ID);

		Mockito.when(
			_userAccountService.addUserAccount(
				_EMAIL_ADDRESS, _LAST_NAME, _FIRST_NAME)
		).thenReturn(
			newUserAccount
		);

		Mockito.when(
			_accountService.fetchAccountRoleId(_ACCOUNT_ID, _CONTACT_ROLE)
		).thenReturn(
			_ACCOUNT_ROLE_ID
		);

		Mockito.when(
			_accountService.fetchAccountRoleId(
				_ACCOUNT_ID, RoleConstants.NAME_ACCOUNT_ADMINISTRATOR)
		).thenReturn(
			_ADMINISTRATOR_ROLE_ID
		);

		_provisioningContactService.addProjectContacts(
			_account, List.of(_createContactRole(_CONTACT_ROLE)), null,
			new ArrayList<>());

		Mockito.verify(
			_accountService
		).addAccountUserAccountRole(
			_ACCOUNT_ID, _ADMINISTRATOR_ROLE_ID, _USER_ID
		);
	}

	@Test
	public void testAddProjectContactsSkipsAutoPromotionWhenDesignatedAdministratorPresent()
		throws Exception {

		Mockito.when(
			_userAccountService.hasUserAccounts(_ACCOUNT_ID)
		).thenReturn(
			false
		);

		Mockito.when(
			_accountService.fetchAccountRoleId(_ACCOUNT_ID, _CONTACT_ROLE)
		).thenReturn(
			_ACCOUNT_ROLE_ID
		);

		UserAccount firstUserAccount = new UserAccount();

		firstUserAccount.setId(_USER_ID);

		Mockito.when(
			_userAccountService.addUserAccount(
				_EMAIL_ADDRESS, _LAST_NAME, _FIRST_NAME)
		).thenReturn(
			firstUserAccount
		);

		Mockito.when(
			_accountService.fetchAccountRoleId(
				_ACCOUNT_ID, RoleConstants.NAME_ACCOUNT_ADMINISTRATOR)
		).thenReturn(
			_ADMINISTRATOR_ROLE_ID
		);

		UserAccount secondUserAccount = new UserAccount();

		secondUserAccount.setId(_SECOND_USER_ID);

		Mockito.when(
			_userAccountService.addUserAccount(
				_SECOND_EMAIL_ADDRESS, _LAST_NAME, _FIRST_NAME)
		).thenReturn(
			secondUserAccount
		);

		_provisioningContactService.addProjectContacts(
			_account,
			List.of(
				_createContactRole(RoleConstants.NAME_ACCOUNT_ADMINISTRATOR),
				_createContactRole(
					_CONTACT_ROLE, _SECOND_EMAIL_ADDRESS, _FIRST_NAME,
					_LAST_NAME)),
			null, new ArrayList<>());

		Mockito.verify(
			_accountService, Mockito.never()
		).addAccountUserAccountRole(
			_ACCOUNT_ID, _ADMINISTRATOR_ROLE_ID, _SECOND_USER_ID
		);
	}

	@Test
	public void testAddProjectContactsSkipsExistingMember() throws Exception {
		UserAccount existingUserAccount = new UserAccount();

		existingUserAccount.setId(_USER_ID);

		Mockito.when(
			_userAccountService.fetchUserAccountByEmailAddress(_EMAIL_ADDRESS)
		).thenReturn(
			existingUserAccount
		);

		Mockito.when(
			_userAccountService.hasAccountUserAccount(_ACCOUNT_ID, _USER_ID)
		).thenReturn(
			true
		);

		List<Long> userIds = _provisioningContactService.addProjectContacts(
			_account, List.of(_createContactRole(_CONTACT_ROLE)), null,
			new ArrayList<>());

		Assertions.assertTrue(userIds.isEmpty());

		Mockito.verify(
			_provisioningAssignmentService, Mockito.never()
		).assignAccountRole(
			Mockito.any(), Mockito.anyLong(), Mockito.any()
		);
	}

	@Test
	public void testAddProjectContactsSkipsLiferayDomainEmail()
		throws Exception {

		Mockito.when(
			_emailAddressValidatorService.isLiferayDomain(_EMAIL_ADDRESS)
		).thenReturn(
			true
		);

		List<Long> userIds = _provisioningContactService.addProjectContacts(
			_account, List.of(_createContactRole(_CONTACT_ROLE)), null,
			new ArrayList<>());

		Assertions.assertTrue(userIds.isEmpty());

		Mockito.verifyNoInteractions(_oktaService);
	}

	@Test
	public void testAddProjectContactsSwallowsAssignmentSideEffectFailure()
		throws Exception {

		UserAccount newUserAccount = new UserAccount();

		newUserAccount.setId(_USER_ID);

		Mockito.when(
			_userAccountService.addUserAccount(
				_EMAIL_ADDRESS, _LAST_NAME, _FIRST_NAME)
		).thenReturn(
			newUserAccount
		);

		Mockito.when(
			_accountService.fetchAccountRoleId(_ACCOUNT_ID, _CONTACT_ROLE)
		).thenReturn(
			_ACCOUNT_ROLE_ID
		);

		Mockito.doThrow(
			new RuntimeException("Unable to assign account role")
		).when(
			_provisioningAssignmentService
		).assignAccountRole(
			Mockito.any(), Mockito.anyLong(), Mockito.any()
		);

		List<Long> userIds = _provisioningContactService.addProjectContacts(
			_account, List.of(_createContactRole(_CONTACT_ROLE)), null,
			new ArrayList<>());

		Assertions.assertEquals(List.of(_USER_ID), userIds);
	}

	@Test
	public void testAddProjectContactsSwallowsOktaContactCreationFailure()
		throws Exception {

		UserAccount newUserAccount = new UserAccount();

		newUserAccount.setId(_USER_ID);

		Mockito.when(
			_userAccountService.addUserAccount(
				_EMAIL_ADDRESS, _LAST_NAME, _FIRST_NAME)
		).thenReturn(
			newUserAccount
		);

		Mockito.when(
			_accountService.fetchAccountRoleId(_ACCOUNT_ID, _CONTACT_ROLE)
		).thenReturn(
			_ACCOUNT_ROLE_ID
		);

		Mockito.doThrow(
			new RuntimeException("Unable to create Okta contact")
		).when(
			_oktaService
		).createContact(
			_EMAIL_ADDRESS, _FIRST_NAME, null, _LAST_NAME
		);

		List<Long> userIds = _provisioningContactService.addProjectContacts(
			_account, List.of(_createContactRole(_CONTACT_ROLE)), null,
			new ArrayList<>());

		Assertions.assertEquals(List.of(_USER_ID), userIds);

		Mockito.verify(
			_accountService
		).addAccountUserAccount(
			_ACCOUNT_ID, _ACCOUNT_ROLE_ID, _USER_ID
		);
	}

	@Test
	public void testAddProjectContactsWarnsOnUnknownContactRole()
		throws Exception {

		UserAccount newUserAccount = new UserAccount();

		newUserAccount.setId(_USER_ID);

		Mockito.when(
			_userAccountService.addUserAccount(
				_EMAIL_ADDRESS, _LAST_NAME, _FIRST_NAME)
		).thenReturn(
			newUserAccount
		);

		Mockito.when(
			_accountService.fetchAccountRoleId(_ACCOUNT_ID, _CONTACT_ROLE)
		).thenReturn(
			null
		);

		List<String> warningMessages = new ArrayList<>();

		_provisioningContactService.addProjectContacts(
			_account, List.of(_createContactRole(_CONTACT_ROLE)), null,
			warningMessages);

		Assertions.assertEquals(1, warningMessages.size());
		Assertions.assertTrue(
			warningMessages.get(
				0
			).contains(
				"Unable to find account role"
			));

		Mockito.verify(
			_accountService
		).addAccountUserAccount(
			_ACCOUNT_ID, _USER_ID
		);

		Mockito.verify(
			_provisioningAssignmentService, Mockito.never()
		).assignAccountRole(
			Mockito.any(), Mockito.anyLong(), Mockito.any()
		);
	}

	@Test
	public void testAddProjectContactsWarnsWhenAdministratorRoleIsMissing()
		throws Exception {

		Mockito.when(
			_userAccountService.hasUserAccounts(_ACCOUNT_ID)
		).thenReturn(
			false
		);

		UserAccount newUserAccount = new UserAccount();

		newUserAccount.setId(_USER_ID);

		Mockito.when(
			_userAccountService.addUserAccount(
				_EMAIL_ADDRESS, _LAST_NAME, _FIRST_NAME)
		).thenReturn(
			newUserAccount
		);

		Mockito.when(
			_accountService.fetchAccountRoleId(_ACCOUNT_ID, _CONTACT_ROLE)
		).thenReturn(
			_ACCOUNT_ROLE_ID
		);

		Mockito.when(
			_accountService.fetchAccountRoleId(
				_ACCOUNT_ID, RoleConstants.NAME_ACCOUNT_ADMINISTRATOR)
		).thenReturn(
			null
		);

		List<String> warningMessages = new ArrayList<>();

		_provisioningContactService.addProjectContacts(
			_account, List.of(_createContactRole(_CONTACT_ROLE)), null,
			warningMessages);

		Assertions.assertEquals(1, warningMessages.size());
		Assertions.assertTrue(
			warningMessages.get(
				0
			).contains(
				"Unable to find account role " +
					RoleConstants.NAME_ACCOUNT_ADMINISTRATOR
			));

		Mockito.verify(
			_accountService, Mockito.never()
		).addAccountUserAccountRole(
			Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong()
		);
	}

	private SalesforceProjectContactRole _createContactRole(
		String contactRole) {

		return _createContactRole(
			contactRole, _EMAIL_ADDRESS, _FIRST_NAME, _LAST_NAME);
	}

	private SalesforceProjectContactRole _createContactRole(
		String contactRole, String emailAddress, String firstName,
		String lastName) {

		JSONObject jsonObject =
			SalesforceModelTestUtil.createProjectContactRoleJSONObject(
				contactRole, emailAddress, firstName, lastName, _PROJECT_ID);

		return new SalesforceProjectContactRole(jsonObject);
	}

	private static final long _ACCOUNT_ID = 1000L;

	private static final long _ACCOUNT_ROLE_ID = 500L;

	private static final long _ADMINISTRATOR_ROLE_ID = 600L;

	private static final String _CONTACT_ROLE = "Member";

	private static final String _EMAIL_ADDRESS = "contact@example.com";

	private static final String _FIRST_NAME = "Jane";

	private static final String _LAST_NAME = "Doe";

	private static final String _PROJECT_ID = "PROJECT-1";

	private static final String _SECOND_EMAIL_ADDRESS =
		"other-contact@example.com";

	private static final long _SECOND_USER_ID = 200L;

	private static final long _USER_ID = 100L;

	private Account _account;
	private AccountService _accountService;
	private EmailAddressValidatorService _emailAddressValidatorService;
	private OktaService _oktaService;
	private ProjectMembershipService _projectMembershipService;
	private ProvisioningAssignmentService _provisioningAssignmentService;
	private ProvisioningContactService _provisioningContactService;
	private UserAccountService _userAccountService;

}