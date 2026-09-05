/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.google.auth.oauth2.IdToken;
import com.google.auth.oauth2.IdTokenCredentials;
import com.google.auth.oauth2.IdTokenProvider;

import com.liferay.one.exception.GoogleCloudFunctionUnavailableException;
import com.liferay.petra.string.StringBundler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import java.nio.charset.StandardCharsets;

import java.time.Instant;

import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Felipe Veloso
 */
public class GoogleCloudFunctionServiceTest {

	@BeforeEach
	public void setUp() throws Exception {
		_httpServer = HttpServer.create(
			new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);

		_httpServer.createContext(
			"/",
			httpExchange -> {
				_requestURI.set(String.valueOf(httpExchange.getRequestURI()));
				_requestAuthorization.set(
					httpExchange.getRequestHeaders(
					).getFirst(
						"Authorization"
					));

				_respond(httpExchange);
			});

		_httpServer.start();

		Mockito.when(
			_idTokenProvider.idTokenWithAudience(
				Mockito.anyString(), Mockito.any())
		).thenAnswer(
			invocation -> {
				if (_idTokenException != null) {
					throw _idTokenException;
				}

				return IdToken.create(
					_createIdTokenValue(invocation.getArgument(0)));
			}
		);

		_googleCloudFunctionService = new GoogleCloudFunctionService();

		InetSocketAddress inetSocketAddress = _httpServer.getAddress();

		ReflectionTestUtils.setField(
			_googleCloudFunctionService, "_gcfBaseURL",
			"http://localhost:" + inetSocketAddress.getPort());

		ReflectionTestUtils.setField(
			_googleCloudFunctionService, "_idTokenProvider", _idTokenProvider);
	}

	@AfterEach
	public void tearDown() {
		_httpServer.stop(0);
	}

	@Test
	public void testFetchLDPProjectEventHistory() throws Exception {
		_responseBody = _EVENT_HISTORY_RESPONSE;

		Assertions.assertEquals(
			_EVENT_HISTORY_RESPONSE,
			_googleCloudFunctionService.fetchLDPProjectEventHistory(
				"2026-08-31", "month", _SALESFORCE_PROJECT_ID, "2026-06-01"));

		String requestURI = _requestURI.get();

		Assertions.assertTrue(
			requestURI.startsWith(
				"/ldp_metrics_api/api/v1/projects/" + _SALESFORCE_PROJECT_ID +
					"/ldp/usage/event-history"));
		Assertions.assertTrue(requestURI.contains("endDate=2026-08-31"));
		Assertions.assertTrue(requestURI.contains("granularity=month"));
		Assertions.assertTrue(requestURI.contains("startDate=2026-06-01"));
	}

	@Test
	public void testFetchLDPProjectEventSummary() throws Exception {
		_responseBody = _EVENT_SUMMARY_RESPONSE;

		Assertions.assertEquals(
			_EVENT_SUMMARY_RESPONSE,
			_googleCloudFunctionService.fetchLDPProjectEventSummary(
				"2026-08-31", _SALESFORCE_PROJECT_ID, "2026-06-01"));

		String requestURI = _requestURI.get();

		Assertions.assertTrue(
			requestURI.startsWith(
				"/ldp_metrics_api/api/v1/projects/" + _SALESFORCE_PROJECT_ID +
					"/ldp/usage/event-summary"));
		Assertions.assertTrue(requestURI.contains("endDate=2026-08-31"));
		Assertions.assertTrue(requestURI.contains("startDate=2026-06-01"));
	}

	@Test
	public void testFetchLDPProjectUsage() throws Exception {
		_responseBody = _USAGE_RESPONSE;

		Assertions.assertEquals(
			_USAGE_RESPONSE,
			_googleCloudFunctionService.fetchLDPProjectUsage(
				_SALESFORCE_PROJECT_ID));
		Assertions.assertEquals(
			"/ldp_metrics_api/api/v1/projects/" + _SALESFORCE_PROJECT_ID +
				"/ldp/usage",
			_requestURI.get());
	}

	@Test
	public void testFetchLDPProjectUsageSendsBearerAuthorization()
		throws Exception {

		_responseBody = _USAGE_RESPONSE;

		_googleCloudFunctionService.fetchLDPProjectUsage(
			_SALESFORCE_PROJECT_ID);

		Assertions.assertTrue(
			_requestAuthorization.get(
			).startsWith(
				"Bearer eyJ"
			));
	}

	@Test
	public void testFetchLDPProjectUsageWhenIdTokenProviderFails() {
		_idTokenException = new IOException("Unable to mint an ID token");

		GoogleCloudFunctionUnavailableException
			googleCloudFunctionUnavailableException = Assertions.assertThrows(
				GoogleCloudFunctionUnavailableException.class,
				() -> _googleCloudFunctionService.fetchLDPProjectUsage(
					_SALESFORCE_PROJECT_ID));

		Assertions.assertEquals(
			"Unable to authenticate to DataOps for project " +
				_SALESFORCE_PROJECT_ID,
			googleCloudFunctionUnavailableException.getMessage());

		Assertions.assertNull(_requestURI.get());
	}

	@Test
	public void testFetchLDPProjectUsageWhenResponseIsNotFound()
		throws Exception {

		_responseStatus = 404;

		Assertions.assertNull(
			_googleCloudFunctionService.fetchLDPProjectUsage(
				_SALESFORCE_PROJECT_ID));
	}

	@Test
	public void testFetchLDPProjectUsageWhenResponseIsServerError() {
		_responseStatus = 500;

		GoogleCloudFunctionUnavailableException
			googleCloudFunctionUnavailableException = Assertions.assertThrows(
				GoogleCloudFunctionUnavailableException.class,
				() -> _googleCloudFunctionService.fetchLDPProjectUsage(
					_SALESFORCE_PROJECT_ID));

		Assertions.assertEquals(
			"Unable to read DataOps usage for project " +
				_SALESFORCE_PROJECT_ID,
			googleCloudFunctionUnavailableException.getMessage());
	}

	@Test
	public void testGetIdTokenCredentialsCachesOneInstancePerAudience() {
		Assertions.assertSame(
			_getIdTokenCredentials(_AUDIENCE_COMPOSABLE),
			_getIdTokenCredentials(_AUDIENCE_COMPOSABLE));
	}

	@Test
	public void testGetIdTokenCredentialsSeparatesAudiences() {
		Assertions.assertNotSame(
			_getIdTokenCredentials(_AUDIENCE_COMPOSABLE),
			_getIdTokenCredentials(_AUDIENCE_CUSTOMER));
	}

	@Test
	public void testGetIdTokenProviderReusesResolvedProvider() {
		Assertions.assertSame(
			_idTokenProvider,
			ReflectionTestUtils.invokeMethod(
				_googleCloudFunctionService, "_getIdTokenProvider"));
	}

	private String _createIdTokenValue(String audience) {
		Base64.Encoder encoder = Base64.getUrlEncoder(
		).withoutPadding();

		String header = encoder.encodeToString(
			"{\"alg\": \"RS256\", \"typ\": \"JWT\"}".getBytes(
				StandardCharsets.UTF_8));

		Instant instant = Instant.now();

		String payload = encoder.encodeToString(
			StringBundler.concat(
				"{\"aud\": \"", audience, "\", \"exp\": ",
				instant.getEpochSecond() + 3600, ", \"iat\": ",
				instant.getEpochSecond(),
				", \"iss\": \"https://accounts.google.com\", \"sub\": \"1\"}"
			).getBytes(
				StandardCharsets.UTF_8
			));

		String signature = encoder.encodeToString(
			"signature".getBytes(StandardCharsets.UTF_8));

		return StringBundler.concat(header, ".", payload, ".", signature);
	}

	private IdTokenCredentials _getIdTokenCredentials(String audience) {
		return ReflectionTestUtils.invokeMethod(
			_googleCloudFunctionService, "_getIdTokenCredentials", audience);
	}

	private void _respond(HttpExchange httpExchange) throws IOException {
		byte[] bytes = _responseBody.getBytes(StandardCharsets.UTF_8);

		httpExchange.getResponseHeaders(
		).set(
			"Content-Type", "application/json"
		);

		httpExchange.sendResponseHeaders(_responseStatus, bytes.length);

		try (OutputStream outputStream = httpExchange.getResponseBody()) {
			outputStream.write(bytes);
		}
	}

	private static final String _AUDIENCE_COMPOSABLE =
		"https://example.com/composable_usage_api";

	private static final String _AUDIENCE_CUSTOMER =
		"https://example.com/customer_usage_api";

	private static final String _EVENT_HISTORY_RESPONSE =
		"{\"eventHistory\": [{\"date\": \"2026-06-01\"}]}";

	private static final String _EVENT_SUMMARY_RESPONSE =
		"{\"eventSummary\": [{\"eventsCount\": 13000}]}";

	private static final String _SALESFORCE_PROJECT_ID = "a0B0g00000eABCD123";

	private static final String _USAGE_RESPONSE =
		"{\"apiRequestsCount\": 45000}";

	private GoogleCloudFunctionService _googleCloudFunctionService;
	private HttpServer _httpServer;
	private IOException _idTokenException;
	private final IdTokenProvider _idTokenProvider = Mockito.mock(
		IdTokenProvider.class);
	private final AtomicReference<String> _requestAuthorization =
		new AtomicReference<>();
	private final AtomicReference<String> _requestURI = new AtomicReference<>();
	private volatile String _responseBody = "{}";
	private volatile int _responseStatus = 200;

}