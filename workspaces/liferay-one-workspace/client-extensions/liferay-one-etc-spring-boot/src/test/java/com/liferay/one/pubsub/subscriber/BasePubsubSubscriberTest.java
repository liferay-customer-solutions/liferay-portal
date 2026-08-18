/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.pubsub.subscriber;

import com.liferay.one.pubsub.Message;
import com.liferay.petra.string.StringPool;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Felipe Franca
 */
public class BasePubsubSubscriberTest {

	@Test
	public void testInitializeDoesNotStartSubscriberWhenProjectIdIsBlank() {
		TestPubsubSubscriber testPubsubSubscriber = new TestPubsubSubscriber();

		Assertions.assertDoesNotThrow(testPubsubSubscriber::initialize);

		Assertions.assertNull(
			ReflectionTestUtils.getField(testPubsubSubscriber, "_subscriber"));
	}

	@Test
	public void testStopDoesNothingWhenSubscriberWasNeverStarted() {
		TestPubsubSubscriber testPubsubSubscriber = new TestPubsubSubscriber();

		Assertions.assertDoesNotThrow(testPubsubSubscriber::stop);
	}

	private static class TestPubsubSubscriber extends BasePubsubSubscriber {

		@Override
		public void receive(Message message) {
		}

		@Override
		protected String getProjectId() {
			return StringPool.BLANK;
		}

	}

}