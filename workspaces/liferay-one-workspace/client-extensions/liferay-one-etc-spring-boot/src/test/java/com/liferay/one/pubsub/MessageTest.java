/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.pubsub;

import com.liferay.portal.kernel.util.HashMapBuilder;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Felipe Franca
 */
public class MessageTest {

	@Test
	public void testGetAttributesReturnsEmptyMapWhenAttributesAreNull() {
		Message message = new Message(null, "payload", "test-topic");

		Map<String, String> attributes = message.getAttributes();

		Assertions.assertTrue(attributes.isEmpty());
	}

	@Test
	public void testGetAttributesReturnsUnmodifiableMap() {
		Message message = new Message(
			Map.of("key", "value"), "payload", "test-topic");

		Map<String, String> attributes = message.getAttributes();

		Assertions.assertThrows(
			UnsupportedOperationException.class,
			() -> attributes.put("key", "other value"));
	}

	@Test
	public void testGetReturnsNullWhenAttributesAreNull() {
		Message message = new Message(null, "payload", "test-topic");

		Assertions.assertNull(message.get("key"));
	}

	@Test
	public void testPutCreatesAttributesWhenAbsent() {
		Message message = new Message(null, "payload", "test-topic");

		message.put("key", "value");

		Assertions.assertEquals("value", message.get("key"));
	}

	@Test
	public void testSetAttributesCopiesSourceMap() {
		Map<String, String> attributes = HashMapBuilder.put(
			"key", "value"
		).build();

		Message message = new Message(attributes, "payload", "test-topic");

		attributes.put("key", "mutated value");
		attributes.put("otherKey", "other value");

		Assertions.assertEquals("value", message.get("key"));
		Assertions.assertNull(message.get("otherKey"));
	}

	@Test
	public void testSetAttributesToNullClearsAttributes() {
		Message message = new Message(
			Map.of("key", "value"), "payload", "test-topic");

		message.setAttributes(null);

		Assertions.assertNull(message.get("key"));

		Map<String, String> attributes = message.getAttributes();

		Assertions.assertTrue(attributes.isEmpty());
	}

	@Test
	public void testToStringIncludesTopicAttributesAndPayload() {
		Message message = new Message(
			Map.of("key", "value"), "payload", "test-topic");

		Assertions.assertEquals(
			"{topic=test-topic, attributes={key=value}, payload=payload}",
			message.toString());
	}

}