/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.headless.admin.user.client.pagination.Page;
import com.liferay.headless.admin.user.client.pagination.Pagination;
import com.liferay.headless.admin.user.client.problem.Problem;
import com.liferay.headless.admin.user.client.resource.v1_0.UserAccountResource;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * @author Amos Fong
 */
@Component
public class UserAccountService extends OneBaseService {

	public UserAccount addUserAccount(
			String emailAddress, String familyName, String givenName)
		throws Exception {

		UserAccountResource userAccountResource = _buildUserAccountResource();

		UserAccount userAccount = new UserAccount();

		userAccount.setEmailAddress(() -> emailAddress);

		if (Validator.isNotNull(familyName)) {
			userAccount.setFamilyName(() -> familyName);
		}

		if (Validator.isNotNull(givenName)) {
			userAccount.setGivenName(() -> givenName);
		}

		try {
			return userAccountResource.postUserAccount(null, null, userAccount);
		}
		catch (Problem.ProblemException problemException) {
			Problem problem = problemException.getProblem();

			if ((problem != null) &&
				Objects.equals(
					problem.getStatus(), HttpStatus.CONFLICT.name())) {

				return getUserAccountByEmailAddress(emailAddress);
			}

			throw problemException;
		}
	}

	public UserAccount fetchUserAccountByEmailAddress(String emailAddress)
		throws Exception {

		UserAccountResource userAccountResource = _buildUserAccountResource(
			"nestedFields", "accountBriefs,customFields,organizationBriefs");

		try {
			return userAccountResource.getUserAccountByEmailAddress(
				emailAddress);
		}
		catch (Problem.ProblemException problemException) {
			Problem problem = problemException.getProblem();

			if ((problem != null) && isNotFound(problem.getStatus())) {
				return null;
			}

			throw problemException;
		}
	}

	public List<UserAccount> getAccountUserAccounts(long accountId)
		throws Exception {

		UserAccountResource userAccountResource = _buildUserAccountResource(
			"nestedFields", "customFields");

		List<UserAccount> userAccounts = new ArrayList<>();

		int page = 1;

		while (true) {
			Page<UserAccount> userAccountsPage =
				userAccountResource.getAccountUserAccountsPage(
					accountId, null, null, Pagination.of(page, _PAGE_SIZE),
					null);

			userAccounts.addAll(userAccountsPage.getItems());

			if (page >= userAccountsPage.getLastPage()) {
				break;
			}

			page++;
		}

		return userAccounts;
	}

	public List<UserAccount> getAllUserAccounts() throws Exception {
		UserAccountResource userAccountResource = UserAccountResource.builder(
		).endpoint(
			lxcDXPMainDomain, lxcDXPServerProtocol
		).header(
			HttpHeaders.AUTHORIZATION, getAuthorization()
		).build();

		List<UserAccount> userAccounts = new ArrayList<>();

		int page = 1;

		while (true) {
			Page<UserAccount> userAccountsPage =
				userAccountResource.getUserAccountsPage(
					null, null, Pagination.of(page, _PAGE_SIZE), null);

			userAccounts.addAll(userAccountsPage.getItems());

			if (page >= userAccountsPage.getLastPage()) {
				break;
			}

			page++;
		}

		return userAccounts;
	}

	public UserAccount getMyUserAccount(Jwt jwt) throws Exception {
		UserAccountResource userAccountResource = _buildUserAccountResource(
			jwt);

		return userAccountResource.getMyUserAccount();
	}

	public UserAccount getUserAccount(long userId) throws Exception {
		UserAccountResource userAccountResource = _buildUserAccountResource();

		return userAccountResource.getUserAccount(userId);
	}

	public UserAccount getUserAccountByEmailAddress(String emailAddress)
		throws Exception {

		UserAccountResource userAccountResource = _buildUserAccountResource();

		return userAccountResource.getUserAccountByEmailAddress(emailAddress);
	}

	public boolean hasAccountUserAccount(long accountId, long userId)
		throws Exception {

		UserAccountResource userAccountResource = _buildUserAccountResource();

		try {
			userAccountResource.getAccountUserAccount(accountId, userId);

			return true;
		}
		catch (Problem.ProblemException problemException) {
			Problem problem = problemException.getProblem();

			if ((problem != null) && isNotFound(problem.getStatus())) {
				return false;
			}

			throw problemException;
		}
	}

	public boolean hasUserAccounts(long accountId) throws Exception {
		UserAccountResource userAccountResource = _buildUserAccountResource();

		Page<UserAccount> userAccountsPage =
			userAccountResource.getAccountUserAccountsPage(
				accountId, null, null, Pagination.of(1, 1), null);

		if (userAccountsPage.getTotalCount() > 0) {
			return true;
		}

		return false;
	}

	private UserAccountResource _buildUserAccountResource(
		Jwt jwt, String... parameters) {

		return UserAccountResource.builder(
		).endpoint(
			lxcDXPMainDomain, lxcDXPServerProtocol
		).header(
			HttpHeaders.AUTHORIZATION, getAuthorization(jwt)
		).parameters(
			parameters
		).build();
	}

	private UserAccountResource _buildUserAccountResource(
		String... parameters) {

		return UserAccountResource.builder(
		).endpoint(
			lxcDXPMainDomain, lxcDXPServerProtocol
		).header(
			HttpHeaders.AUTHORIZATION, getAuthorization()
		).parameters(
			parameters
		).build();
	}

	private static final int _PAGE_SIZE = 500;

}