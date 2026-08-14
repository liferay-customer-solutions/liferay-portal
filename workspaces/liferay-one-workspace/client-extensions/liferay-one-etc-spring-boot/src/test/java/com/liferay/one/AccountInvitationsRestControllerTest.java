/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.liferay.one.model.AccountInvitation;
import com.liferay.one.service.AccountInvitationService;
import com.liferay.one.util.KeyedLock;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Pedro Oliveira
 */
public class AccountInvitationsRestControllerTest {

	@Test
	public void testGetAcceptReturnsErrorWhenUpdateFails() throws Exception {
		AccountInvitationsRestController accountInvitationsRestController =
			_createController();

		Mockito.when(
			_accountInvitationService.fetchAccountInvitationByToken(_TOKEN)
		).thenReturn(
			_createAccountInvitation(false, _FUTURE_CUSTOM_EXPIRATION_DATE)
		);

		Mockito.doThrow(
			new IllegalStateException("Unable to update the invitation")
		).when(
			_accountInvitationService
		).updateAccepted(
			_ACCOUNT_INVITATION_ID
		);

		ResponseEntity<String> responseEntity =
			accountInvitationsRestController.getAccept(_TOKEN);

		Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		Assertions.assertEquals("error", _getStatus(responseEntity));
	}

	@Test
	public void testGetAcceptReturnsExpiredInvitation() throws Exception {
		AccountInvitationsRestController accountInvitationsRestController =
			_createController();

		Mockito.when(
			_accountInvitationService.fetchAccountInvitationByToken(_TOKEN)
		).thenReturn(
			_createAccountInvitation(false, _PAST_CUSTOM_EXPIRATION_DATE)
		);

		ResponseEntity<String> responseEntity =
			accountInvitationsRestController.getAccept(_TOKEN);

		Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		Assertions.assertEquals("expired", _getStatus(responseEntity));

		Mockito.verify(
			_accountInvitationService, Mockito.never()
		).updateAccepted(
			ArgumentMatchers.anyLong()
		);
	}

	@Test
	public void testGetAcceptReturnsExpiredWhenExpirationDateMissing()
		throws Exception {

		AccountInvitationsRestController accountInvitationsRestController =
			_createController();

		Mockito.when(
			_accountInvitationService.fetchAccountInvitationByToken(_TOKEN)
		).thenReturn(
			_createAccountInvitation(false, null)
		);

		ResponseEntity<String> responseEntity =
			accountInvitationsRestController.getAccept(_TOKEN);

		Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		Assertions.assertEquals("expired", _getStatus(responseEntity));

		Mockito.verify(
			_accountInvitationService, Mockito.never()
		).updateAccepted(
			ArgumentMatchers.anyLong()
		);
	}

	@Test
	public void testGetAcceptReturnsInvalidTokenWithoutLookup()
		throws Exception {

		AccountInvitationsRestController accountInvitationsRestController =
			_createController();

		ResponseEntity<String> responseEntity =
			accountInvitationsRestController.getAccept("x' or 'a' eq 'a");

		Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		Assertions.assertEquals("invalid", _getStatus(responseEntity));

		Mockito.verifyNoInteractions(_accountInvitationService);
	}

	@Test
	public void testGetAcceptReturnsInvalidWhenInvitationMissing()
		throws Exception {

		AccountInvitationsRestController accountInvitationsRestController =
			_createController();

		Mockito.when(
			_accountInvitationService.fetchAccountInvitationByToken(_TOKEN)
		).thenReturn(
			null
		);

		ResponseEntity<String> responseEntity =
			accountInvitationsRestController.getAccept(_TOKEN);

		Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		Assertions.assertEquals("invalid", _getStatus(responseEntity));

		Mockito.verify(
			_accountInvitationService, Mockito.never()
		).updateAccepted(
			ArgumentMatchers.anyLong()
		);
	}

	@Test
	public void testGetAcceptSkipsAcceptedInvitation() throws Exception {
		AccountInvitationsRestController accountInvitationsRestController =
			_createController();

		Mockito.when(
			_accountInvitationService.fetchAccountInvitationByToken(_TOKEN)
		).thenReturn(
			_createAccountInvitation(true, _FUTURE_CUSTOM_EXPIRATION_DATE)
		);

		ResponseEntity<String> responseEntity =
			accountInvitationsRestController.getAccept(_TOKEN);

		Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		Assertions.assertEquals("accepted", _getStatus(responseEntity));

		Mockito.verify(
			_accountInvitationService, Mockito.never()
		).updateAccepted(
			ArgumentMatchers.anyLong()
		);
	}

	@Test
	public void testGetAcceptUpdatesAccepted() throws Exception {
		AccountInvitationsRestController accountInvitationsRestController =
			_createController();

		Mockito.when(
			_accountInvitationService.fetchAccountInvitationByToken(_TOKEN)
		).thenReturn(
			_createAccountInvitation(false, _FUTURE_CUSTOM_EXPIRATION_DATE)
		);

		ResponseEntity<String> responseEntity =
			accountInvitationsRestController.getAccept(_TOKEN);

		Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		Assertions.assertEquals("accepted", _getStatus(responseEntity));

		Mockito.verify(
			_accountInvitationService
		).updateAccepted(
			_ACCOUNT_INVITATION_ID
		);
	}

	private AccountInvitation _createAccountInvitation(
		boolean accepted, String customExpirationDate) {

		return new AccountInvitation(
			new JSONObject(
			).put(
				"accepted", accepted
			).put(
				"accountExternalReferenceCode", "ACC-1"
			).put(
				"customExpirationDate", customExpirationDate
			).put(
				"emailAddress", "jane@example.com"
			).put(
				"externalReferenceCode", "INV-1"
			).put(
				"familyName", "Doe"
			).put(
				"givenName", "Jane"
			).put(
				"id", _ACCOUNT_INVITATION_ID
			).put(
				"projectExternalReferenceCode", ""
			).put(
				"projectRoleExternalReferenceCode", ""
			).put(
				"roleExternalReferenceCodes",
				new JSONArray(
					List.of()
				).toString()
			).put(
				"token", _TOKEN
			));
	}

	private AccountInvitationsRestController _createController() {
		AccountInvitationsRestController accountInvitationsRestController =
			new AccountInvitationsRestController();

		ReflectionTestUtils.setField(
			accountInvitationsRestController, "_accountInvitationService",
			_accountInvitationService);
		ReflectionTestUtils.setField(
			accountInvitationsRestController, "_keyedLock", new KeyedLock());

		return accountInvitationsRestController;
	}

	private String _getStatus(ResponseEntity<String> responseEntity) {
		JSONObject jsonObject = new JSONObject(responseEntity.getBody());

		return jsonObject.getString("status");
	}

	private static final long _ACCOUNT_INVITATION_ID = 44444;

	private static final String _FUTURE_CUSTOM_EXPIRATION_DATE =
		"2999-01-01T00:00:00Z";

	private static final String _PAST_CUSTOM_EXPIRATION_DATE =
		"2000-01-01T00:00:00Z";

	private static final String _TOKEN = "11111111-2222-3333-4444-555555555555";

	private final AccountInvitationService _accountInvitationService =
		Mockito.mock(AccountInvitationService.class);

}