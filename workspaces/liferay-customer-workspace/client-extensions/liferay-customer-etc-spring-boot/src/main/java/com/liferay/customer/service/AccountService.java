/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.customer.service;

import com.liferay.client.extension.util.spring.boot3.service.BaseService;
import com.liferay.headless.admin.user.client.dto.v1_0.Account;
import com.liferay.headless.admin.user.client.resource.v1_0.AccountResource;

import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * @author Felipe Franca
 */
@Component
public class AccountService extends BaseService {

	public Account getAccount(Jwt jwt, String externalReferenceCode)
		throws Exception {

		AccountResource accountResource = _getAccountResource(jwt);

		return accountResource.getAccountByExternalReferenceCode(
			externalReferenceCode);
	}

	public void updateAccount(
			Jwt jwt, String externalReferenceCode, Account account)
		throws Exception {

		AccountResource accountResource = _getAccountResource(jwt);

		accountResource.putAccountByExternalReferenceCode(
			externalReferenceCode, account);
	}

	private AccountResource _getAccountResource(Jwt jwt) {
		return AccountResource.builder(
		).header(
			HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue()
		).endpoint(
			lxcDXPMainDomain, lxcDXPServerProtocol
		).build();
	}

}