/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.customer.service;

import com.liferay.client.extension.util.spring.boot3.service.BaseService;
import com.liferay.headless.admin.user.client.dto.v1_0.AccountRole;
import com.liferay.headless.admin.user.client.pagination.Page;
import com.liferay.headless.admin.user.client.pagination.Pagination;
import com.liferay.headless.admin.user.client.resource.v1_0.AccountRoleResource;
import com.liferay.portal.kernel.util.ListUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * @author Felipe Franca
 */
@Component
public class AccountRoleService extends BaseService {

	public void addAccountAccountRolesUserAccount(
			Jwt jwt, String externalReferenceCode, List<Long> accountRoleIds,
			String emailAddress)
		throws Exception {

		if (ListUtil.isEmpty(accountRoleIds)) {
			return;
		}

		for (long accountRoleId : accountRoleIds) {
			addAccountAccountRoleUserAccount(
				jwt, externalReferenceCode, accountRoleId, emailAddress);
		}
	}

	public void addAccountAccountRoleUserAccount(
			Jwt jwt, String externalReferenceCode, Long accountRoleId,
			String emailAddress)
		throws Exception {

		AccountRoleResource accountRoleResource = _getAccountRoleResource(jwt);

		accountRoleResource.
			postAccountByExternalReferenceCodeAccountRoleUserAccountByEmailAddress(
				externalReferenceCode, accountRoleId, emailAddress);
	}

	public void deleteAccountAccountRoleUserAccount(
			Jwt jwt, String externalReferenceCode, Long accountRoleId,
			String emailAddress)
		throws Exception {

		AccountRoleResource accountRoleResource = _getAccountRoleResource(jwt);

		accountRoleResource.
			deleteAccountByExternalReferenceCodeAccountRoleUserAccountByEmailAddress(
				externalReferenceCode, accountRoleId, emailAddress);
	}

	public List<AccountRole> getAccountAccountRoles(
			Jwt jwt, String externalReferenceCode)
		throws Exception {

		List<AccountRole> accountRoles = new ArrayList<>();

		AccountRoleResource accountRoleResource = _getAccountRoleResource(jwt);

		for (int page = 1;; page++) {
			Page<AccountRole> accountRolesPage =
				accountRoleResource.
					getAccountAccountRolesByExternalReferenceCodePage(
						externalReferenceCode, null, null,
						Pagination.of(page, 100), null);

			accountRoles.addAll(accountRolesPage.getItems());

			if (accountRolesPage.getLastPage() == page) {
				break;
			}
		}

		return accountRoles;
	}

	public Collection<AccountRole> getAccountUserAccountAccountRoles(
			Jwt jwt, String externalReferenceCode, String emailAddress)
		throws Exception {

		AccountRoleResource accountRoleResource = _getAccountRoleResource(jwt);

		Page<AccountRole> accountRolesPage =
			accountRoleResource.
				getAccountByExternalReferenceCodeUserAccountByEmailAddressAccountRolesPage(
					externalReferenceCode, emailAddress);

		return accountRolesPage.getItems();
	}

	public void updateAccountAccountRolesUserAccount(
			Jwt jwt, String externalReferenceCode, List<Long> accountRoleIds,
			String emailAddress)
		throws Exception {

		addAccountAccountRolesUserAccount(
			jwt, externalReferenceCode, accountRoleIds, emailAddress);

		Collection<AccountRole> accountRoles =
			getAccountUserAccountAccountRoles(
				jwt, externalReferenceCode, emailAddress);

		for (AccountRole accountRole : accountRoles) {
			if (!accountRoleIds.contains(accountRole.getId())) {
				deleteAccountAccountRoleUserAccount(
					jwt, externalReferenceCode, accountRole.getId(),
					emailAddress);
			}
		}
	}

	private AccountRoleResource _getAccountRoleResource(Jwt jwt) {
		return AccountRoleResource.builder(
		).header(
			HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue()
		).endpoint(
			lxcDXPMainDomain, lxcDXPServerProtocol
		).build();
	}

}