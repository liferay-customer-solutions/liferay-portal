/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.jira.service;

import com.liferay.one.jira.exception.JiraAssetSchemaException;
import com.liferay.one.jira.model.JiraAssetObject;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.Validator;

import java.net.URI;

import java.time.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import reactor.util.retry.Retry;

/**
 * @author Drew Brokke
 */
@Component
public class JiraAssetPersistence extends BaseJiraService {

	public JSONObject createObject(
		String objectTypeId, JiraAssetObject jiraAssetObject) {

		String requestBody = new JSONObject(
		).put(
			"attributes", jiraAssetObject.toAttributesJSONArray()
		).put(
			"objectTypeId", objectTypeId
		).toString();

		try {
			String response = post(
				requestBody, _headers(), _objectURI("create"));

			return _toJSONObject(response);
		}
		catch (WebClientResponseException webClientResponseException) {
			_log.error(
				StringBundler.concat(
					"Unable to create asset object with object type ",
					objectTypeId, ": ",
					webClientResponseException.getResponseBodyAsString(),
					"; request body: ", requestBody));

			throw webClientResponseException;
		}
	}

	public JSONObject deleteObject(String objectId) {
		try {
			String response = Mono.fromCallable(
				() -> delete(
					getAuthorization(), StringPool.BLANK, _objectURI(objectId))
			).retryWhen(
				Retry.backoff(
					_MAX_RETRIES, _RETRY_MIN_BACKOFF
				).maxBackoff(
					_RETRY_MAX_BACKOFF
				).scheduler(
					Schedulers.boundedElastic()
				).filter(
					this::_isRetryable
				).onRetryExhaustedThrow(
					(retryBackoffSpec, retrySignal) -> retrySignal.failure()
				)
			).block();

			return _toJSONObject(response);
		}
		catch (WebClientResponseException webClientResponseException) {
			HttpStatusCode httpStatusCode =
				webClientResponseException.getStatusCode();

			if (httpStatusCode.isSameCodeAs(HttpStatus.NOT_FOUND)) {
				if (_log.isInfoEnabled()) {
					_log.info(
						StringBundler.concat(
							"Skipping delete of asset object ", objectId,
							" because it does not exist"));
				}

				return new JSONObject();
			}

			_log.error(
				StringBundler.concat(
					"Unable to delete asset object ", objectId, ": ",
					webClientResponseException.getResponseBodyAsString()));

			throw webClientResponseException;
		}
		catch (RuntimeException runtimeException) {
			_log.error(
				"Unable to delete asset object " + objectId, runtimeException);

			throw runtimeException;
		}
	}

	public JSONObject getObject(String objectId) {
		return _toJSONObject(get(getAuthorization(), _objectURI(objectId)));
	}

	public JSONArray getObjectSchemas() {
		JSONArray itemsJSONArray = new JSONArray();

		boolean last = false;
		int startAt = 0;

		while (!last) {
			JSONObject resultsJSONObject = _getObjectSchemasPageJSONObject(
				startAt);

			JSONArray valuesJSONArray = resultsJSONObject.optJSONArray(
				"values");

			if ((valuesJSONArray == null) || valuesJSONArray.isEmpty()) {
				break;
			}

			itemsJSONArray.putAll(valuesJSONArray);

			last = resultsJSONObject.optBoolean("isLast");

			startAt += _MAX_RESULTS;
		}

		return itemsJSONArray;
	}

	public JSONArray getObjectTypeAttributes(String objectTypeId) {
		return _toSchemaJSONArray(
			get(
				getAuthorization(),
				_v1URI(
					StringBundler.concat(
						"objecttype/", objectTypeId, "/attributes"))),
			"Unable to parse attributes response for object type " +
				objectTypeId);
	}

	public JSONArray getObjectTypes(String schemaId) {
		return _toSchemaJSONArray(
			get(
				getAuthorization(),
				_v1URI(
					StringBundler.concat(
						"objectschema/", schemaId, "/objecttypes"))),
			"Unable to parse object types response for schema " + schemaId);
	}

	public JSONArray searchObjects(String aql) {
		JSONArray itemsJSONArray = new JSONArray();

		boolean last = false;
		int startAt = 0;

		while (!last) {
			JSONObject resultsJSONObject = _searchObjectsPage(aql, startAt);

			JSONArray valuesJSONArray = resultsJSONObject.optJSONArray(
				"values");

			if ((valuesJSONArray == null) || valuesJSONArray.isEmpty()) {
				break;
			}

			itemsJSONArray.putAll(valuesJSONArray);

			last = resultsJSONObject.optBoolean("last");

			startAt += _MAX_RESULTS;
		}

		return itemsJSONArray;
	}

	public <T> List<T> searchObjects(
		String aql, Function<JSONObject, T> transformFunction) {

		ArrayList<T> results = new ArrayList<>();

		JSONArray jsonArray = searchObjects(aql);

		for (int i = 0; i < jsonArray.length(); i++) {
			results.add(transformFunction.apply(jsonArray.getJSONObject(i)));
		}

		return results;
	}

	public JSONObject updateObject(
		String objectId, JiraAssetObject jiraAssetObject) {

		String requestBody = new JSONObject(
		).put(
			"attributes", jiraAssetObject.toAttributesJSONArray()
		).toString();

		try {
			String response = put(
				requestBody, _headers(), _objectURI(objectId));

			return _toJSONObject(response);
		}
		catch (WebClientResponseException webClientResponseException) {
			_log.error(
				StringBundler.concat(
					"Unable to update asset object ", objectId, ": ",
					webClientResponseException.getResponseBodyAsString(),
					"; request body: ", requestBody));

			throw webClientResponseException;
		}
	}

	private JSONObject _getObjectSchemasPageJSONObject(int startAt) {
		String response = get(
			getAuthorization(),
			UriComponentsBuilder.fromUri(
				_v1URI("objectschema/list")
			).queryParam(
				"maxResults", _MAX_RESULTS
			).queryParam(
				"startAt", startAt
			).build(
			).toUri());

		return _toJSONObject(response);
	}

	private Map<String, String> _headers() {
		return HashMapBuilder.put(
			HttpHeaders.AUTHORIZATION, getAuthorization()
		).put(
			HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE
		).build();
	}

	private boolean _isRetryable(Throwable throwable) {
		if (throwable instanceof
				WebClientResponseException webClientResponseException) {

			HttpStatusCode httpStatusCode =
				webClientResponseException.getStatusCode();

			if (httpStatusCode.is5xxServerError() ||
				httpStatusCode.isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS)) {

				return true;
			}

			return false;
		}

		return true;
	}

	private URI _objectURI(String suffix) {
		return _v1URI("object/" + suffix);
	}

	private JSONObject _searchObjectsPage(String aql, int startAt) {
		String response = post(
			new JSONObject(
			).put(
				"qlQuery", aql
			).toString(),
			_headers(),
			UriComponentsBuilder.fromUri(
				_v1URI("object/aql")
			).queryParam(
				"maxResults", _MAX_RESULTS
			).queryParam(
				"startAt", startAt
			).build(
			).toUri());

		return _toJSONObject(response);
	}

	private JSONObject _toJSONObject(String response) {
		if (Validator.isNull(response)) {
			return new JSONObject();
		}

		try {
			return new JSONObject(response);
		}
		catch (JSONException jsonException) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					"Unable to parse JSON object response", jsonException);
			}

			return new JSONObject();
		}
	}

	private JSONArray _toSchemaJSONArray(String response, String message) {
		if (Validator.isNull(response)) {
			throw new JiraAssetSchemaException(message + ": empty response");
		}

		try {
			return new JSONArray(response);
		}
		catch (JSONException jsonException) {
			throw new JiraAssetSchemaException(message, jsonException);
		}
	}

	private URI _v1URI(String path) {
		return UriComponentsBuilder.fromUriString(
			StringBundler.concat(
				_JIRA_CLOUD_API_URL, "/jsm/assets/workspace/", _jiraWorkspaceId,
				"/v1/", path)
		).build(
		).toUri();
	}

	private static final String _JIRA_CLOUD_API_URL =
		"https://api.atlassian.com";

	private static final int _MAX_RESULTS = 100;

	private static final int _MAX_RETRIES = 3;

	// Callers hold a KeyedLock stripe across the retried call, so the
	// worst case backoff must stay within a few seconds to keep unrelated
	// syncs that hash to the same stripe from stalling

	private static final Duration _RETRY_MAX_BACKOFF = Duration.ofSeconds(2);

	private static final Duration _RETRY_MIN_BACKOFF = Duration.ofMillis(500);

	private static final Log _log = LogFactory.getLog(
		JiraAssetPersistence.class);

	@Value("${liferay.one.jira.workspace.id}")
	private String _jiraWorkspaceId;

}