/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.liferay.one.permission.AdminPermission;
import com.liferay.one.pubsub.Message;
import com.liferay.one.pubsub.subscriber.BasePubsubSubscriber;

import java.util.ArrayList;
import java.util.Arrays;

import org.json.JSONObject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author Karoline Silva
 */
public class AdminRestControllerTest {

	@Test
	public void testPostDispatchesMessageToMatchingSubscriber()
		throws Exception {

		BasePubsubSubscriber basePubsubSubscriber = _createSubscriber(
			"test-topic");

		AdminRestController adminRestController = _createController(
			basePubsubSubscriber);

		String json = new JSONObject(
		).put(
			"attributes", "a=1\nb=2"
		).put(
			"payload", "line1\nline2"
		).put(
			"topic", "test-topic"
		).toString();

		ResponseEntity<Void> responseEntity =
			adminRestController.postPubsubDispatch(null, json);

		Assertions.assertEquals(
			HttpStatus.OK.value(),
			responseEntity.getStatusCode(
			).value());

		ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(
			Message.class);

		Mockito.verify(
			basePubsubSubscriber
		).receive(
			messageArgumentCaptor.capture()
		);

		Message message = messageArgumentCaptor.getValue();

		Assertions.assertEquals("line1line2", message.getPayload());
		Assertions.assertEquals("test-topic", message.getTopic());
		Assertions.assertEquals(
			Arrays.asList("a", "b"),
			new ArrayList<>(
				message.getAttributes(
				).keySet()));
		Assertions.assertEquals("1", message.get("a"));
		Assertions.assertEquals("2", message.get("b"));
	}

	@Test
	public void testPostParsesValueContainingEquals() throws Exception {
		BasePubsubSubscriber basePubsubSubscriber = _createSubscriber(
			"test-topic");

		AdminRestController adminRestController = _createController(
			basePubsubSubscriber);

		String json = new JSONObject(
		).put(
			"attributes", "url=https://example.com?a=1&b=2"
		).put(
			"payload", "body"
		).put(
			"topic", "test-topic"
		).toString();

		adminRestController.postPubsubDispatch(null, json);

		ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(
			Message.class);

		Mockito.verify(
			basePubsubSubscriber
		).receive(
			messageArgumentCaptor.capture()
		);

		Message message = messageArgumentCaptor.getValue();

		Assertions.assertEquals(
			"https://example.com?a=1&b=2", message.get("url"));
	}

	@Test
	public void testPostThrowsBadGatewayWhenDispatchFails() throws Exception {
		BasePubsubSubscriber basePubsubSubscriber = _createSubscriber(
			"test-topic");

		Mockito.doThrow(
			new RuntimeException("downstream boom")
		).when(
			basePubsubSubscriber
		).receive(
			Mockito.any()
		);

		AdminRestController adminRestController = _createController(
			basePubsubSubscriber);

		String json = new JSONObject(
		).put(
			"attributes", ""
		).put(
			"payload", "body"
		).put(
			"topic", "test-topic"
		).toString();

		ResponseStatusException responseStatusException =
			Assertions.assertThrows(
				ResponseStatusException.class,
				() -> adminRestController.postPubsubDispatch(null, json));

		Assertions.assertEquals(
			HttpStatus.BAD_GATEWAY.value(),
			responseStatusException.getStatusCode(
			).value());
	}

	@Test
	public void testPostThrowsBadRequestWhenPropertiesAreMalformed()
		throws Exception {

		BasePubsubSubscriber basePubsubSubscriber = _createSubscriber(
			"test-topic");

		AdminRestController adminRestController = _createController(
			basePubsubSubscriber);

		String json = new JSONObject(
		).put(
			"attributes", "missing-delimiter"
		).put(
			"payload", "body"
		).put(
			"topic", "test-topic"
		).toString();

		ResponseStatusException responseStatusException =
			Assertions.assertThrows(
				ResponseStatusException.class,
				() -> adminRestController.postPubsubDispatch(null, json));

		Assertions.assertEquals(
			HttpStatus.BAD_REQUEST.value(),
			responseStatusException.getStatusCode(
			).value());

		Mockito.verify(
			basePubsubSubscriber, Mockito.never()
		).receive(
			Mockito.any()
		);
	}

	@Test
	public void testPostThrowsNotFoundWhenTopicIsUnknown() throws Exception {
		BasePubsubSubscriber basePubsubSubscriber = _createSubscriber(
			"other-topic");

		AdminRestController adminRestController = _createController(
			basePubsubSubscriber);

		String json = new JSONObject(
		).put(
			"attributes", ""
		).put(
			"payload", "body"
		).put(
			"topic", "test-topic"
		).toString();

		ResponseStatusException responseStatusException =
			Assertions.assertThrows(
				ResponseStatusException.class,
				() -> adminRestController.postPubsubDispatch(null, json));

		Assertions.assertEquals(
			HttpStatus.NOT_FOUND.value(),
			responseStatusException.getStatusCode(
			).value());

		Mockito.verify(
			basePubsubSubscriber, Mockito.never()
		).receive(
			Mockito.any()
		);
	}

	private AdminRestController _createController(
		BasePubsubSubscriber... basePubsubSubscribers) {

		AdminRestController adminRestController = new AdminRestController();

		ReflectionTestUtils.setField(
			adminRestController, "_basePubsubSubscribers",
			Arrays.asList(basePubsubSubscribers));
		ReflectionTestUtils.setField(
			adminRestController, "_adminPermission",
			Mockito.mock(AdminPermission.class));

		return adminRestController;
	}

	private BasePubsubSubscriber _createSubscriber(String topic) {
		BasePubsubSubscriber basePubsubSubscriber = Mockito.mock(
			BasePubsubSubscriber.class);

		Mockito.when(
			basePubsubSubscriber.getTopic()
		).thenReturn(
			topic
		);

		return basePubsubSubscriber;
	}

}