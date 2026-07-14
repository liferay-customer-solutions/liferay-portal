/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.one.okta.service.OktaService;
import com.liferay.one.permission.AdminPermission;
import com.liferay.one.service.UserAccountService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ricardo Mariz
 */
@RequestMapping("/user-accounts")
@RestController
public class UserAccountsRestController extends OneBaseRestController {

	@PostMapping("/{userId}/sync-with-okta")
	public void postSyncWithOkta(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("userId") long userId)
		throws Exception {

		_adminPermission.check(jwt);

		UserAccount userAccount = _userAccountService.getUserAccount(userId);

		_oktaService.syncContact(
			userAccount.getEmailAddress(), userAccount.getGivenName(),
			userAccount.getFamilyName(),
			userAccount.getExternalReferenceCode());
	}

	@Autowired
	private AdminPermission _adminPermission;

	@Autowired
	private OktaService _oktaService;

	@Autowired
	private UserAccountService _userAccountService;

}