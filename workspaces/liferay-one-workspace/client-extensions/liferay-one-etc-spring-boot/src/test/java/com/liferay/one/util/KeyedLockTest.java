/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Drew Brokke
 */
public class KeyedLockTest {

	@BeforeEach
	public void setUp() {
		_keyedLock = new KeyedLock();
	}

	@Test
	public void testWithLockIsReentrant() throws Exception {
		List<String> events = new ArrayList<>();

		_keyedLock.withLock(
			"key", () -> _keyedLock.withLock("key", () -> events.add("ran")));

		Assertions.assertEquals(Collections.singletonList("ran"), events);
	}

	@Test
	public void testWithLockReleasesLockAfterException() throws Exception {
		Exception exception = Assertions.assertThrows(
			Exception.class,
			() -> _keyedLock.withLock(
				"key",
				() -> {
					throw new Exception("expected");
				}));

		Assertions.assertEquals("expected", exception.getMessage());

		List<String> events = Collections.synchronizedList(new ArrayList<>());

		Thread thread = new Thread(
			() -> _keyedLock.withLock("key", () -> events.add("ran")));

		thread.start();

		thread.join(10000);

		Assertions.assertEquals(Collections.singletonList("ran"), events);
	}

	@Test
	public void testWithLockRunsWithNullKey() throws Exception {
		List<String> events = new ArrayList<>();

		_keyedLock.withLock(null, () -> events.add("ran"));

		Assertions.assertEquals(Collections.singletonList("ran"), events);
	}

	@Test
	public void testWithLockSerializesSameKey() throws Exception {
		CountDownLatch enteredLatch = new CountDownLatch(1);
		List<String> events = Collections.synchronizedList(new ArrayList<>());
		CountDownLatch releaseLatch = new CountDownLatch(1);

		Thread firstThread = new Thread(
			() -> {
				try {
					_keyedLock.withLock(
						"key",
						() -> {
							enteredLatch.countDown();

							releaseLatch.await(10, TimeUnit.SECONDS);

							events.add("first");
						});
				}
				catch (InterruptedException interruptedException) {
					Thread currentThread = Thread.currentThread();

					currentThread.interrupt();
				}
			});

		firstThread.start();

		Assertions.assertTrue(enteredLatch.await(10, TimeUnit.SECONDS));

		Thread secondThread = new Thread(
			() -> _keyedLock.withLock("key", () -> events.add("second")));

		secondThread.start();

		secondThread.join(200);

		Assertions.assertEquals(Collections.emptyList(), events);

		releaseLatch.countDown();

		firstThread.join(10000);
		secondThread.join(10000);

		Assertions.assertEquals(Arrays.asList("first", "second"), events);
	}

	private KeyedLock _keyedLock;

}