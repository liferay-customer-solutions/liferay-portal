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

		if (!_contains(accountEntryId, actionId, jwt)) {
			throw new PrincipalException();
		}
	}

	private boolean _contains(long accountEntryId, String actionId, Jwt jwt)
		throws Exception {

		UserAccount userAccount = _userAccountService.getMyUserAccount(jwt);

		for (RoleBrief roleBrief : userAccount.getRoleBriefs()) {
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

		for (OrganizationBrief organizationBrief :
				userAccount.getOrganizationBriefs()) {

			if (ArrayUtil.contains(
					account.getOrganizationIds(), organizationBrief.getId())) {

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