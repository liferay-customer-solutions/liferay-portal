/**
 * SPDX-FileCopyrightText: (c) 2023 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.partner.service;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.liferay.client.extension.util.spring.boot.LiferayOAuth2AccessTokenManager;

/**
 * @author Felipe Franca
 */
@Component
public class UserAccountWebService {

    public JSONObject getUserAccount(
			Jwt jwt)
		throws Exception {

		JSONObject jsonObject = new JSONObject(
			WebClient.create(
				_lxcDXPServerProtocol + "://" + _lxcDXPMainDomain
			).get(
			).uri(
				"/o/headless-admin-user/v1.0/user-accounts/"
				+ jwt.getClaimAsString("sub")
			).accept(
				MediaType.APPLICATION_JSON
			).header(
				HttpHeaders.AUTHORIZATION, _getAuthorization()
			).retrieve(
			).bodyToMono(
				String.class
			).block());

		return jsonObject;
	}

	private String _getAuthorization() {
		return _liferayOAuth2AccessTokenManager.getAuthorization(
			"liferay-partner-etc-spring-boot-oauth-application-headless-server");
	}

	@Autowired
	private LiferayOAuth2AccessTokenManager _liferayOAuth2AccessTokenManager;

    @Value("${com.liferay.lxc.dxp.mainDomain}")
	private String _lxcDXPMainDomain;

	@Value("${com.liferay.lxc.dxp.server.protocol}")
	private String _lxcDXPServerProtocol;

}
