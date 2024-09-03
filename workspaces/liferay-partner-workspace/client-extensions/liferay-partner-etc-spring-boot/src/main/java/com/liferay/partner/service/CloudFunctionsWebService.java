/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.partner.service;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.IdTokenCredentials;
import com.google.auth.oauth2.IdTokenProvider;
import com.google.common.io.CharStreams;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.nio.charset.StandardCharsets;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Felipe Franca
 */
@Component
public class CloudFunctionsWebService {

	public JSONObject getItems(String uri) throws Exception {
		JSONObject gcpServiceAccountKeyJSONObject = new JSONObject();

		gcpServiceAccountKeyJSONObject.put(
			"auth_provider_x509_cert_url",
			_gcpServiceAccountKeyAuthProviderCertURL
		).put(
			"auth_uri", _gcpServiceAccountKeyAuthURI
		).put(
			"client_email", _gcpServiceAccountKeyClientEmail
		).put(
			"client_id", _gcpServiceAccountKeyClientId
		).put(
			"client_x509_cert_url", _gcpServiceAccountKeyClientCertURL
		).put(
			"private_key", _gcpServiceAccountKeyPrivateKey
		).put(
			"private_key_id", _gcpServiceAccountKeyPrivateKeyId
		).put(
			"project_id", _gcpServiceAccountKeyProjectId
		).put(
			"token_uri", _gcpServiceAccountKeyTokenURI
		).put(
			"type", _gcpServiceAccountKeyType
		).put(
			"universe_domain", _gcpServiceAccountKeyUniverseDomain
		);

		try (InputStream inputStream = new ByteArrayInputStream(
				gcpServiceAccountKeyJSONObject.toString(
				).getBytes())) {

			GoogleCredentials credentials = GoogleCredentials.fromStream(
				inputStream);

			IdTokenCredentials tokenCredential = IdTokenCredentials.newBuilder(
			).setIdTokenProvider(
				(IdTokenProvider)credentials
			).setTargetAudience(
				_cloudFunctionsBaseUrl
			).build();

			GenericUrl genericUrl = new GenericUrl(
				_cloudFunctionsBaseUrl + uri);
			HttpCredentialsAdapter adapter = new HttpCredentialsAdapter(
				tokenCredential);
			HttpTransport transport = new NetHttpTransport();

			HttpRequest httpRequest = transport.createRequestFactory(
				adapter
			).buildGetRequest(
				genericUrl
			);

			HttpResponse httpResponse = httpRequest.execute();

			JSONObject jsonObject;

			try {
				String result = CharStreams.toString(
					new InputStreamReader(
						httpResponse.getContent(), StandardCharsets.UTF_8));

				jsonObject = new JSONObject(result);
			}
			finally {
				httpResponse.disconnect();
			}

			return jsonObject;
		}
	}

	@Value("${liferay.partner.cloud.functions.base.url}")
	private String _cloudFunctionsBaseUrl;

	@Value(
		"${liferay.partner.gcp.service.account.key.auth-provider-x509-cert-url}"
	)
	private String _gcpServiceAccountKeyAuthProviderCertURL;

	@Value("${liferay.partner.gcp.service.account.key.auth-uri}")
	private String _gcpServiceAccountKeyAuthURI;

	@Value("${liferay.partner.gcp.service.account.key.client-x509-cert-url}")
	private String _gcpServiceAccountKeyClientCertURL;

	@Value("${liferay.partner.gcp.service.account.key.client-email}")
	private String _gcpServiceAccountKeyClientEmail;

	@Value("${liferay.partner.gcp.service.account.key.client-id}")
	private String _gcpServiceAccountKeyClientId;

	@Value("${liferay.partner.gcp.service.account.key.private-key}")
	private String _gcpServiceAccountKeyPrivateKey;

	@Value("${liferay.partner.gcp.service.account.key.private-key-id}")
	private String _gcpServiceAccountKeyPrivateKeyId;

	@Value("${liferay.partner.gcp.service.account.key.project-id}")
	private String _gcpServiceAccountKeyProjectId;

	@Value("${liferay.partner.gcp.service.account.key.token-uri}")
	private String _gcpServiceAccountKeyTokenURI;

	@Value("${liferay.partner.gcp.service.account.key.type}")
	private String _gcpServiceAccountKeyType;

	@Value("${liferay.partner.gcp.service.account.key.universe-domain}")
	private String _gcpServiceAccountKeyUniverseDomain;

}