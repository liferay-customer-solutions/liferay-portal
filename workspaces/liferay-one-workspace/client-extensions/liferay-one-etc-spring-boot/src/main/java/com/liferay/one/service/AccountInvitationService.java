/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.one.model.AccountInvitation;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Pedro Oliveira
 */
@Component
public class AccountInvitationService extends OneBaseService {

	public AccountInvitation addAccountInvitation(
			String accountExternalReferenceCode, String emailAddress,
			String familyName, String givenName,
			String projectExternalReferenceCode,
			String projectRoleExternalReferenceCode,
			List<String> roleExternalReferenceCodes)
		throws Exception {

		JSONObject accountInvitationJSONObject = new JSONObject(
		).put(
			"accepted", false
		).put(
			"accountExternalReferenceCode", accountExternalReferenceCode
		).put(
			"customExpirationDate", _getCustomExpirationDate()
		).put(
			"emailAddress", emailAddress
		).put(
			"familyName", familyName
		).put(
			"givenName", givenName
		).put(
			"projectExternalReferenceCode", projectExternalReferenceCode
		).put(
			"projectRoleExternalReferenceCode", projectRoleExternalReferenceCode
		).put(
			"roleExternalReferenceCodes",
			new JSONArray(
				roleExternalReferenceCodes
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

	public void deleteAccountInvitation(long accountInvitationId)
		throws Exception {

		delete(
			getAuthorization(), StringPool.BLANK,
			UriComponentsBuilder.fromPath(
				"/o/c/accountinvitations/{accountInvitationId}"
			).buildAndExpand(
				accountInvitationId
			).toUri());
	}

	public AccountInvitation fetchAccountInvitation(long accountInvitationId)
		throws Exception {

		String response = null;

		try {
			response = get(
				getAuthorization(),
				UriComponentsBuilder.fromPath(
					"/o/c/accountinvitations/{accountInvitationId}"
				).buildAndExpand(
					accountInvitationId
				).toUri());
		}
		catch (WebClientResponseException webClientResponseException) {
			HttpStatusCode httpStatusCode =
				webClientResponseException.getStatusCode();

			if (!httpStatusCode.isSameCodeAs(HttpStatus.NOT_FOUND)) {
				throw webClientResponseException;
			}
		}

		if (Validator.isNull(response)) {
			return null;
		}

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
			String accountExternalReferenceCode, String emailAddress,
			String projectExternalReferenceCode)
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
			if (!accountInvitation.isAccepted() &&
				Objects.equals(
					accountInvitation.getProjectExternalReferenceCode(),
					projectExternalReferenceCode)) {

				return accountInvitation;
			}
		}

		return null;
	}

	public List<AccountInvitation> getPendingAccountInvitations(
			String accountExternalReferenceCode)
		throws Exception {

		List<AccountInvitation> pendingAccountInvitations = new ArrayList<>();

		List<AccountInvitation> accountInvitations = getAllItems(
			"/o/c/accountinvitations",
			"accountExternalReferenceCode eq '" +
				_escapeFilterValue(accountExternalReferenceCode) + "'",
			AccountInvitation::new);

		for (AccountInvitation accountInvitation : accountInvitations) {
			if (!accountInvitation.isAccepted()) {
				pendingAccountInvitations.add(accountInvitation);
			}
		}

		return pendingAccountInvitations;
	}

	public AccountInvitation renewAccountInvitation(long accountInvitationId)
		throws Exception {

		JSONObject accountInvitationJSONObject = new JSONObject(
		).put(
			"customExpirationDate", _getCustomExpirationDate()
		).put(
			"token",
			UUID.randomUUID(
			).toString()
		);

		String response = _patchAccountInvitation(
			accountInvitationId, accountInvitationJSONObject);

		return new AccountInvitation(new JSONObject(response));
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
			String projectRoleExternalReferenceCode,
			List<String> roleExternalReferenceCodes)
		throws Exception {

		JSONObject accountInvitationJSONObject = new JSONObject(
		).put(
			"customExpirationDate", _getCustomExpirationDate()
		).put(
			"familyName", familyName
		).put(
			"givenName", givenName
		).put(
			"projectRoleExternalReferenceCode", projectRoleExternalReferenceCode
		).put(
			"roleExternalReferenceCodes",
			new JSONArray(
				roleExternalReferenceCodes
			).toString()
		).put(
			"token",
			UUID.randomUUID(
			).toString()
		);

		String response = _patchAccountInvitation(
			accountInvitationId, accountInvitationJSONObject);

		return new AccountInvitation(new JSONObject(response));
	}

	private String _escapeFilterValue(String value) {
		return StringUtil.replace(value, '\'', "''");
	}

	private String _getCustomExpirationDate() {
		return DateTimeFormatter.ISO_INSTANT.format(
			Instant.now(
			).plus(
				_invitationExpirationDays, ChronoUnit.DAYS
			).truncatedTo(
				ChronoUnit.SECONDS
			));
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

	@Value("${liferay.one.invitation.expiration.days}")
	private int _invitationExpirationDays;

}