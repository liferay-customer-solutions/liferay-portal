/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.liferay.client.extension.util.spring.boot3.BaseRestController;
import com.liferay.one.permission.DebugMessageQueuePermission;
import com.liferay.one.pubsub.Message;
import com.liferay.one.pubsub.subscriber.BasePubsubSubscriber;
import com.liferay.petra.string.CharPool;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author Ryan Schuhler
 * @author Kyle Bischof
 */
@RequestMapping("/admin/debug-message-queue")
@RestController
public class DebugMessageQueueRestController extends BaseRestController {

	public DebugMessageQueueRestController(
		List<BasePubsubSubscriber> basePubsubSubscribers,
		DebugMessageQueuePermission debugMessageQueuePermission) {

		_basePubsubSubscribers = basePubsubSubscribers;
		_debugMessageQueuePermission = debugMessageQueuePermission;
	}

	@GetMapping("/routing-keys")
	public ResponseEntity<String> getRoutingKeys(
			@AuthenticationPrincipal Jwt jwt)
		throws Exception {

		_debugMessageQueuePermission.check(jwt);

		JSONArray jsonArray = new JSONArray();

		for (BasePubsubSubscriber basePubsubSubscriber :
				_basePubsubSubscribers) {

			String topic = basePubsubSubscriber.getTopic();

			if (Validator.isNull(topic)) {
				continue;
			}

			jsonArray.put(
				new JSONObject(
				).put(
					"routingKey", topic
				).put(
					"subscriber",
					basePubsubSubscriber.getClass(
					).getSimpleName()
				));
		}

		return new ResponseEntity<>(jsonArray.toString(), HttpStatus.OK);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ProblemDetail> handleException(Exception exception) {
		_log.error(exception);

		return _toResponseEntity(
			HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
	}

	@ExceptionHandler(PrincipalException.class)
	public ResponseEntity<ProblemDetail> handleException(
		PrincipalException principalException) {

		_log.error(principalException);

		return _toResponseEntity(
			HttpStatus.FORBIDDEN,
			"You do not have permission to access this resource");
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ProblemDetail> handleException(
		ResponseStatusException responseStatusException) {

		_log.error(responseStatusException);

		return new ResponseEntity<>(
			responseStatusException.getBody(),
			responseStatusException.getStatusCode());
	}

	@PostMapping
	public ResponseEntity<Void> post(
			@AuthenticationPrincipal Jwt jwt, @RequestBody String json)
		throws Exception {

		_debugMessageQueuePermission.check(jwt);

		JSONObject jsonObject = new JSONObject(json);

		Map<String, String> attributes = _parseProperties(
			jsonObject.optString("properties"));
		String payload = StringUtil.removeChars(
			jsonObject.optString("message"), CharPool.NEW_LINE,
			CharPool.RETURN);
		String routingKey = jsonObject.optString("routingKey");

		Message message = new Message(attributes, payload, routingKey);

		boolean dispatched = false;

		for (BasePubsubSubscriber basePubsubSubscriber :
				_basePubsubSubscribers) {

			if (!routingKey.equals(basePubsubSubscriber.getTopic())) {
				continue;
			}

			try {
				basePubsubSubscriber.dispatch(message);

				dispatched = true;
			}
			catch (Exception exception) {
				Class<?> clazz = basePubsubSubscriber.getClass();

				throw new ResponseStatusException(
					HttpStatus.BAD_GATEWAY,
					"Unable to dispatch message to subscriber " +
						clazz.getSimpleName(),
					exception);
			}
		}

		if (!dispatched) {
			throw new ResponseStatusException(
				HttpStatus.NOT_FOUND,
				"No subscriber consumes routing key " + routingKey);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	private Map<String, String> _parseProperties(String properties) {
		Map<String, String> attributes = new LinkedHashMap<>();

		String[] lines = StringUtil.split(properties, CharPool.NEW_LINE);

		for (String line : lines) {
			int index = line.indexOf(CharPool.EQUAL);

			if (index <= 0) {
				throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"Unable to parse properties line: " + line);
			}

			attributes.put(line.substring(0, index), line.substring(index + 1));
		}

		return attributes;
	}

	private ResponseEntity<ProblemDetail> _toResponseEntity(
		HttpStatus httpStatus, String detail) {

		return new ResponseEntity<>(
			ProblemDetail.forStatusAndDetail(httpStatus, detail), httpStatus);
	}

	private static final Log _log = LogFactory.getLog(
		DebugMessageQueueRestController.class);

	private final List<BasePubsubSubscriber> _basePubsubSubscribers;
	private final DebugMessageQueuePermission _debugMessageQueuePermission;

}