/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.permission;

import com.liferay.headless.admin.user.client.dto.v1_0.RoleBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.headless.admin.user.client.resource.v1_0.UserAccountResource;
import com.liferay.one.constants.RoleConstants;
import com.liferay.portal.kernel.security.auth.PrincipalException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * @author Kyle Bischof
 */
@Component
public class DebugMessageQueuePermission {

	public void check(Jwt jwt) throws Exception {
		if (!_contains(jwt)) {
			throw new PrincipalException();
		}
	}

	private boolean _contains(Jwt jwt) throws Exception {
		UserAccountResource userAccountResource = UserAccountResource.builder(
		).header(
			HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue()
		).endpoint(
			_lxcDXPMainDomain, _lxcDXPServerProtocol
		).build();

		UserAccount userAccount = userAccountResource.getMyUserAccount();

		for (RoleBrief roleBrief : userAccount.getRoleBriefs()) {
			String roleBriefName = roleBrief.getName();

			if (roleBriefName.equals(RoleConstants.NAME_ADMINISTRATOR) ||
				roleBriefName.equals(
					RoleConstants.NAME_PROVISIONING_ADMINISTRATOR)) {

				return true;
			}
		}

		return false;
	}

	@Value("${com.liferay.lxc.dxp.mainDomain}")
	private String _lxcDXPMainDomain;

	@Value("${com.liferay.lxc.dxp.server.protocol}")
	private String _lxcDXPServerProtocol;

}