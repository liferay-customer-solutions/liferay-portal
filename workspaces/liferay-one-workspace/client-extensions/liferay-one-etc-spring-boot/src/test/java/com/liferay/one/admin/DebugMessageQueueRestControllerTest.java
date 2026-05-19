/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * @author Ryan Schuhler
 */
@ExtendWith(MockitoExtension.class)
public class DebugMessageQueueRestControllerTest {

	@BeforeEach
	public void setUp() {
		DebugMessageQueueService service = new DebugMessageQueueService(
			"ebenezer-support-opportunity-entries:salesforce-opportunities",
			_pubSubPublisher);

		_controller = new DebugMessageQueueRestController(service) {

			@Override
			protected List<String> getUserRoleNames(Jwt jwt) {
				return List.of("Liferay Staff");
			}

		};
	}

	@Test
	public void testPostHappyPath() throws Exception {
		String json =
			"{\"routingKey\":\"ebenezer-support-opportunity-entries\"," +
				"\"message\":\"hello\\nworld\"," +
					"\"properties\":\"k1=v1\\nk2=v2\"}";

		ResponseEntity<String> response = _controller.post(_jwt, json);

		Assertions.assertThat(
			response.getStatusCode()
		).isEqualTo(
			HttpStatus.OK
		);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, String>> propsCaptor =
			ArgumentCaptor.forClass(Map.class);

		Mockito.verify(
			_pubSubPublisher
		).publish(
			ArgumentMatchers.eq("salesforce-opportunities"),
			ArgumentMatchers.eq("helloworld"), propsCaptor.capture()
		);

		Map<String, String> capturedProps = propsCaptor.getValue();

		Assertions.assertThat(
			capturedProps
		).containsEntry(
			"k1", "v1"
		).containsEntry(
			"k2", "v2"
		);

		Assertions.assertThat(
			new ArrayList<>(capturedProps.keySet())
		).containsExactly(
			"k1", "k2"
		);
	}

	@Test
	public void testPostMalformedProperties() {
		String json =
			"{\"routingKey\":\"ebenezer-support-opportunity-entries\"," +
				"\"message\":\"test\",\"properties\":\"no-equals-sign\"}";

		ResponseEntity<String> response = _controller.post(_jwt, json);

		Assertions.assertThat(
			response.getStatusCode()
		).isEqualTo(
			HttpStatus.BAD_REQUEST
		);

		Assertions.assertThat(
			response.getBody()
		).contains(
			"MALFORMED_PROPERTIES"
		);
	}

	@Test
	public void testPostPublishFailure() throws Exception {
		Mockito.doThrow(
			new RuntimeException("Pub/Sub unavailable")
		).when(
			_pubSubPublisher
		).publish(
			ArgumentMatchers.any(), ArgumentMatchers.any(),
			ArgumentMatchers.any()
		);

		String json =
			"{\"routingKey\":\"ebenezer-support-opportunity-entries\"," +
				"\"message\":\"test\",\"properties\":\"\"}";

		ResponseEntity<String> response = _controller.post(_jwt, json);

		Assertions.assertThat(
			response.getStatusCode()
		).isEqualTo(
			HttpStatus.BAD_GATEWAY
		);

		Assertions.assertThat(
			response.getBody()
		).contains(
			"PUBLISH_FAILURE"
		);
	}

	@Test
	public void testPostUnknownRoutingKey() {
		String json =
			"{\"routingKey\":\"unknown-key\",\"message\":\"test\"," +
				"\"properties\":\"\"}";

		ResponseEntity<String> response = _controller.post(_jwt, json);

		Assertions.assertThat(
			response.getStatusCode()
		).isEqualTo(
			HttpStatus.BAD_REQUEST
		);

		Assertions.assertThat(
			response.getBody()
		).contains(
			"UNKNOWN_ROUTING_KEY"
		);
	}

	private DebugMessageQueueRestController _controller;

	@Mock
	private Jwt _jwt;

	@Mock
	private PubSubPublisher _pubSubPublisher;

}