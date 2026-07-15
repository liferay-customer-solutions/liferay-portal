/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.permission;

import com.liferay.headless.admin.user.client.dto.v1_0.Account;
import com.liferay.headless.admin.user.client.dto.v1_0.AccountBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.OrganizationBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.RoleBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.one.constants.RoleConstants;
import com.liferay.one.service.AccountService;
import com.liferay.one.service.UserAccountService;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.security.permission.ActionKeys;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Wellington Barbosa
 */
public class LicenseKeyPermissionTest {

	@Test
	public void testCheckUpdateGrantsManageLicenseKeysAccountRoles()
		throws Exception {

		for (String roleName : RoleConstants.NAMES_MANAGE_LICENSE_KEYS) {
			LicenseKeyPermission licenseKeyPermission = _createPermission(
				_createUserAccount(
					new String[0],
					_createAccountBrief(_ACCOUNT_ID, new String[] {roleName})));

			licenseKeyPermission.check(_ACCOUNT_ID, ActionKeys.UPDATE, null);
		}
	}

	@Test
	public void testCheckUpdateGrantsProvisioningAdministrator()
		throws Exception {

		LicenseKeyPermission licenseKeyPermission = _createPermission(
			_createUserAccount(
				new String[] {RoleConstants.NAME_PROVISIONING_ADMINISTRATOR}));

		licenseKeyPermission.check(_ACCOUNT_ID, ActionKeys.UPDATE, null);
	}

	@Test
	public void testCheckUpdateThrowsForAccountMemberWithoutManageRole()
		throws Exception {

		LicenseKeyPermission licenseKeyPermission = _createPermission(
			_createUserAccount(
				new String[0],
				_createAccountBrief(
					_ACCOUNT_ID,
					new String[] {RoleConstants.NAME_ACCOUNT_MEMBER})));

		Assertions.assertThrows(
			PrincipalException.class,
			() -> licenseKeyPermission.check(
				_ACCOUNT_ID, ActionKeys.UPDATE, null));
	}

	@Test
	public void testCheckUpdateThrowsForLiferayStaff() throws Exception {
		LicenseKeyPermission licenseKeyPermission = _createPermission(
			_createUserAccount(
				new String[] {RoleConstants.NAME_LIFERAY_STAFF}));

		Assertions.assertThrows(
			PrincipalException.class,
			() -> licenseKeyPermission.check(
				_ACCOUNT_ID, ActionKeys.UPDATE, null));
	}

	@Test
	public void testCheckUpdateThrowsForManageRoleOnAnotherAccount()
		throws Exception {

		LicenseKeyPermission licenseKeyPermission = _createPermission(
			_createUserAccount(
				new String[0],
				_createAccountBrief(
					_ACCOUNT_ID + 1,
					new String[] {RoleConstants.NAME_SUPPORT_ADMINISTRATOR})));

		Assertions.assertThrows(
			PrincipalException.class,
			() -> licenseKeyPermission.check(
				_ACCOUNT_ID, ActionKeys.UPDATE, null));
	}

	@Test
	public void testCheckViewGrantsAccountMember() throws Exception {
		LicenseKeyPermission licenseKeyPermission = _createPermission(
			_createUserAccount(
				new String[0],
				_createAccountBrief(_ACCOUNT_ID, new String[0])));

		licenseKeyPermission.check(_ACCOUNT_ID, ActionKeys.VIEW, null);

		Mockito.verify(
			_accountService, Mockito.never()
		).fetchAccount(
			Mockito.anyLong()
		);
	}

	@Test
	public void testCheckViewGrantsAdministrator() throws Exception {
		LicenseKeyPermission licenseKeyPermission = _createPermission(
			_createUserAccount(
				new String[] {RoleConstants.NAME_ADMINISTRATOR}));

		licenseKeyPermission.check(_ACCOUNT_ID, ActionKeys.VIEW, null);
	}

	@Test
	public void testCheckViewGrantsLiferayStaff() throws Exception {
		LicenseKeyPermission licenseKeyPermission = _createPermission(
			_createUserAccount(
				new String[] {RoleConstants.NAME_LIFERAY_STAFF}));

		licenseKeyPermission.check(_ACCOUNT_ID, ActionKeys.VIEW, null);
	}

	@Test
	public void testCheckViewGrantsOrganizationMember() throws Exception {
		UserAccount userAccount = _createUserAccount(new String[0]);

		OrganizationBrief organizationBrief = Mockito.mock(
			OrganizationBrief.class);

		Mockito.when(
			organizationBrief.getId()
		).thenReturn(
			_ORGANIZATION_ID
		);

		Mockito.when(
			userAccount.getOrganizationBriefs()
		).thenReturn(
			new OrganizationBrief[] {organizationBrief}
		);

		LicenseKeyPermission licenseKeyPermission = _createPermission(
			userAccount);

		Account account = Mockito.mock(Account.class);

		Mockito.when(
			account.getOrganizationIds()
		).thenReturn(
			new Long[] {_ORGANIZATION_ID}
		);

		Mockito.when(
			_accountService.fetchAccount(_ACCOUNT_ID)
		).thenReturn(
			account
		);

		licenseKeyPermission.check(_ACCOUNT_ID, ActionKeys.VIEW, null);
	}

	@Test
	public void testCheckViewThrowsForNonmember() throws Exception {
		LicenseKeyPermission licenseKeyPermission = _createPermission(
			_createUserAccount(new String[0]));

		Account account = Mockito.mock(Account.class);

		Mockito.when(
			account.getOrganizationIds()
		).thenReturn(
			new Long[0]
		);

		Mockito.when(
			_accountService.fetchAccount(_ACCOUNT_ID)
		).thenReturn(
			account
		);

		Assertions.assertThrows(
			PrincipalException.class,
			() -> licenseKeyPermission.check(
				_ACCOUNT_ID, ActionKeys.VIEW, null));
	}

	@Test
	public void testCheckViewThrowsWhenAccountMissing() throws Exception {
		LicenseKeyPermission licenseKeyPermission = _createPermission(
			_createUserAccount(new String[0]));

		Assertions.assertThrows(
			PrincipalException.class,
			() -> licenseKeyPermission.check(
				_ACCOUNT_ID, ActionKeys.VIEW, null));
	}

	private AccountBrief _createAccountBrief(
		long accountId, String[] roleNames) {

		AccountBrief accountBrief = Mockito.mock(AccountBrief.class);

		Mockito.when(
			accountBrief.getId()
		).thenReturn(
			accountId
		);

		RoleBrief[] roleBriefs = new RoleBrief[roleNames.length];

		for (int i = 0; i < roleNames.length; i++) {
			RoleBrief roleBrief = Mockito.mock(RoleBrief.class);

			Mockito.when(
				roleBrief.getName()
			).thenReturn(
				roleNames[i]
			);

			roleBriefs[i] = roleBrief;
		}

		Mockito.when(
			accountBrief.getRoleBriefs()
		).thenReturn(
			roleBriefs
		);

		return accountBrief;
	}

	private LicenseKeyPermission _createPermission(UserAccount userAccount)
		throws Exception {

		LicenseKeyPermission licenseKeyPermission = new LicenseKeyPermission();

		UserAccountService userAccountService = Mockito.mock(
			UserAccountService.class);

		Mockito.when(
			userAccountService.getMyUserAccount(Mockito.any())
		).thenReturn(
			userAccount
		);

		ReflectionTestUtils.setField(
			licenseKeyPermission, "_accountService", _accountService);

		ReflectionTestUtils.setField(
			licenseKeyPermission, "_userAccountService", userAccountService);

		return licenseKeyPermission;
	}

	private UserAccount _createUserAccount(
		String[] roleNames, AccountBrief... accountBriefs) {

		UserAccount userAccount = Mockito.mock(UserAccount.class);

		Mockito.when(
			userAccount.getAccountBriefs()
		).thenReturn(
			accountBriefs
		);

		Mockito.when(
			userAccount.getOrganizationBriefs()
		).thenReturn(
			new OrganizationBrief[0]
		);

		RoleBrief[] roleBriefs = new RoleBrief[roleNames.length];

		for (int i = 0; i < roleNames.length; i++) {
			RoleBrief roleBrief = Mockito.mock(RoleBrief.class);

			Mockito.when(
				roleBrief.getName()
			).thenReturn(
				roleNames[i]
			);

			roleBriefs[i] = roleBrief;
		}

		Mockito.when(
			userAccount.getRoleBriefs()
		).thenReturn(
			roleBriefs
		);

		return userAccount;
	}

	private static final long _ACCOUNT_ID = 555L;

	private static final long _ORGANIZATION_ID = 77L;

	private final AccountService _accountService = Mockito.mock(
		AccountService.class);

}