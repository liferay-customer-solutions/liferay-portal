/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.permission;

import com.liferay.headless.admin.user.client.dto.v1_0.Account;
import com.liferay.headless.admin.user.client.dto.v1_0.OrganizationBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.RoleBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.one.constants.RoleConstants;
import com.liferay.one.service.AccountService;
import com.liferay.one.service.UserAccountService;
import com.liferay.one.util.UserAccountUtil;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.util.ArrayUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * @author Wellington Barbosa
 */
@Component
public class LicenseKeyPermission {

	public void check(long accountEntryId, String actionId, Jwt jwt)
		throws Exception {

		check(
			_userAccountService.getMyUserAccount(jwt), accountEntryId,
			actionId);
	}

	public void check(
			UserAccount userAccount, long accountEntryId, String actionId)
		throws Exception {

		if (!_contains(userAccount, accountEntryId, actionId)) {
			throw new PrincipalException();
		}
	}

	private boolean _contains(
			UserAccount userAccount, long accountEntryId, String actionId)
		throws Exception {

		if (_hasGlobalRole(userAccount, actionId)) {
			return true;
		}

		if (actionId.equals(ActionKeys.UPDATE)) {
			return UserAccountUtil.hasAccountRole(
				userAccount, accountEntryId,
				RoleConstants.NAMES_MANAGE_LICENSE_KEYS);
		}

		if (!actionId.equals(ActionKeys.VIEW)) {
			return false;
		}

		if (UserAccountUtil.hasAccountMembership(userAccount, accountEntryId)) {
			return true;
		}

		Account account = _accountService.fetchAccount(accountEntryId);

		if (account == null) {
			return false;
		}

		OrganizationBrief[] organizationBriefs =
			userAccount.getOrganizationBriefs();

		if (organizationBriefs == null) {
			return false;
		}

		for (OrganizationBrief organizationBrief : organizationBriefs) {
			if (ArrayUtil.contains(
					account.getOrganizationIds(), organizationBrief.getId())) {

				return true;
			}
		}

		return false;
	}

	private boolean _hasGlobalRole(UserAccount userAccount, String actionId) {
		RoleBrief[] roleBriefs = userAccount.getRoleBriefs();

		if (roleBriefs == null) {
			return false;
		}

		for (RoleBrief roleBrief : roleBriefs) {
			String roleBriefName = roleBrief.getName();

			if (roleBriefName.equals(RoleConstants.NAME_ADMINISTRATOR) ||
				roleBriefName.equals(
					RoleConstants.NAME_PROVISIONING_ADMINISTRATOR) ||
				roleBriefName.equals(RoleConstants.NAME_PROVISIONING_MEMBER)) {

				return true;
			}

			if (roleBriefName.equals(RoleConstants.NAME_LIFERAY_STAFF) &&
				actionId.equals(ActionKeys.VIEW)) {

				return true;
			}
		}

		return false;
	}

	@Autowired
	private AccountService _accountService;

	@Autowired
	private UserAccountService _userAccountService;

}