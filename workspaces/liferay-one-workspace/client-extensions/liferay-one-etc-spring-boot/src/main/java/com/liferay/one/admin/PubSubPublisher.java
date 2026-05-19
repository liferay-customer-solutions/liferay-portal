/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.admin;

import com.google.api.core.ApiFuture;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;

import java.io.ByteArrayInputStream;

import java.nio.charset.StandardCharsets;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author Ryan Schuhler
 */
@Service
public class PubSubPublisher {

	public void publish(
			String topicName, String message, Map<String, String> attributes)
		throws Exception {

		if (_serviceAccountKey.isBlank()) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					"Skipping Pub/Sub publish — no service account key " +
						"configured. Topic: " + topicName);
			}

			return;
		}

		Publisher publisher = _publishers.computeIfAbsent(
			topicName, this::_createPublisher);

		PubsubMessage pubsubMessage = PubsubMessage.newBuilder(
		).setData(
			ByteString.copyFromUtf8(message)
		).putAllAttributes(
			attributes
		).build();

		ApiFuture<String> future = publisher.publish(pubsubMessage);

		future.get();
	}

	@PreDestroy
	public void tearDown() {
		for (Publisher publisher : _publishers.values()) {
			publisher.shutdown();
		}
	}

	private Publisher _createPublisher(String topicName) {
		try {
			return Publisher.newBuilder(
				TopicName.of(_projectId, topicName)
			).setCredentialsProvider(
				FixedCredentialsProvider.create(
					ServiceAccountCredentials.fromStream(
						new ByteArrayInputStream(
							_serviceAccountKey.getBytes(StandardCharsets.UTF_8))
					).createScoped(
						Collections.singletonList(
							"https://www.googleapis.com/auth/cloud-platform")
					))
			).build();
		}
		catch (Exception exception) {
			throw new RuntimeException(
				"Failed to create publisher for topic: " + topicName,
				exception);
		}
	}

	private static final Log _log = LogFactory.getLog(PubSubPublisher.class);

	@Value("${liferay.one.admin.debug.message.queue.gcp.project.id:}")
	private String _projectId;

	private final Map<String, Publisher> _publishers =
		new ConcurrentHashMap<>();

	@Value("${liferay.one.admin.debug.message.queue.gcp.service.account.key:}")
	private String _serviceAccountKey;

}