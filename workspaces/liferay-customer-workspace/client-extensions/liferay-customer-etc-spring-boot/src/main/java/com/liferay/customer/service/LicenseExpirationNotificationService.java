/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.customer.service;

import com.liferay.osb.provisioning.rest.client.dto.v1_0.LicenseKey;

import java.util.List;

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
public class LicenseExpirationNotificationService
	extends BaseNotificationService {

	@Scheduled(
		cron = "${liferay.customer.notification.subscription.license.expiration.cron:-}"
	)
	public void sendNotifications() throws Exception {
		if (_log.isInfoEnabled()) {
			_log.info("Checking for license expirations");
		}

		List<LicenseKey> licenseKeys =
			_provisioningService.checkLicenseExpirations();

		if (licenseKeys.isEmpty()) {
			if (_log.isInfoEnabled()) {
				_log.info("No expiring licenses to notify");
			}

			return;
		}

		if (_log.isInfoEnabled()) {
			_log.info(
				"Found " + licenseKeys.size() + " expiring licenses to notify");
		}

		for (LicenseKey licenseKey : licenseKeys) {
			try {
				JSONObject licenseKeyJSONObject = new JSONObject(
					licenseKey.toString());

				_log.info(licenseKeyJSONObject);

				String externalReferenceCode = licenseKeyJSONObject.getString(
					"externalReferenceCode");

				if ((externalReferenceCode == null) ||
					externalReferenceCode.isEmpty()) {

					continue;
				}

				String subscriptionFilter =
					"type eq 'licenseExpiration' and filter eq '" +
						escapeFilterValue(externalReferenceCode) + "'";

				JSONArray subscriptionsJSONArray =
					_notificationSubscriptionService.
						getNotificationSubscriptionsJSONArray(
							subscriptionFilter);

				JSONObject templatePayloadJSONObject = new JSONObject();

				templatePayloadJSONObject.put(
					"LICENSE_DETAILS", licenseKeyJSONObject.toString(2));

				sendNotifications(
					subscriptionsJSONArray, "LICENSE-EXPIRATION",
					templatePayloadJSONObject);
			}
			catch (Exception exception) {
				_log.error(
					"Unable to process license expiration notification: " +
						licenseKey,
					exception);
			}
		}

		if (_log.isInfoEnabled()) {
			_log.info(
				"Sent " + licenseKeys.size() +
					" license expiration notifications");
		}
	}

	private static final Log _log = LogFactory.getLog(
		LicenseExpirationNotificationService.class);

	@Autowired
	private NotificationSubscriptionService _notificationSubscriptionService;

	@Autowired
	private ProvisioningService _provisioningService;

}