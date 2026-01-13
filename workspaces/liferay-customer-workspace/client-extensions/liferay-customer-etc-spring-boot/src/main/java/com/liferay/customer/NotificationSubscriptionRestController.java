/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.customer;

import com.liferay.client.extension.util.spring.boot3.BaseRestController;
import com.liferay.customer.service.NotificationSubscriptionService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ryan Schuhler
 */
@RequestMapping("/notification-subscriptions")
@RestController
public class NotificationSubscriptionRestController extends BaseRestController {

	@GetMapping("/check")
	public ResponseEntity<String> checkNotificationSubscriptions() {
		try {
			_notificationSubscriptionService.scheduled();

			return new ResponseEntity<>(
				"Notification subscriptions checked successfully.",
				HttpStatus.OK);
		}
		catch (Exception exception) {
			_log.error(
				"Error checking notification subscriptions via REST endpoint",
				exception);

			return new ResponseEntity<>(
				"Error checking notification subscriptions: " +
					exception.getMessage(),
				HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private static final Log _log = LogFactory.getLog(
		NotificationSubscriptionRestController.class);

	@Autowired
	private NotificationSubscriptionService _notificationSubscriptionService;

}