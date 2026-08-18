/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.pubsub.subscriber;

import com.liferay.one.pubsub.Message;
import com.liferay.one.service.NotificationQueueEntryService;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Felipe Franca
 */
public class DeadLetterPubsubSubscriberTest {

	@BeforeEach
	public void setUp() {
		_subscriber = new DeadLetterPubsubSubscriber();

		_notificationQueueEntryService = Mockito.mock(
			NotificationQueueEntryService.class);

		ReflectionTestUtils.setField(
			_subscriber, "_emailAddressGlobal", _EMAIL_ADDRESS_GLOBAL);
		ReflectionTestUtils.setField(
			_subscriber, "_notificationQueueEntryService",
			_notificationQueueEntryService);
		ReflectionTestUtils.setField(
			_subscriber, "_notificationRecipient", _NOTIFICATION_RECIPIENT);
		ReflectionTestUtils.setField(_subscriber, "_projectId", "test-project");
		ReflectionTestUtils.setField(
			_subscriber, "_subscription", "test-subscription");
	}

	@Test
	public void testAttributeNameConstantsMatchGCPDeadLetterAttributes() {
		Assertions.assertEquals(
			"CloudPubSubDeadLetterSourceDeliveryCount",
			BaseDeadLetterPubsubSubscriber.
				SOURCE_DELIVERY_COUNT_ATTRIBUTE_NAME);
		Assertions.assertEquals(
			"CloudPubSubDeadLetterSourceSubscription",
			BaseDeadLetterPubsubSubscriber.SOURCE_SUBSCRIPTION_ATTRIBUTE_NAME);
	}

	@Test
	public void testGetTopicReturnsDeadLetterTopic() {
		Assertions.assertEquals(
			"one-liferay-dead-letter", _subscriber.getTopic());
	}

	@Test
	public void testIsAutoCreateTopicReturnsFalse() {
		Assertions.assertFalse(_subscriber.isAutoCreateTopic());
	}

	@Test
	public void testIsDeadLetterTopicEnabledReturnsFalse() {
		Assertions.assertFalse(_subscriber.isDeadLetterTopicEnabled());
	}

	@Test
	public void testOnDeadLetterDefaultsToZeroAttemptsWhenAttributesAbsent()
		throws Exception {

		Message message = new Message(
			Collections.emptyMap(), "{\"a\":\"value\"}", "test-topic");

		Assertions.assertDoesNotThrow(() -> _subscriber.receive(message));

		ArgumentCaptor<String> bodyArgumentCaptor = ArgumentCaptor.forClass(
			String.class);

		Mockito.verify(
			_notificationQueueEntryService
		).addNotificationQueueEntry(
			Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
			bodyArgumentCaptor.capture()
		);

		Assertions.assertTrue(
			bodyArgumentCaptor.getValue(
			).contains(
				"0 delivery attempts"
			));
	}

	@Test
	public void testOnDeadLetterDefaultsToZeroAttemptsWhenDeliveryCountIsNotNumeric()
		throws Exception {

		Assertions.assertDoesNotThrow(
			() -> _subscriber.receive(
				_createMessage("not a number", _SOURCE_SUBSCRIPTION)));

		ArgumentCaptor<String> bodyArgumentCaptor = ArgumentCaptor.forClass(
			String.class);

		Mockito.verify(
			_notificationQueueEntryService
		).addNotificationQueueEntry(
			Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
			bodyArgumentCaptor.capture()
		);

		Assertions.assertTrue(
			bodyArgumentCaptor.getValue(
			).contains(
				"0 delivery attempts"
			));
	}

	@Test
	public void testOnDeadLetterDropsMessageWhenRecipientBlank()
		throws Exception {

		ReflectionTestUtils.setField(_subscriber, "_notificationRecipient", "");

		Assertions.assertDoesNotThrow(
			() -> _subscriber.receive(
				_createMessage("3", _SOURCE_SUBSCRIPTION)));

		Mockito.verifyNoInteractions(_notificationQueueEntryService);
	}

	@Test
	public void testOnDeadLetterEscapesPayloadInNotificationBody()
		throws Exception {

		Message message = new Message(
			Map.of(
				BaseDeadLetterPubsubSubscriber.
					SOURCE_DELIVERY_COUNT_ATTRIBUTE_NAME,
				"3",
				BaseDeadLetterPubsubSubscriber.
					SOURCE_SUBSCRIPTION_ATTRIBUTE_NAME,
				_SOURCE_SUBSCRIPTION),
			"{\"a\":\"<script>alert(1)</script>\"}", "test-topic");

		_subscriber.receive(message);

		ArgumentCaptor<String> bodyArgumentCaptor = ArgumentCaptor.forClass(
			String.class);

		Mockito.verify(
			_notificationQueueEntryService
		).addNotificationQueueEntry(
			Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
			bodyArgumentCaptor.capture()
		);

		String body = bodyArgumentCaptor.getValue();

		Assertions.assertFalse(body.contains("<script>"));
		Assertions.assertTrue(body.contains("&lt;script&gt;"));
	}

	@Test
	public void testOnDeadLetterIncludesEscapedAttributesInNotificationBody()
		throws Exception {

		Message message = new Message(
			Map.of(
				BaseDeadLetterPubsubSubscriber.
					SOURCE_DELIVERY_COUNT_ATTRIBUTE_NAME,
				"3",
				BaseDeadLetterPubsubSubscriber.
					SOURCE_SUBSCRIPTION_ATTRIBUTE_NAME,
				_SOURCE_SUBSCRIPTION, "customAttribute", "<bold>"),
			"{\"a\":\"value\"}", "test-topic");

		_subscriber.receive(message);

		ArgumentCaptor<String> bodyArgumentCaptor = ArgumentCaptor.forClass(
			String.class);

		Mockito.verify(
			_notificationQueueEntryService
		).addNotificationQueueEntry(
			Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
			bodyArgumentCaptor.capture()
		);

		String body = bodyArgumentCaptor.getValue();

		Assertions.assertTrue(body.contains("customAttribute=&lt;bold&gt;"));
		Assertions.assertFalse(body.contains("<bold>"));
	}

	@Test
	public void testOnDeadLetterSendsNotificationEmailWithAttemptCountAndSourceSubscription()
		throws Exception {

		_subscriber.receive(_createMessage("3", _SOURCE_SUBSCRIPTION));

		ArgumentCaptor<String> bodyArgumentCaptor = ArgumentCaptor.forClass(
			String.class);

		Mockito.verify(
			_notificationQueueEntryService
		).addNotificationQueueEntry(
			Mockito.eq(_EMAIL_ADDRESS_GLOBAL), Mockito.eq("Liferay One"),
			Mockito.eq(_NOTIFICATION_RECIPIENT),
			Mockito.eq("Liferay One Dead Letter Notification"),
			bodyArgumentCaptor.capture()
		);

		String body = bodyArgumentCaptor.getValue();

		Assertions.assertTrue(body.contains("3 delivery attempts"));
		Assertions.assertTrue(body.contains(_SOURCE_SUBSCRIPTION));
	}

	@Test
	public void testOnDeadLetterSwallowsNotificationFailure() throws Exception {
		Mockito.doThrow(
			new RuntimeException("Unable to add notification queue entry")
		).when(
			_notificationQueueEntryService
		).addNotificationQueueEntry(
			Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
			Mockito.any()
		);

		Assertions.assertDoesNotThrow(
			() -> _subscriber.receive(
				_createMessage("3", _SOURCE_SUBSCRIPTION)));
	}

	private Message _createMessage(
		String deliveryCount, String sourceSubscription) {

		return new Message(
			Map.of(
				BaseDeadLetterPubsubSubscriber.
					SOURCE_DELIVERY_COUNT_ATTRIBUTE_NAME,
				deliveryCount,
				BaseDeadLetterPubsubSubscriber.
					SOURCE_SUBSCRIPTION_ATTRIBUTE_NAME,
				sourceSubscription),
			"{\"a\":\"value\"}", "test-topic");
	}

	private static final String _EMAIL_ADDRESS_GLOBAL = "global@example.com";

	private static final String _NOTIFICATION_RECIPIENT = "oncall@example.com";

	private static final String _SOURCE_SUBSCRIPTION =
		"projects/test-project/subscriptions/source-subscription";

	private NotificationQueueEntryService _notificationQueueEntryService;
	private DeadLetterPubsubSubscriber _subscriber;

}