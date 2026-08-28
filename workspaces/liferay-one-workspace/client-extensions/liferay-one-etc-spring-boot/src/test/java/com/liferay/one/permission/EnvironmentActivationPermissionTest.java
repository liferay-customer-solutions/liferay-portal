/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.permission;

import com.liferay.headless.admin.user.client.dto.v1_0.AccountBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.RoleBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.one.constants.RoleConstants;
import com.liferay.one.model.Project;
import com.liferay.one.model.ProjectMembership;
import com.liferay.one.service.ProjectMembershipService;
import com.liferay.one.service.ProjectService;
import com.liferay.one.service.UserAccountService;
import com.liferay.portal.kernel.security.auth.PrincipalException;

import java.util.Objects;

import org.json.JSONObject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Felipe Franca
 */
public class EnvironmentActivationPermissionTest {

	@Test
	public void testCheckGrantsAccountAdministrator() throws Exception {
		Project project = _createProject(_ACCOUNT_ERC);

		EnvironmentActivationPermission environmentActivationPermission =
			_createPermission(
				_createUserAccount(
					new String[0], _ACCOUNT_ERC,
					new String[] {RoleConstants.NAME_ACCOUNT_ADMINISTRATOR}),
				project, null);

		Assertions.assertSame(
			project, environmentActivationPermission.check(null, _PROJECT_ERC));

		Mockito.verify(
			_projectService, Mockito.times(1)
		).fetchProject(
			_PROJECT_ERC
		);
	}

	@Test
	public void testCheckGrantsAccountAdministratorWithNullRoleBriefs()
		throws Exception {

		Project project = _createProject(_ACCOUNT_ERC);

		EnvironmentActivationPermission environmentActivationPermission =
			_createPermission(
				_createUserAccount(
					null, _ACCOUNT_ERC,
					new String[] {RoleConstants.NAME_ACCOUNT_ADMINISTRATOR}),
				project, null);

		Assertions.assertSame(
			project, environmentActivationPermission.check(null, _PROJECT_ERC));
	}

	@Test
	public void testCheckGrantsAdministrator() throws Exception {
		EnvironmentActivationPermission environmentActivationPermission =
			_createPermission(
				_createUserAccount(
					new String[] {RoleConstants.NAME_ADMINISTRATOR}, null,
					new String[0]),
				null, null);

		Assertions.assertNull(
			environmentActivationPermission.check(null, _PROJECT_ERC));
	}

	@Test
	public void testCheckGrantsLiferayStaff() throws Exception {
		EnvironmentActivationPermission environmentActivationPermission =
			_createPermission(
				_createUserAccount(
					new String[] {RoleConstants.NAME_LIFERAY_STAFF}, null,
					new String[0]),
				null, null);

		environmentActivationPermission.check(null, _PROJECT_ERC);
	}

	@Test
	public void testCheckGrantsProjectAdminMembership() throws Exception {
		EnvironmentActivationPermission environmentActivationPermission =
			_createPermission(
				_createUserAccount(new String[0], null, new String[0]),
				_createProject(_ACCOUNT_ERC),
				_createProjectMembership(RoleConstants.ERC_PROJECT_ADMIN));

		environmentActivationPermission.check(null, _PROJECT_ERC);
	}

	@Test
	public void testCheckGrantsProjectAdminMembershipWithNullAccountRoleBriefs()
		throws Exception {

		Project project = _createProject(_ACCOUNT_ERC);

		EnvironmentActivationPermission environmentActivationPermission =
			_createPermission(
				_createUserAccount(new String[0], _ACCOUNT_ERC, null), project,
				_createProjectMembership(RoleConstants.ERC_PROJECT_ADMIN));

		Assertions.assertSame(
			project, environmentActivationPermission.check(null, _PROJECT_ERC));
	}

	@Test
	public void testCheckThrowsForNullAccountBriefs() throws Exception {
		UserAccount userAccount = _createUserAccount(
			new String[0], null, new String[0]);

		Mockito.when(
			userAccount.getAccountBriefs()
		).thenReturn(
			null
		);

		EnvironmentActivationPermission environmentActivationPermission =
			_createPermission(userAccount, _createProject(_ACCOUNT_ERC), null);

		Assertions.assertThrows(
			PrincipalException.class,
			() -> environmentActivationPermission.check(null, _PROJECT_ERC));
	}

	@Test
	public void testCheckThrowsForNullProject() throws Exception {
		EnvironmentActivationPermission environmentActivationPermission =
			_createPermission(
				_createUserAccount(new String[0], null, new String[0]), null,
				null);

		Assertions.assertThrows(
			PrincipalException.class,
			() -> environmentActivationPermission.check(null, _PROJECT_ERC));
	}

	@Test
	public void testCheckThrowsForRequesterMembership() throws Exception {
		EnvironmentActivationPermission environmentActivationPermission =
			_createPermission(
				_createUserAccount(new String[0], null, new String[0]),
				_createProject(_ACCOUNT_ERC),
				_createProjectMembership(RoleConstants.ERC_PROJECT_REQUESTER));

		Assertions.assertThrows(
			PrincipalException.class,
			() -> environmentActivationPermission.check(null, _PROJECT_ERC));
	}

	private EnvironmentActivationPermission _createPermission(
			UserAccount userAccount, Project project,
			ProjectMembership projectMembership)
		throws Exception {

		EnvironmentActivationPermission environmentActivationPermission =
			new EnvironmentActivationPermission();

		UserAccountService userAccountService = Mockito.mock(
			UserAccountService.class);

		Mockito.when(
			userAccountService.getMyUserAccount(Mockito.any())
		).thenReturn(
			userAccount
		);

		Mockito.when(
			_projectService.fetchProject(_PROJECT_ERC)
		).thenReturn(
			project
		);

		ProjectMembershipService projectMembershipService = Mockito.mock(
			ProjectMembershipService.class);

		ProjectMembership projectAdminMembership = null;

		if ((projectMembership != null) &&
			Objects.equals(
				RoleConstants.ERC_PROJECT_ADMIN,
				projectMembership.getRoleExternalReferenceCode())) {

			projectAdminMembership = projectMembership;
		}

		Mockito.when(
			projectMembershipService.fetchProjectMembership(
				_PROJECT_ERC, RoleConstants.ERC_PROJECT_ADMIN,
				userAccount.getId())
		).thenReturn(
			projectAdminMembership
		);

		ReflectionTestUtils.setField(
			environmentActivationPermission, "_projectMembershipService",
			projectMembershipService);

		ReflectionTestUtils.setField(
			environmentActivationPermission, "_projectService",
			_projectService);

		ReflectionTestUtils.setField(
			environmentActivationPermission, "_userAccountService",
			userAccountService);

		return environmentActivationPermission;
	}

	private Project _createProject(String accountExternalReferenceCode) {
		return new Project(
			new JSONObject(
			).put(
				"externalReferenceCode", _PROJECT_ERC
			).put(
				"r_accountEntryToProject_accountEntryERC",
				accountExternalReferenceCode
			));
	}

	private ProjectMembership _createProjectMembership(
		String roleExternalReferenceCode) {

		return new ProjectMembership(
			new JSONObject(
			).put(
				"r_projectToProjectMembership_c_projectERC", _PROJECT_ERC
			).put(
				"r_userToProjectMembership_userId", _USER_ID
			).put(
				"roleExternalReferenceCode", roleExternalReferenceCode
			));
	}

	private RoleBrief[] _createRoleBriefs(String[] roleNames) {
		if (roleNames == null) {
			return null;
		}

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

		return roleBriefs;
	}

	private UserAccount _createUserAccount(
		String[] globalRoleNames, String accountExternalReferenceCode,
		String[] accountRoleNames) {

		UserAccount userAccount = Mockito.mock(UserAccount.class);

		Mockito.when(
			userAccount.getId()
		).thenReturn(
			_USER_ID
		);

		RoleBrief[] roleBriefs = _createRoleBriefs(globalRoleNames);

		Mockito.when(
			userAccount.getRoleBriefs()
		).thenReturn(
			roleBriefs
		);

		if (accountExternalReferenceCode == null) {
			Mockito.when(
				userAccount.getAccountBriefs()
			).thenReturn(
				new AccountBrief[0]
			);

			return userAccount;
		}

		RoleBrief[] accountRoleBriefs = _createRoleBriefs(accountRoleNames);

		AccountBrief accountBrief = Mockito.mock(AccountBrief.class);

		Mockito.when(
			accountBrief.getExternalReferenceCode()
		).thenReturn(
			accountExternalReferenceCode
		);

		Mockito.when(
			accountBrief.getRoleBriefs()
		).thenReturn(
			accountRoleBriefs
		);

		Mockito.when(
			userAccount.getAccountBriefs()
		).thenReturn(
			new AccountBrief[] {accountBrief}
		);

		return userAccount;
	}

	private static final String _ACCOUNT_ERC = "ACCOUNT-1";

	private static final String _PROJECT_ERC = "PRJCT-001";

	private static final long _USER_ID = 1000L;

	private final ProjectService _projectService = Mockito.mock(
		ProjectService.class);

}