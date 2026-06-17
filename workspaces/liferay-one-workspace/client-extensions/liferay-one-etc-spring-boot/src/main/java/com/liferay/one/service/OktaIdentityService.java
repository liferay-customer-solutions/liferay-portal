/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.one.constants.OktaConstants;
import com.liferay.one.model.OktaUser;
import com.liferay.one.pubsub.Message;
import com.liferay.one.pubsub.OktaPubsubPublisher;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.workflow.WorkflowConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * @author Karoline Silva
 */
@Component
public class OktaIdentityService {

	public void activateUser(String emailAddress) throws Exception {
		_oktaPubsubPublisher.publish(
			new Message(
				null,
				new JSONObject(
				).put(
					"action", "ACTIVATE"
				).put(
					"login", emailAddress
				).toString(),
				OktaConstants.TOPIC_OKTA_USER_UPDATE));
	}

	public void addMembership(String groupName, String emailAddress)
		throws Exception {

		_oktaPubsubPublisher.publish(
			new Message(
				null,
				new JSONObject(
				).put(
					"action", "ADD"
				).put(
					"groupName", groupName
				).put(
					"login", emailAddress
				).toString(),
				OktaConstants.TOPIC_OKTA_USER_GROUP_UPDATE));
	}

	public void assignUserToApplication(String appId, String emailAddress)
		throws Exception {

		_oktaPubsubPublisher.publish(
			new Message(
				null,
				new JSONObject(
				).put(
					"action", "ASSIGN"
				).put(
					"appId", appId
				).put(
					"emailAddress", emailAddress
				).toString(),
				OktaConstants.TOPIC_OKTA_APP_USER_UPDATE));
	}

	public OktaUser createContact(
			String emailAddress, String firstName, String middleName,
			String lastName)
		throws Exception {

		String response = _getOktaUser(emailAddress);

		JSONObject jsonObject = new JSONObject(response);

		if (jsonObject.has("errorCode")) {
			_oktaPubsubPublisher.publish(
				new Message(
					null,
					new JSONObject(
					).put(
						"emailAddress", emailAddress
					).put(
						"firstName", firstName
					).put(
						"lastName", lastName
					).put(
						"uuid",
						UUID.randomUUID(
						).toString()
					).toString(),
					OktaConstants.TOPIC_OKTA_USER_CREATE));

			return null;
		}

		return new OktaUser(jsonObject);
	}

	public OktaUser fetchContactByEmailAddress(String emailAddress)
		throws Exception {

		String response = _getOktaUser(emailAddress);

		JSONObject jsonObject = new JSONObject(response);

		if (jsonObject.has("errorCode")) {
			return null;
		}

		return new OktaUser(jsonObject);
	}

	public OktaUser fetchContactByUuid(String uuid) throws Exception {
		ResponseEntity<String> responseEntity = _webClient.get(
		).uri(
			uriBuilder -> uriBuilder.path(
				OktaConstants.URL_API_REST_USERS
			).queryParam(
				"search", "profile.uuid eq \"" + uuid + "\""
			).build()
		).retrieve(
		).toEntity(
			String.class
		).block();

		if ((responseEntity == null) ||
			Validator.isNull(responseEntity.getBody())) {

			return null;
		}

		JSONArray jsonArray = new JSONArray(responseEntity.getBody());

		if (jsonArray.isEmpty()) {
			return null;
		}

		return new OktaUser(jsonArray.getJSONObject(0));
	}

	public Integer fetchContactStatusByEmailAddress(String emailAddress)
		throws Exception {

		String response = _getOktaUser(emailAddress);

		JSONObject jsonObject = new JSONObject(response);

		if (jsonObject.has("errorCode")) {
			return null;
		}

		String status = jsonObject.optString("status");

		if (_statusesDeactivated.contains(status)) {
			return WorkflowConstants.STATUS_INACTIVE;
		}

		if (_statusesPending.contains(status)) {
			return WorkflowConstants.STATUS_PENDING;
		}

		return WorkflowConstants.STATUS_APPROVED;
	}

	public List<OktaUser> getGroupContacts(String groupId) throws Exception {
		return _getAllGroupMembers(groupId);
	}

	public void removeMembership(String groupName, String emailAddress)
		throws Exception {

		_oktaPubsubPublisher.publish(
			new Message(
				null,
				new JSONObject(
				).put(
					"action", "REMOVE"
				).put(
					"groupName", groupName
				).put(
					"login", emailAddress
				).toString(),
				OktaConstants.TOPIC_OKTA_USER_GROUP_UPDATE));
	}

	public OktaUser syncContact(
			String emailAddress, String firstName, String lastName, String uuid)
		throws Exception {

		String response = _getOktaUser(emailAddress);

		JSONObject jsonObject = new JSONObject(response);

		if (jsonObject.has("errorCode")) {
			_oktaPubsubPublisher.publish(
				new Message(
					null,
					new JSONObject(
					).put(
						"emailAddress", emailAddress
					).put(
						"firstName", firstName
					).put(
						"lastName", lastName
					).put(
						"uuid", uuid
					).toString(),
					OktaConstants.TOPIC_OKTA_USER_CREATE));

			return null;
		}

		return new OktaUser(jsonObject);
	}

	public void unassignUserFromApplication(String appId, String emailAddress)
		throws Exception {

		_oktaPubsubPublisher.publish(
			new Message(
				null,
				new JSONObject(
				).put(
					"action", "UNASSIGN"
				).put(
					"appId", appId
				).put(
					"emailAddress", emailAddress
				).toString(),
				OktaConstants.TOPIC_OKTA_APP_USER_UPDATE));
	}

	@PostConstruct
	protected void init() {
		_webClient = _webClientBuilder.baseUrl(
			"https://" + _host
		).defaultHeader(
			HttpHeaders.AUTHORIZATION, "SSWS " + _apiToken
		).build();
	}

	private List<OktaUser> _getAllGroupMembers(String groupId) {
		List<OktaUser> users = new ArrayList<>();

		String url = StringBundler.concat(
			OktaConstants.URL_API_REST_GROUPS, groupId,
			OktaConstants.URL_API_REST_GROUPS_USERS, "?limit=200");

		while (url != null) {
			ResponseEntity<String> responseEntity = _webClient.get(
			).uri(
				url
			).retrieve(
			).toEntity(
				String.class
			).block();

			if (responseEntity == null) {
				break;
			}

			String body = responseEntity.getBody();

			if (body == null) {
				break;
			}

			JSONArray jsonArray = new JSONArray(body);

			for (int i = 0; i < jsonArray.length(); i++) {
				users.add(new OktaUser(jsonArray.getJSONObject(i)));
			}

			url = _getNextUrl(responseEntity.getHeaders());
		}

		return users;
	}

	private String _getNextUrl(HttpHeaders headers) {
		List<String> links = headers.get("link");

		if (links == null) {
			return null;
		}

		for (String link : links) {
			String[] parts = link.split(";");

			if ((parts.length > 1) &&
				Objects.equals(parts[1].trim(), "rel=\"next\"")) {

				String urlPart = parts[0].trim();

				return urlPart.substring(1, urlPart.length() - 1);
			}
		}

		return null;
	}

	private String _getOktaUser(String login) {
		try {
			ResponseEntity<String> responseEntity = _webClient.get(
			).uri(
				OktaConstants.URL_API_REST_USERS + login
			).retrieve(
			).toEntity(
				String.class
			).block();

			if ((responseEntity == null) ||
				Validator.isNull(responseEntity.getBody())) {

				return _RESPONSE_USER_NOT_FOUND;
			}

			return responseEntity.getBody();
		}
		catch (WebClientResponseException webClientResponseException) {
			String body = webClientResponseException.getResponseBodyAsString();

			if (Validator.isNull(body)) {
				return _RESPONSE_USER_NOT_FOUND;
			}

			return body;
		}
	}

	private static final String _RESPONSE_USER_NOT_FOUND =
		"{\"errorCode\": \"E0000095\"}";

	private static final Set<String> _statusesDeactivated =
		Collections.singleton("DEPROVISIONED");
	private static final Set<String> _statusesPending = new HashSet<>(
		Arrays.asList("PROVISIONED", "STAGED"));

	@Value("${liferay.one.okta.api.token}")
	private String _apiToken;

	@Value("${liferay.one.okta.host}")
	private String _host;

	@Autowired
	private OktaPubsubPublisher _oktaPubsubPublisher;

	private WebClient _webClient;

	@Autowired
	private WebClient.Builder _webClientBuilder;

}