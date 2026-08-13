/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.one.model.AccountInvitation;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.StringUtil;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Pedro Oliveira
 */
@Component
public class AccountInvitationService extends OneBaseService {

	public AccountInvitation addAccountInvitation(
			String accountExternalReferenceCode, String emailAddress,
			String familyName, String givenName, List<String> roleNames)
		throws Exception {

		JSONObject accountInvitationJSONObject = new JSONObject(
		).put(
			"accepted", false
		).put(
			"accountExternalReferenceCode", accountExternalReferenceCode
		).put(
			"emailAddress", emailAddress
		).put(
			"familyName", familyName
		).put(
			"givenName", givenName
		).put(
			"roleNames",
			new JSONArray(
				roleNames
			).toString()
		).put(
			"token",
			UUID.randomUUID(
			).toString()
		);

		String response = post(
			getAuthorization(), accountInvitationJSONObject.toString(),
			UriComponentsBuilder.fromPath(
				"/o/c/accountinvitations"
			).build(
			).toUri());

		return new AccountInvitation(new JSONObject(response));
	}

	public AccountInvitation fetchAccountInvitationByToken(String token)
		throws Exception {

		List<AccountInvitation> accountInvitations = getAllItems(
			"/o/c/accountinvitations",
			"token eq '" + _escapeFilterValue(token) + "'",
			AccountInvitation::new);

		for (AccountInvitation accountInvitation : accountInvitations) {
			if (Objects.equals(accountInvitation.getToken(), token)) {
				return accountInvitation;
			}
		}

		return null;
	}

	public AccountInvitation fetchPendingAccountInvitation(
			String accountExternalReferenceCode, String emailAddress)
		throws Exception {

		List<AccountInvitation> accountInvitations = getAllItems(
			"/o/c/accountinvitations",
			StringBundler.concat(
				"accountExternalReferenceCode eq '",
				_escapeFilterValue(accountExternalReferenceCode),
				"' and emailAddress eq '", _escapeFilterValue(emailAddress),
				"'"),
			AccountInvitation::new);

		for (AccountInvitation accountInvitation : accountInvitations) {
			if (!accountInvitation.isAccepted()) {
				return accountInvitation;
			}
		}

		return null;
	}

	public void updateAccepted(long accountInvitationId) throws Exception {
		JSONObject accountInvitationJSONObject = new JSONObject(
		).put(
			"accepted", true
		);

		_patchAccountInvitation(
			accountInvitationId, accountInvitationJSONObject);
	}

	public AccountInvitation updateAccountInvitation(
			long accountInvitationId, String familyName, String givenName,
			List<String> roleNames)
		throws Exception {

		JSONObject accountInvitationJSONObject = new JSONObject(
		).put(
			"familyName", familyName
		).put(
			"givenName", givenName
		).put(
			"roleNames",
			new JSONArray(
				roleNames
			).toString()
		);

		String response = _patchAccountInvitation(
			accountInvitationId, accountInvitationJSONObject);

		return new AccountInvitation(new JSONObject(response));
	}

	private String _escapeFilterValue(String value) {
		return StringUtil.replace(value, '\'', "''");
	}

	private String _patchAccountInvitation(
			long accountInvitationId, JSONObject accountInvitationJSONObject)
		throws Exception {

		return patch(
			getAuthorization(), accountInvitationJSONObject.toString(),
			UriComponentsBuilder.fromPath(
				"/o/c/accountinvitations/{accountInvitationId}"
			).buildAndExpand(
				accountInvitationId
			).toUri());
	}

}