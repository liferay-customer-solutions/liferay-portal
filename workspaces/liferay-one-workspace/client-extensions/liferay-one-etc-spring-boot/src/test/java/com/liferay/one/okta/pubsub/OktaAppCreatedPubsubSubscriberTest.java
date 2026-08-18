/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.okta.pubsub;

import com.liferay.headless.admin.user.client.dto.v1_0.Account;
import com.liferay.one.constants.PropertyConstants;
import com.liferay.one.jira.exception.AccountNotFoundException;
import com.liferay.one.pubsub.Message;
import com.liferay.one.service.AccountService;
import com.liferay.one.service.PropertyService;

import java.util.Collections;

import org.json.JSONException;
import org.json.JSONObject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Felipe Franca
 */
public class OktaAppCreatedPubsubSubscriberTest {

	@BeforeEach
	public void setUp() throws Exception {
		_subscriber = new OktaAppCreatedPubsubSubscriber();

		_accountService = Mockito.mock(AccountService.class);
		_propertyService = Mockito.mock(PropertyService.class);

		Account account = new Account();

		account.setId(_ACCOUNT_ID);

		Mockito.when(
			_accountService.fetchAccountByExternalReferenceCode(_ACCOUNT_KEY)
		).thenReturn(
			account
		);

		ReflectionTestUtils.setField(
			_subscriber, "_accountService", _accountService);
		ReflectionTestUtils.setField(_subscriber, "_projectId", "test-project");
		ReflectionTestUtils.setField(
			_subscriber, "_propertyService", _propertyService);
		ReflectionTestUtils.setField(
			_subscriber, "_subscription", "test-subscription");
		ReflectionTestUtils.setField(_subscriber, "_topic", "test-topic");
	}

	@Test
	public void testIsAutoCreateTopicReturnsFalse() {
		Assertions.assertFalse(_subscriber.isAutoCreateTopic());
	}

	@Test
	public void testReceiveAddsOktaApplicationProperty() throws Exception {
		_receiveMessage(_ACCOUNT_KEY, _APP_ID);

		Mockito.verify(
			_propertyService
		).addProperty(
			_ACCOUNT_ID, PropertyConstants.NAME_OKTA_APPLICATION, _APP_ID
		);
	}

	@Test
	public void testReceiveSkipsWhenPropertyAlreadyExists() throws Exception {
		Mockito.when(
			_propertyService.getPropertyValue(
				_ACCOUNT_ID, PropertyConstants.NAME_OKTA_APPLICATION)
		).thenReturn(
			"existing-app-id"
		);

		_receiveMessage(_ACCOUNT_KEY, _APP_ID);

		Mockito.verify(
			_propertyService, Mockito.never()
		).addProperty(
			Mockito.anyLong(), Mockito.any(), Mockito.any()
		);
	}

	@Test
	public void testReceiveThrowsOnMalformedPayload() {
		Message message = new Message(
			Collections.emptyMap(), "not json", "test-topic");

		Assertions.assertThrows(
			JSONException.class, () -> _subscriber.receive(message));
	}

	@Test
	public void testReceiveThrowsWhenAccountKeyIsMissing() {
		Assertions.assertThrows(
			IllegalArgumentException.class, () -> _receiveMessage("", _APP_ID));
	}

	@Test
	public void testReceiveThrowsWhenAccountNotFound() throws Exception {
		Mockito.when(
			_accountService.fetchAccountByExternalReferenceCode(
				"unknown-account-key")
		).thenReturn(
			null
		);

		Assertions.assertThrows(
			AccountNotFoundException.class,
			() -> _receiveMessage("unknown-account-key", _APP_ID));
	}

	@Test
	public void testReceiveThrowsWhenAppIdIsMissing() {
		Assertions.assertThrows(
			IllegalArgumentException.class,
			() -> _receiveMessage(_ACCOUNT_KEY, ""));
	}

	@Test
	public void testReceiveThrowsWhenPayloadHasNoAccountKeyOrAppId()
		throws Exception {

		Message message = new Message(
			Collections.emptyMap(), "{}", "test-topic");

		Assertions.assertThrows(
			IllegalArgumentException.class, () -> _subscriber.receive(message));

		Mockito.verify(
			_accountService, Mockito.never()
		).fetchAccountByExternalReferenceCode(
			Mockito.any()
		);

		Mockito.verifyNoInteractions(_propertyService);
	}

	private void _receiveMessage(String accountKey, String appId)
		throws Exception {

		Message message = new Message(
			Collections.emptyMap(),
			new JSONObject(
			).put(
				"accountKey", accountKey
			).put(
				"appId", appId
			).toString(),
			"test-topic");

		_subscriber.receive(message);
	}

	private static final long _ACCOUNT_ID = 1000L;

	private static final String _ACCOUNT_KEY = "ACCOUNT-1";

	private static final String _APP_ID = "APP-1";

	private AccountService _accountService;
	private PropertyService _propertyService;
	private OktaAppCreatedPubsubSubscriber _subscriber;

}