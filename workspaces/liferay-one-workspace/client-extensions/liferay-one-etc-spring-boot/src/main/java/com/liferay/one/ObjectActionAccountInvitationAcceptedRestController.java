/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.liferay.one.model.AccountInvitation;
import com.liferay.one.service.AccountInvitationAcceptanceService;
import com.liferay.one.service.AccountInvitationService;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Pedro Oliveira
 */
@RequestMapping("/object/action/account/invitation/accepted")
@RestController
public class ObjectActionAccountInvitationAcceptedRestController
	extends OneBaseRestController {

	@PostMapping
	public void post(@AuthenticationPrincipal Jwt jwt, @RequestBody String json)
		throws Exception {

		JSONObject jsonObject = new JSONObject(json);

		JSONObject objectEntryJSONObject = jsonObject.getJSONObject(
			"objectEntry");

		AccountInvitation accountInvitation =
			_accountInvitationService.fetchAccountInvitation(
				objectEntryJSONObject.getLong("id"));

		if ((accountInvitation == null) || !accountInvitation.isAccepted()) {
			return;
		}

		_accountInvitationAcceptanceService.provisionAccountInvitation(
			accountInvitation);
	}

	@Autowired
	private AccountInvitationAcceptanceService
		_accountInvitationAcceptanceService;

	@Autowired
	private AccountInvitationService _accountInvitationService;

}