/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.customer.service;

import com.liferay.customer.constants.NotificationSubscriptionConstants;
import com.liferay.headless.delivery.client.dto.v1_0.StructuredContent;

import java.time.ZonedDateTime;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author Ryan Schuhler
 */
@Component
public class ReleaseNotificationService extends BaseNotificationService {

	@Scheduled(
		cron = "${liferay.customer.notification.subscription.release.cron:-}"
	)
	public void sendNotifications() {
		try {
			sendNotifications(
				_lastSuccessfulRun.get(
					_RELEASE_NOTIFICATIONS_LAST_SUCCESSFUL_RUN_KEY));

			_lastSuccessfulRun.put(
				_RELEASE_NOTIFICATIONS_LAST_SUCCESSFUL_RUN_KEY,
				ZonedDateTime.now());
		}
		catch (Exception exception) {
			_log.error("Error sending release notifications", exception);
		}
	}

	public void sendNotifications(ZonedDateTime zonedDateTime)
		throws Exception {

		if (_log.isInfoEnabled()) {
			_log.info("Checking for release notifications");
		}

		List<StructuredContent> filteredContents =
			_structuredContentService.getStructuredContent(
				NotificationSubscriptionConstants.CATEGORY_NAME_RELEASE_NOTES,
				zonedDateTime);

		if (filteredContents.isEmpty()) {
			if (_log.isInfoEnabled()) {
				_log.info("No new release notes to notify");
			}

			return;
		}

		if (_log.isInfoEnabled()) {
			_log.info(
				"Found " + filteredContents.size() +
					" release notes to notify");
		}

		for (StructuredContent structuredContent : filteredContents) {
			try {
				JSONObject structuredContentJSONObject = new JSONObject(
					structuredContent.toString());

				String productVersion = structuredContentJSONObject.optString(
					"productVersion");

				if ((productVersion == null) || productVersion.isEmpty()) {
					continue;
				}

				String subscriptionFilter =
					"type eq 'release' and filter eq '" +
						escapeFilterValue(productVersion) + "'";

				JSONArray subscriptionsJSONArray =
					_notificationSubscriptionService.
						getNotificationSubscriptionsJSONArray(
							subscriptionFilter);

				JSONObject templatePayloadJSONObject = new JSONObject();

				templatePayloadJSONObject.put(
					"RELEASE_CONTENT",
					structuredContentJSONObject.optString("content", "")
				).put(
					"RELEASE_TITLE",
					structuredContentJSONObject.optString("title", "")
				);

				sendNotifications(
					subscriptionsJSONArray, "RELEASE",
					templatePayloadJSONObject);
			}
			catch (Exception exception) {
				_log.error(
					"Unable to process release notification: " +
						structuredContent,
					exception);
			}
		}

		if (_log.isInfoEnabled()) {
			_log.info(
				"Sent " + filteredContents.size() + " release notifications");
		}
	}

	private static final String _RELEASE_NOTIFICATIONS_LAST_SUCCESSFUL_RUN_KEY =
		"releaseNotifications";

	private static final Log _log = LogFactory.getLog(
		ReleaseNotificationService.class);

	private final Map<String, ZonedDateTime> _lastSuccessfulRun =
		new ConcurrentHashMap<>();

	@Autowired
	private NotificationSubscriptionService _notificationSubscriptionService;

	@Autowired
	private StructuredContentService _structuredContentService;

}