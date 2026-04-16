/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.customer.permission;

import com.liferay.customer.constants.RoleConstants;
import com.liferay.customer.service.AccountService;
import com.liferay.customer.service.UserAccountService;
import com.liferay.headless.admin.user.client.dto.v1_0.Account;
import com.liferay.headless.admin.user.client.dto.v1_0.AccountBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.OrganizationBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.RoleBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.util.ArrayUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * @author Jenny Chen
 */
@Component
public class BusinessEventPermission {

	public void check(
			Jwt jwt, String accountExternalReferenceCode, String actionId)
		throws Exception {

		if (!_contains(jwt, accountExternalReferenceCode, actionId)) {
			throw new PrincipalException();
		}
	}

	private boolean _contains(
			Jwt jwt, String accountExternalReferenceCode, String actionId)
		throws Exception {

		UserAccount userAccount = _userAccountService.getMyUserAccount(jwt);

		for (RoleBrief roleBrief : userAccount.getRoleBriefs()) {
			String roleBriefName = roleBrief.getName();

			if (roleBriefName.equals(RoleConstants.NAME_ADMINISTRATOR) ||
				roleBriefName.equals(RoleConstants.NAME_LIFERAY_STAFF)) {

				return true;
			}
		}

		Account account = _accountService.getAccount(
			jwt, accountExternalReferenceCode);

		AccountBrief[] accountBriefs = userAccount.getAccountBriefs();

		for (AccountBrief accountBrief : accountBriefs) {
			if (accountExternalReferenceCode.equals(
					accountBrief.getExternalReferenceCode())) {

				for (RoleBrief roleBrief : accountBrief.getRoleBriefs()) {
					if (ArrayUtil.contains(
							RoleConstants.NAMES_SUPPORT_ACCOUNT_ROLES,
							roleBrief.getName()) &&
						actionId.equals(ActionKeys.VIEW)) {

						return true;
					}

					if (ArrayUtil.contains(
							RoleConstants.NAMES_SUPPORT_ACCOUNT_TICKET_ROLES,
							roleBrief.getName()) &&
						actionId.equals(ActionKeys.UPDATE)) {

						return true;
					}
				}
			}
		}

		OrganizationBrief[] organizationBriefs =
			userAccount.getOrganizationBriefs();

		for (OrganizationBrief organizationBrief : organizationBriefs) {
			if (ArrayUtil.contains(
					account.getOrganizationIds(), organizationBrief.getId())) {

				return true;
			}
		}

		return false;
	}

	@Autowired
	private AccountService _accountService;

	@Value("${com.liferay.lxc.dxp.mainDomain}")
	private String _lxcDXPMainDomain;

	@Value("${com.liferay.lxc.dxp.server.protocol}")
	private String _lxcDXPServerProtocol;

	@Autowired
	private UserAccountService _userAccountService;

}