/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.jira.synchronizer;

import com.liferay.petra.function.UnsafeRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Assertions;

import org.mockito.stubbing.Answer;

/**
 * Asserts that two operations serialize on a shared
 * {@link com.liferay.one.jira.util.KeyedLock} key: the first operation
 * blocks inside a call stubbed with {@link #block}, the second operation is
 * started while the first is still blocked and must not record any event
 * until the first is released, and the recorded events must land in the
 * expected order. Use one instance per test.
 *
 * @author Drew Brokke
 */
public class LockSerializationTestHelper {

	public void assertSerialized(
			UnsafeRunnable<Exception> firstUnsafeRunnable,
			UnsafeRunnable<Exception> secondUnsafeRunnable,
			String... expectedEvents)
		throws Exception {

		Thread firstThread = _startThread(firstUnsafeRunnable);

		Assertions.assertTrue(_enteredLatch.await(10, TimeUnit.SECONDS));

		Thread secondThread = _startThread(secondUnsafeRunnable);

		secondThread.join(200);

		Assertions.assertEquals(Collections.emptyList(), _events);

		_releaseLatch.countDown();

		firstThread.join(10000);
		secondThread.join(10000);

		if (!_throwables.isEmpty()) {
			Assertions.fail(_throwables.get(0));
		}

		Assertions.assertEquals(Arrays.asList(expectedEvents), _events);
	}

	public Answer<Object> block(String event) {
		return invocation -> {
			if (_blocked.compareAndSet(false, true)) {
				_enteredLatch.countDown();

				_releaseLatch.await(10, TimeUnit.SECONDS);
			}

			_events.add(event);

			return null;
		};
	}

	public Answer<Object> record(String event) {
		return invocation -> {
			_events.add(event);

			return null;
		};
	}

	private Thread _startThread(UnsafeRunnable<Exception> unsafeRunnable) {
		Thread thread = new Thread(
			() -> {
				try {
					unsafeRunnable.run();
				}
				catch (Throwable throwable) {
					_throwables.add(throwable);
				}
			});

		thread.start();

		return thread;
	}

	private final AtomicBoolean _blocked = new AtomicBoolean();
	private final CountDownLatch _enteredLatch = new CountDownLatch(1);
	private final List<String> _events = Collections.synchronizedList(
		new ArrayList<>());
	private final CountDownLatch _releaseLatch = new CountDownLatch(1);
	private final List<Throwable> _throwables = Collections.synchronizedList(
		new ArrayList<>());

}