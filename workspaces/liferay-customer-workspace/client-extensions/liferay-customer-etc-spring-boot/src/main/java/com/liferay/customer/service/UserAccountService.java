/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.customer.service;

import com.liferay.client.extension.util.spring.boot3.service.BaseService;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.headless.admin.user.client.pagination.Page;
import com.liferay.headless.admin.user.client.pagination.Pagination;
import com.liferay.headless.admin.user.client.resource.v1_0.UserAccountResource;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * @author Felipe Franca
 */
@Component
public class UserAccountService extends BaseService {

	public void addAccountUserAccount(
			Jwt jwt, String externalReferenceCode, String emailAddress)
		throws Exception {

		UserAccountResource userAccountResource = _getUserAccountResource(jwt);

		userAccountResource.
			postAccountUserAccountByExternalReferenceCodeByEmailAddress(
				externalReferenceCode, emailAddress);
	}

	public void addUserAccount(Jwt jwt, UserAccount userAccount)
		throws Exception {

		UserAccountResource userAccountResource = _getUserAccountResource(jwt);

		userAccountResource.postUserAccount(null, null, userAccount);
	}

	public void deleteAccountUserAccount(
			Jwt jwt, String externalReferenceCode, String emailAddress)
		throws Exception {

		UserAccountResource userAccountResource = _getUserAccountResource(jwt);

		userAccountResource.
			deleteAccountUserAccountByExternalReferenceCodeByEmailAddress(
				externalReferenceCode, emailAddress);
	}

	public List<UserAccount> getAccountUserAccounts(
			Jwt jwt, String externalReferenceCode)
		throws Exception {

		List<UserAccount> userAccounts = new ArrayList<>();

		UserAccountResource userAccountResource = _getUserAccountResource(jwt);

		for (int page = 1;; page++) {
			Page<UserAccount> userAccountsPage =
				userAccountResource.
					getAccountUserAccountsByExternalReferenceCodePage(
						externalReferenceCode, null, null,
						Pagination.of(page, 100), null);

			userAccounts.addAll(userAccountsPage.getItems());

			if (userAccountsPage.getLastPage() == page) {
				break;
			}
		}

		return userAccounts;
	}

	public UserAccount getMyUserAccount(Jwt jwt) throws Exception {
		UserAccountResource userAccountResource = _getUserAccountResource(jwt);

		return userAccountResource.getMyUserAccount();
	}

	public UserAccount getUserAccount(Jwt jwt, String emailAddress)
		throws Exception {

		UserAccountResource userAccountResource = _getUserAccountResource(jwt);

		return userAccountResource.getUserAccountByEmailAddress(emailAddress);
	}

	public void updateUserAccount(
			Jwt jwt, Long userAccountId, UserAccount userAccount)
		throws Exception {

		UserAccountResource userAccountResource = _getUserAccountResource(jwt);

		userAccountResource.patchUserAccount(userAccountId, userAccount);
	}

	private UserAccountResource _getUserAccountResource(Jwt jwt) {
		return UserAccountResource.builder(
		).header(
			HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue()
		).endpoint(
			lxcDXPMainDomain, lxcDXPServerProtocol
		).build();
	}

}