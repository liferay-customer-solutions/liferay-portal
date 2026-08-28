/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.permission;

import com.liferay.headless.admin.user.client.dto.v1_0.AccountBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.RoleBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.Account;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.Order;
import com.liferay.one.constants.RoleConstants;
import com.liferay.one.service.CommerceOrderService;
import com.liferay.one.service.UserAccountService;
import com.liferay.portal.kernel.security.auth.PrincipalException;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * @author Ricardo Mariz
 */
@Component
public class CommerceOrderPermission {

	public void check(long commerceOrderId, Jwt jwt) throws Exception {
		check(commerceOrderId, _userAccountService.getMyUserAccount(jwt));
	}

	public void check(long commerceOrderId, UserAccount userAccount)
		throws Exception {

		if (!_contains(commerceOrderId, userAccount)) {
			throw new PrincipalException();
		}
	}

	private boolean _contains(long commerceOrderId, UserAccount userAccount)
		throws Exception {

		for (RoleBrief roleBrief : userAccount.getRoleBriefs()) {
			String roleBriefName = roleBrief.getName();

			if (roleBriefName.equals(RoleConstants.NAME_ADMINISTRATOR) ||
				roleBriefName.equals(RoleConstants.NAME_LIFERAY_STAFF)) {

				return true;
			}
		}

		Order commerceOrder = _commerceOrderService.fetchCommerceOrder(
			commerceOrderId);

		if (commerceOrder == null) {
			return false;
		}

		Account account = commerceOrder.getAccount();

		if (account == null) {
			return false;
		}

		for (AccountBrief accountBrief : userAccount.getAccountBriefs()) {
			if (Objects.equals(accountBrief.getId(), account.getId())) {
				return true;
			}
		}

		return false;
	}

	@Autowired
	private CommerceOrderService _commerceOrderService;

	@Autowired
	private UserAccountService _userAccountService;

}