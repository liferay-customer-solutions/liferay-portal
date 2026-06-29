/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.client.extension.util.spring.boot3.service.BaseService;
import com.liferay.petra.string.StringBundler;

import java.time.Duration;

import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.util.UriComponentsBuilder;

import reactor.util.retry.Retry;

/**
 * @author Keven Leone
 */
@Component
public class ConsoleService extends BaseService {

	public void deleteProject(String projectId) throws Exception {
		delete(
			getAuthorization(), "",
			UriComponentsBuilder.fromUriString(
				_consoleAuthURL
			).path(
				"/projects/" + projectId
			).build(
			).toUri());
	}

	public JSONObject deployApp(
			String emailAddress, String orderId, String projectId)
		throws Exception {

		JSONObject jsonObject = new JSONObject(
			post(
				getAuthorization(),
				new JSONObject(
				).put(
					"orderId", orderId
				).put(
					"userEmail", emailAddress
				).toString(),
				UriComponentsBuilder.fromUriString(
					_consoleAuthURL
				).path(
					"/admin/projects/" + projectId + "/apps"
				).build(
				).toUri()));

		if (_log.isInfoEnabled()) {
			_log.info("Deployed app for project " + projectId);
		}

		return jsonObject;
	}

	public synchronized String getAuthorization() throws Exception {
		if ((_authorization != null) &&
			(System.currentTimeMillis() < (_tokenExpirationMillis - 30000))) {

			return _authorization;
		}

		String json = post(
			null,
			new JSONObject(
			).put(
				"email", _consoleAuthEmailAddress
			).put(
				"password", _consoleAuthPassword
			).toString(),
			UriComponentsBuilder.fromUriString(
				_consoleAuthURL
			).path(
				"/login"
			).build(
			).toUri());

		if (json == null) {
			throw new Exception("Unable to get authorization");
		}

		String token = new JSONObject(
			json
		).getString(
			"token"
		);

		_authorization = "Bearer " + token;

		_tokenExpirationMillis = System.currentTimeMillis() + 900000;

		return _authorization;
	}

	public void setUpProject(
			String cluster, boolean deployable, String dxpProjectUid,
			String dxpVirtualInstanceId, String[] emailAddresses, long orderId,
			String projectId)
		throws Exception {

		JSONObject jsonObject = _postProject(cluster, projectId);

		for (String emailAddress : emailAddresses) {
			try {
				_inviteProject(emailAddress, projectId);
			}
			catch (Exception exception) {
				_log.error(
					StringBundler.concat(
						"Unable to invite ", emailAddress, " to project ",
						projectId),
					exception);
			}
		}

		_linkDXPWithProject(
			dxpProjectUid, dxpVirtualInstanceId, jsonObject.getString("id"));

		if (deployable) {
			deployApp(
				_consoleAuthEmailAddress, String.valueOf(orderId), projectId);
		}
	}

	@Override
	protected ExchangeFilterFunction getWebClientExchangeFilterFunction() {
		return (clientRequest, exchangeFunction) -> exchangeFunction.exchange(
			clientRequest
		).retryWhen(
			Retry.fixedDelay(
				3, Duration.ofSeconds(5)
			).doBeforeRetry(
				retrySignal -> {
					if (_log.isInfoEnabled()) {
						_log.info(
							StringBundler.concat(
								"Retrying ", clientRequest.url(),
								retrySignal.totalRetries() + 1));
					}
				}
			)
		);
	}

	private void _inviteProject(String emailAddress, String projectId)
		throws Exception {

		if (Objects.equals(emailAddress, _consoleAuthEmailAddress)) {
			return;
		}

		post(
			getAuthorization(),
			new JSONObject(
			).put(
				"email", emailAddress
			).put(
				"role", "admin"
			).toString(),
			UriComponentsBuilder.fromUriString(
				_consoleAuthURL
			).path(
				"/projects/" + projectId + "/invite"
			).build(
			).toUri());

		if (_log.isInfoEnabled()) {
			_log.info(
				StringBundler.concat(
					"Invited ", emailAddress, " to project ", projectId));
		}
	}

	private void _linkDXPWithProject(
			String dxpProjectUid, String dxpVirtualInstanceId,
			String extensionProjectUid)
		throws Exception {

		post(
			getAuthorization(),
			new JSONObject(
			).put(
				"dxpProjectUid", dxpProjectUid
			).put(
				"dxpVirtualInstanceId", dxpVirtualInstanceId
			).put(
				"extensionProjectUid", extensionProjectUid
			).toString(),
			UriComponentsBuilder.fromUriString(
				_consoleAuthURL
			).path(
				"/lxc-extension-links"
			).build(
			).toUri());

		if (_log.isInfoEnabled()) {
			_log.info(
				StringBundler.concat(
					"Linked Liferay virtual instance ", dxpVirtualInstanceId,
					" with project ", extensionProjectUid));
		}
	}

	private JSONObject _postProject(String cluster, String projectId)
		throws Exception {

		JSONObject jsonObject = new JSONObject(
			post(
				getAuthorization(),
				new JSONObject(
				).put(
					"cluster", cluster
				).put(
					"environment", true
				).put(
					"metadata",
					new JSONObject(
					).put(
						"skipCloudProviderIamConfiguration", true
					)
				).put(
					"projectId", projectId
				).toString(),
				UriComponentsBuilder.fromUriString(
					_consoleAuthURL
				).path(
					"/projects"
				).build(
				).toUri()));

		if (_log.isInfoEnabled()) {
			_log.info("Created project " + jsonObject);
		}

		return jsonObject;
	}

	private static final Log _log = LogFactory.getLog(ConsoleService.class);

	private String _authorization;

	@Value("${liferay.one.console.auth.email.address}")
	private String _consoleAuthEmailAddress;

	@Value("${liferay.one.console.auth.password}")
	private String _consoleAuthPassword;

	@Value("${liferay.one.console.auth.url}")
	private String _consoleAuthURL;

	private long _tokenExpirationMillis;

}