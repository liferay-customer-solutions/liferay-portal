/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.headless.admin.user.client.resource.v1_0.UserAccountResource;

import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * @author Amos Fong
 */
@Component
public class UserAccountService extends OneBaseService {

	public UserAccount getMyUserAccount(Jwt jwt) throws Exception {
		UserAccountResource userAccountResource = UserAccountResource.builder(
		).endpoint(
			lxcDXPMainDomain, lxcDXPServerProtocol
		).header(
			HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue()
		).build();

		return userAccountResource.getMyUserAccount();
	}

	public UserAccount getUserAccount(long userId) throws Exception {
		UserAccountResource userAccountResource = UserAccountResource.builder(
		).endpoint(
			lxcDXPMainDomain, lxcDXPServerProtocol
		).header(
			HttpHeaders.AUTHORIZATION, getAuthorization()
		).build();

		return userAccountResource.getUserAccount(userId);
	}

	public UserAccount getUserAccountByEmailAddress(String emailAddress)
		throws Exception {

		UserAccountResource userAccountResource = UserAccountResource.builder(
		).endpoint(
			lxcDXPMainDomain, lxcDXPServerProtocol
		).header(
			HttpHeaders.AUTHORIZATION, getAuthorization()
		).build();

		return userAccountResource.getUserAccountByEmailAddress(emailAddress);
	}

}