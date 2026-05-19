/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.admin;

import com.liferay.client.extension.util.spring.boot3.BaseRestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Ryan Schuhler
 */
@RequestMapping("/admin/debug-message-queue")
@RestController
public class DebugMessageQueueRestController extends BaseRestController {

	public DebugMessageQueueRestController(DebugMessageQueueService service) {
		_service = service;
	}

	@GetMapping("/routing-keys")
	public ResponseEntity<String> getRoutingKeys(
		@AuthenticationPrincipal Jwt jwt) {

		try {
			if (!_hasStaff(jwt)) {
				return ResponseEntity.status(
					HttpStatus.FORBIDDEN
				).body(
					_errorBody("FORBIDDEN", "Staff access required")
				);
			}

			JSONArray jsonArray = new JSONArray();

			for (Map.Entry<String, String> entry :
					_service.getRoutingKeyToTopicMap(
					).entrySet()) {

				jsonArray.put(
					new JSONObject(
					).put(
						"routingKey", entry.getKey()
					).put(
						"topic", entry.getValue()
					));
			}

			return ResponseEntity.ok(jsonArray.toString());
		}
		catch (Exception exception) {
			_log.error(exception);

			return ResponseEntity.status(
				HttpStatus.INTERNAL_SERVER_ERROR
			).body(
				_errorBody("INTERNAL_ERROR", exception.getMessage())
			);
		}
	}

	@PostMapping
	public ResponseEntity<String> post(
		@AuthenticationPrincipal Jwt jwt, @RequestBody String json) {

		try {
			if (!_hasStaff(jwt)) {
				return ResponseEntity.status(
					HttpStatus.FORBIDDEN
				).body(
					_errorBody("FORBIDDEN", "Staff access required")
				);
			}

			JSONObject jsonObject = new JSONObject(json);

			String routingKey = jsonObject.optString("routingKey");
			String message = jsonObject.optString("message");
			String properties = jsonObject.optString("properties");

			_service.publish(routingKey, message, properties);

			return ResponseEntity.ok(
			).build();
		}
		catch (DebugMessageQueueService.UnknownRoutingKeyException
					unknownRoutingKeyException) {

			return ResponseEntity.badRequest(
			).body(
				_errorBody(
					"UNKNOWN_ROUTING_KEY",
					unknownRoutingKeyException.getMessage())
			);
		}
		catch (DebugMessageQueueService.MalformedPropertiesException
					malformedPropertiesException) {

			return ResponseEntity.badRequest(
			).body(
				_errorBody(
					"MALFORMED_PROPERTIES",
					malformedPropertiesException.getMessage())
			);
		}
		catch (Exception exception) {
			_log.error(exception);

			return ResponseEntity.status(
				HttpStatus.BAD_GATEWAY
			).body(
				_errorBody("PUBLISH_FAILURE", exception.getMessage())
			);
		}
	}

	protected List<String> getUserRoleNames(Jwt jwt) throws Exception {
		List<String> roleNames = new ArrayList<>();

		JSONObject userJSONObject = new JSONObject(
			get(
				"Bearer " + jwt.getTokenValue(),
				UriComponentsBuilder.fromPath(
					"/o/headless-admin-user/v1.0/my-user-account"
				).build(
				).toUri()));

		JSONArray roleBriefsJSONArray = userJSONObject.getJSONArray(
			"roleBriefs");

		for (int i = 0; i < roleBriefsJSONArray.length(); i++) {
			JSONObject roleBriefJSONObject = roleBriefsJSONArray.getJSONObject(
				i);

			roleNames.add(roleBriefJSONObject.getString("name"));
		}

		return roleNames;
	}

	private String _errorBody(String code, String message) {
		return new JSONObject(
		).put(
			"error",
			new JSONObject(
			).put(
				"code", code
			).put(
				"message", message
			)
		).toString();
	}

	private boolean _hasStaff(Jwt jwt) throws Exception {
		List<String> roleNames = getUserRoleNames(jwt);

		if (roleNames.contains("Administrator") ||
			roleNames.contains("Liferay Staff")) {

			return true;
		}

		return false;
	}

	private static final Log _log = LogFactory.getLog(
		DebugMessageQueueRestController.class);

	private final DebugMessageQueueService _service;

}