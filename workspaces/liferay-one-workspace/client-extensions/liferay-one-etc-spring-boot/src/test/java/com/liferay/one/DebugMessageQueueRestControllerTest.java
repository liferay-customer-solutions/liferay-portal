/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.liferay.one.permission.DebugMessageQueuePermission;
import com.liferay.one.pubsub.Message;
import com.liferay.one.pubsub.subscriber.BasePubsubSubscriber;
import com.liferay.portal.kernel.security.auth.PrincipalException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONObject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author Karoline Silva
 */
public class DebugMessageQueueRestControllerTest {

	@Test
	public void testHandleExceptionReturnsInternalServerError() {
		DebugMessageQueueRestController debugMessageQueueRestController =
			_createController();

		ResponseEntity<ProblemDetail> responseEntity =
			debugMessageQueueRestController.handleException(new Exception());

		Assertions.assertEquals(
			HttpStatus.INTERNAL_SERVER_ERROR.value(),
			responseEntity.getStatusCode(
			).value());
	}

	@Test
	public void testHandlePrincipalExceptionReturnsForbidden() {
		DebugMessageQueueRestController debugMessageQueueRestController =
			_createController();

		ResponseEntity<ProblemDetail> responseEntity =
			debugMessageQueueRestController.handleException(
				new PrincipalException());

		Assertions.assertEquals(
			HttpStatus.FORBIDDEN.value(),
			responseEntity.getStatusCode(
			).value());
	}

	@Test
	public void testHandleResponseStatusExceptionReturnsStatusAndDetail() {
		DebugMessageQueueRestController debugMessageQueueRestController =
			_createController();

		ResponseEntity<ProblemDetail> responseEntity =
			debugMessageQueueRestController.handleException(
				new ResponseStatusException(HttpStatus.NOT_FOUND, "nope"));

		Assertions.assertEquals(
			HttpStatus.NOT_FOUND.value(),
			responseEntity.getStatusCode(
			).value());

		ProblemDetail problemDetail = responseEntity.getBody();

		Assertions.assertEquals("nope", problemDetail.getDetail());
	}

	@Test
	public void testPostDispatchesMessageToMatchingSubscriber()
		throws Exception {

		TestPubsubSubscriber testPubsubSubscriber = new TestPubsubSubscriber(
			"test-topic", null);

		DebugMessageQueueRestController debugMessageQueueRestController =
			_createController(testPubsubSubscriber);

		String json = new JSONObject(
		).put(
			"message", "line1\nline2"
		).put(
			"properties", "a=1\nb=2"
		).put(
			"routingKey", "test-topic"
		).toString();

		ResponseEntity<Void> responseEntity =
			debugMessageQueueRestController.post(null, json);

		Assertions.assertEquals(
			HttpStatus.OK.value(),
			responseEntity.getStatusCode(
			).value());

		Message message = testPubsubSubscriber.getReceivedMessage();

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
		TestPubsubSubscriber testPubsubSubscriber = new TestPubsubSubscriber(
			"test-topic", null);

		DebugMessageQueueRestController debugMessageQueueRestController =
			_createController(testPubsubSubscriber);

		String json = new JSONObject(
		).put(
			"message", "body"
		).put(
			"properties", "url=https://example.com?a=1&b=2"
		).put(
			"routingKey", "test-topic"
		).toString();

		debugMessageQueueRestController.post(null, json);

		Message message = testPubsubSubscriber.getReceivedMessage();

		Assertions.assertEquals(
			"https://example.com?a=1&b=2", message.get("url"));
	}

	@Test
	public void testPostThrowsBadGatewayWhenDispatchFails() {
		TestPubsubSubscriber testPubsubSubscriber = new TestPubsubSubscriber(
			"test-topic", new RuntimeException("downstream boom"));

		DebugMessageQueueRestController debugMessageQueueRestController =
			_createController(testPubsubSubscriber);

		String json = new JSONObject(
		).put(
			"message", "body"
		).put(
			"properties", ""
		).put(
			"routingKey", "test-topic"
		).toString();

		ResponseStatusException responseStatusException =
			Assertions.assertThrows(
				ResponseStatusException.class,
				() -> debugMessageQueueRestController.post(null, json));

		Assertions.assertEquals(
			HttpStatus.BAD_GATEWAY.value(),
			responseStatusException.getStatusCode(
			).value());
	}

	@Test
	public void testPostThrowsBadRequestWhenPropertiesAreMalformed() {
		TestPubsubSubscriber testPubsubSubscriber = new TestPubsubSubscriber(
			"test-topic", null);

		DebugMessageQueueRestController debugMessageQueueRestController =
			_createController(testPubsubSubscriber);

		String json = new JSONObject(
		).put(
			"message", "body"
		).put(
			"properties", "missing-delimiter"
		).put(
			"routingKey", "test-topic"
		).toString();

		ResponseStatusException responseStatusException =
			Assertions.assertThrows(
				ResponseStatusException.class,
				() -> debugMessageQueueRestController.post(null, json));

		Assertions.assertEquals(
			HttpStatus.BAD_REQUEST.value(),
			responseStatusException.getStatusCode(
			).value());

		Assertions.assertEquals(0, testPubsubSubscriber.getReceiveCount());
	}

	@Test
	public void testPostThrowsNotFoundWhenRoutingKeyIsUnknown() {
		TestPubsubSubscriber testPubsubSubscriber = new TestPubsubSubscriber(
			"other-topic", null);

		DebugMessageQueueRestController debugMessageQueueRestController =
			_createController(testPubsubSubscriber);

		String json = new JSONObject(
		).put(
			"message", "body"
		).put(
			"properties", ""
		).put(
			"routingKey", "test-topic"
		).toString();

		ResponseStatusException responseStatusException =
			Assertions.assertThrows(
				ResponseStatusException.class,
				() -> debugMessageQueueRestController.post(null, json));

		Assertions.assertEquals(
			HttpStatus.NOT_FOUND.value(),
			responseStatusException.getStatusCode(
			).value());

		Assertions.assertEquals(0, testPubsubSubscriber.getReceiveCount());
	}

	private DebugMessageQueueRestController _createController(
		BasePubsubSubscriber... basePubsubSubscribers) {

		List<BasePubsubSubscriber> basePubsubSubscriberList = Arrays.asList(
			basePubsubSubscribers);

		return new DebugMessageQueueRestController(
			basePubsubSubscriberList,
			Mockito.mock(DebugMessageQueuePermission.class));
	}

	private static class TestPubsubSubscriber extends BasePubsubSubscriber {

		public TestPubsubSubscriber(
			String topic, RuntimeException runtimeException) {

			_topic = topic;
			_runtimeException = runtimeException;
		}

		public int getReceiveCount() {
			return _receiveCount;
		}

		public Message getReceivedMessage() {
			return _receivedMessage;
		}

		@Override
		public String getTopic() {
			return _topic;
		}

		@Override
		protected String getProjectId() {
			return null;
		}

		@Override
		protected void receive(Message message) throws Exception {
			_receiveCount++;

			if (_runtimeException != null) {
				throw _runtimeException;
			}

			_receivedMessage = message;
		}

		private int _receiveCount;
		private Message _receivedMessage;
		private final RuntimeException _runtimeException;
		private final String _topic;

	}

}