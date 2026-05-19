/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.admin;

import com.liferay.portal.kernel.util.StringUtil;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author Ryan Schuhler
 */
@Service
public class DebugMessageQueueService {

	public DebugMessageQueueService(
		@Value("${liferay.one.admin.debug.message.queue.routing.keys:}") String
			routingKeysConfig,
		PubSubPublisher pubSubPublisher) {

		_pubSubPublisher = pubSubPublisher;

		_routingKeyToTopicMap = _parseRoutingKeysConfig(routingKeysConfig);
	}

	public Map<String, String> getRoutingKeyToTopicMap() {
		return Collections.unmodifiableMap(_routingKeyToTopicMap);
	}

	public void publish(
			String routingKey, String message, String propertiesText)
		throws Exception {

		String topicName = _routingKeyToTopicMap.get(routingKey);

		if (topicName == null) {
			throw new UnknownRoutingKeyException(routingKey);
		}

		Map<String, String> propertiesMap = _parseProperties(propertiesText);

		String cleanMessage = StringUtil.replace(
			message, new String[] {"\r\n", "\n", "\r"},
			new String[] {"", "", ""});

		_pubSubPublisher.publish(topicName, cleanMessage, propertiesMap);
	}

	public static class MalformedPropertiesException extends Exception {

		public MalformedPropertiesException(String line) {
			super("Malformed property (missing '='): " + line);
		}

	}

	public static class UnknownRoutingKeyException extends Exception {

		public UnknownRoutingKeyException(String routingKey) {
			super("No topic configured for routing key: " + routingKey);
		}

	}

	private LinkedHashMap<String, String> _parseProperties(String text)
		throws Exception {

		LinkedHashMap<String, String> map = new LinkedHashMap<>();

		if ((text == null) || text.isBlank()) {
			return map;
		}

		for (String rawLine : text.split("\n")) {
			String lineString = rawLine.trim();

			if (lineString.isEmpty()) {
				continue;
			}

			int index = lineString.indexOf('=');

			if (index < 0) {
				throw new MalformedPropertiesException(lineString);
			}

			map.put(
				lineString.substring(0, index),
				lineString.substring(index + 1));
		}

		return map;
	}

	private Map<String, String> _parseRoutingKeysConfig(String config) {
		Map<String, String> map = new LinkedHashMap<>();

		if ((config == null) || config.isBlank()) {
			return map;
		}

		for (String rawEntry : config.split(",")) {
			String entryString = rawEntry.trim();

			if (entryString.isEmpty()) {
				continue;
			}

			String[] parts = entryString.split(":", 2);

			if (parts.length == 2) {
				map.put(parts[0].trim(), parts[1].trim());
			}
		}

		return map;
	}

	private final PubSubPublisher _pubSubPublisher;
	private final Map<String, String> _routingKeyToTopicMap;

}