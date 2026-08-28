/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.notification.rest.client.dto.v1_0.NotificationTemplate;
import com.liferay.notification.rest.client.resource.v1_0.NotificationTemplateResource;
import com.liferay.portal.kernel.util.StringUtil;

import java.util.Map;

import org.json.JSONObject;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * @author Amos Fong
 */
@Component
public class NotificationTemplateService extends OneBaseService {

	public JSONObject getAndProcessTemplateJSONObject(
			String externalReferenceCode, String languageId,
			Map<String, String> placeholders)
		throws Exception {

		NotificationTemplateResource notificationTemplateResource =
			NotificationTemplateResource.builder(
			).endpoint(
				getDXPEndpointAddress(), lxcDXPServerProtocol
			).header(
				HttpHeaders.AUTHORIZATION, getAuthorization()
			).build();

		return _getProcessedTemplateJSONObject(
			languageId,
			notificationTemplateResource.
				getNotificationTemplateByExternalReferenceCode(
					externalReferenceCode),
			placeholders);
	}

	private String _getLocalizedValue(Object value, String languageId) {
		if (!(value instanceof Map)) {
			return null;
		}

		Map<String, String> valueMap = (Map<String, String>)value;

		String localizedValue = valueMap.get(languageId);

		if (localizedValue != null) {
			return localizedValue;
		}

		localizedValue = valueMap.get(StringUtil.replace(languageId, '_', '-'));

		if (localizedValue != null) {
			return localizedValue;
		}

		return valueMap.get(_DEFAULT_LANGUAGE_ID);
	}

	private JSONObject _getProcessedTemplateJSONObject(
		String languageId, NotificationTemplate notificationTemplate,
		Map<String, String> placeholders) {

		String body = _getLocalizedValue(
			notificationTemplate.getBody(), languageId);
		String subject = _getLocalizedValue(
			notificationTemplate.getSubject(), languageId);

		JSONObject jsonObject = new JSONObject(
		).put(
			"body", _replacePlaceholders(placeholders, body)
		).put(
			"subject", _replacePlaceholders(placeholders, subject)
		);

		Object[] recipients = notificationTemplate.getRecipients();

		if ((recipients != null) && (recipients.length > 0)) {
			Map<String, Object> recipient = (Map<String, Object>)recipients[0];

			Object from = recipient.get("from");

			jsonObject.put(
				"from",
				_replacePlaceholders(
					placeholders, from instanceof String ? (String)from : null)
			).put(
				"fromName",
				_replacePlaceholders(
					placeholders,
					_getLocalizedValue(recipient.get("fromName"), languageId))
			).put(
				"to",
				_replacePlaceholders(
					placeholders,
					_getLocalizedValue(recipient.get("to"), languageId))
			);
		}

		return jsonObject;
	}

	private String _replacePlaceholders(
		Map<String, String> placeholders, String value) {

		if (value == null) {
			return null;
		}

		for (Map.Entry<String, String> entry : placeholders.entrySet()) {
			value = StringUtil.replace(
				value, "[%" + entry.getKey() + "%]", entry.getValue());
		}

		return value;
	}

	private static final String _DEFAULT_LANGUAGE_ID = "en_US";

}