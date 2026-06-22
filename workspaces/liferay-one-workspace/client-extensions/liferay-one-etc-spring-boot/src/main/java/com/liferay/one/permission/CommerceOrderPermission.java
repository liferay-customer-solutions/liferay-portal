/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.permission;

import com.liferay.headless.admin.user.client.dto.v1_0.AccountBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.RoleBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.headless.admin.user.client.resource.v1_0.UserAccountResource;
import com.liferay.one.constants.RoleConstants;
import com.liferay.one.service.CommerceOrderService;
import com.liferay.portal.kernel.security.auth.PrincipalException;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * @author Ricardo Mariz
 */
@Component
public class CommerceOrderPermission {

	public void check(long commerceOrderId, Jwt jwt) throws Exception {
		if (!_contains(commerceOrderId, jwt)) {
			throw new PrincipalException();
		}
	}

	private boolean _contains(long commerceOrderId, Jwt jwt) throws Exception {
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
				roleBriefName.equals(RoleConstants.NAME_LIFERAY_STAFF)) {

				return true;
			}
		}

		Long accountId = _commerceOrderService.getCommerceOrderAccountId(
			commerceOrderId);

		if (accountId == null) {
			return false;
		}

		for (AccountBrief accountBrief : userAccount.getAccountBriefs()) {
			if (Objects.equals(accountBrief.getId(), accountId)) {
				return true;
			}
		}

		return false;
	}

	@Autowired
	private CommerceOrderService _commerceOrderService;

	@Value("${com.liferay.lxc.dxp.mainDomain}")
	private String _lxcDXPMainDomain;

	@Value("${com.liferay.lxc.dxp.server.protocol}")
	private String _lxcDXPServerProtocol;

}