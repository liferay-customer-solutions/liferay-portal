/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.permission;

import com.liferay.headless.admin.user.client.dto.v1_0.RoleBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.one.constants.RoleConstants;
import com.liferay.one.service.UserAccountService;
import com.liferay.portal.kernel.security.auth.PrincipalException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * @author Kyle Bischof
 */
@Component
public class AdminPermission {

	public void check(Jwt jwt) throws Exception {
		if (!_contains(jwt)) {
			throw new PrincipalException();
		}
	}

	private boolean _contains(Jwt jwt) throws Exception {
		UserAccount userAccount = _userAccountService.getMyUserAccount(jwt);

		for (RoleBrief roleBrief : userAccount.getRoleBriefs()) {
			String name = roleBrief.getName();

			if (name.equals(RoleConstants.NAME_ADMINISTRATOR) ||
				name.equals(RoleConstants.NAME_PROVISIONING_ADMINISTRATOR)) {

				return true;
			}
		}

		return false;
	}

	@Autowired
	private UserAccountService _userAccountService;

}