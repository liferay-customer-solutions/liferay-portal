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
import com.liferay.one.util.FindUtil;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.util.Validator;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * @author Felipe Franca
 */
@Component
public class EnvironmentActivationPermission {

	public Project check(Jwt jwt, String projectExternalReferenceCode)
		throws Exception {

		Project project = _projectService.fetchProject(
			projectExternalReferenceCode);

		if (!_contains(jwt, project, projectExternalReferenceCode)) {
			throw new PrincipalException();
		}

		return project;
	}

	private boolean _contains(
			Jwt jwt, Project project, String projectExternalReferenceCode)
		throws Exception {

		UserAccount userAccount = _userAccountService.getMyUserAccount(jwt);

		RoleBrief[] roleBriefs = userAccount.getRoleBriefs();

		if (roleBriefs != null) {
			for (RoleBrief roleBrief : roleBriefs) {
				String roleBriefName = roleBrief.getName();

				if (Objects.equals(
						RoleConstants.NAME_ADMINISTRATOR, roleBriefName) ||
					Objects.equals(
						RoleConstants.NAME_LIFERAY_STAFF, roleBriefName)) {

					return true;
				}
			}
		}

		if (project == null) {
			return false;
		}

		String accountExternalReferenceCode =
			project.getAccountExternalReferenceCode();

		if (Validator.isNotNull(accountExternalReferenceCode) &&
			_isAccountAdministrator(
				accountExternalReferenceCode, userAccount)) {

			return true;
		}

		ProjectMembership projectMembership =
			_projectMembershipService.fetchProjectMembership(
				projectExternalReferenceCode, RoleConstants.ERC_PROJECT_ADMIN,
				userAccount.getId());

		if (projectMembership != null) {
			return true;
		}

		return false;
	}

	private boolean _isAccountAdministrator(
		String accountExternalReferenceCode, UserAccount userAccount) {

		AccountBrief accountBrief = FindUtil.findFirst(
			userAccount.getAccountBriefs(),
			accountBrief1 -> Objects.equals(
				accountExternalReferenceCode,
				accountBrief1.getExternalReferenceCode()));

		if (accountBrief == null) {
			return false;
		}

		RoleBrief[] roleBriefs = accountBrief.getRoleBriefs();

		if (roleBriefs == null) {
			return false;
		}

		for (RoleBrief roleBrief : roleBriefs) {
			if (Objects.equals(
					RoleConstants.NAME_ACCOUNT_ADMINISTRATOR,
					roleBrief.getName())) {

				return true;
			}
		}

		return false;
	}

	@Autowired
	private ProjectMembershipService _projectMembershipService;

	@Autowired
	private ProjectService _projectService;

	@Autowired
	private UserAccountService _userAccountService;

}