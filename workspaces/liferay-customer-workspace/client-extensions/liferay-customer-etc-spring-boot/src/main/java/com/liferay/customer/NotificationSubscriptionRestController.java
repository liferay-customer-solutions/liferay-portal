/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.customer;

import com.liferay.client.extension.util.spring.boot3.BaseRestController;
import com.liferay.customer.service.AnnouncementNotificationService;
import com.liferay.customer.service.BusinessEventNotificationService;
import com.liferay.customer.service.LicenseExpirationNotificationService;
import com.liferay.customer.service.ReleaseNotificationService;
import com.liferay.customer.service.SecurityVulnerabilityNotificationService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ryan Schuhler
 */
@RequestMapping("/notification-subscriptions")
@RestController
public class NotificationSubscriptionRestController extends BaseRestController {

	@PostMapping("/trigger/{type}")
	public ResponseEntity<String> trigger(@PathVariable String type) {
		try {
			if (type.equals("announcement")) {
				_announcementNotificationService.sendNotifications(null);
			}
			else if (type.equals("business-event")) {
				_businessEventNotificationService.sendNotifications(null);
			}
			else if (type.equals("license-expiration")) {
				_licenseExpirationNotificationService.sendNotifications();
			}
			else if (type.equals("release")) {
				_releaseNotificationService.sendNotifications(null);
			}
			else if (type.equals("security-vulnerability")) {
				_securityVulnerabilityNotificationService.sendNotifications(
					null);
			}
			else {
				return new ResponseEntity<>(
					"Invalid notification type: " + type,
					HttpStatus.BAD_REQUEST);
			}

			return new ResponseEntity<>(
				"Notification triggered for " + type, HttpStatus.OK);
		}
		catch (Exception exception) {
			_log.error("Error triggering notification type " + type, exception);

			return new ResponseEntity<>(
				"Error triggering notification: " + exception.getMessage(),
				HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private static final Log _log = LogFactory.getLog(
		NotificationSubscriptionRestController.class);

	@Autowired
	private AnnouncementNotificationService _announcementNotificationService;

	@Autowired
	private BusinessEventNotificationService _businessEventNotificationService;

	@Autowired
	private LicenseExpirationNotificationService
		_licenseExpirationNotificationService;

	@Autowired
	private ReleaseNotificationService _releaseNotificationService;

	@Autowired
	private SecurityVulnerabilityNotificationService
		_securityVulnerabilityNotificationService;

}