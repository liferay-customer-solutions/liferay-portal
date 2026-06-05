/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.notification.rest.client.dto.v1_0.NotificationQueueEntry;
import com.liferay.notification.rest.client.resource.v1_0.NotificationQueueEntryResource;
import com.liferay.portal.kernel.util.HashMapBuilder;

import java.util.TreeMap;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * @author Amos Fong
 */
@Component
public class NotificationQueueEntryService extends OneBaseService {

	public void addNotificationQueueEntry(
			String fromEmail, String fromName, String toEmail, String subject,
			String body)
		throws Exception {

		NotificationQueueEntryResource notificationQueueEntryResource =
			NotificationQueueEntryResource.builder(
			).endpoint(
				lxcDXPMainDomain, lxcDXPServerProtocol
			).header(
				HttpHeaders.AUTHORIZATION, getAuthorization()
			).build();

		NotificationQueueEntry notificationQueueEntry =
			new NotificationQueueEntry();

		notificationQueueEntry.setBody(() -> body);
		notificationQueueEntry.setRecipients(
			() -> new Object[] {
				new TreeMap<>(
					HashMapBuilder.put(
						"from", fromEmail
					).put(
						"fromName", fromName
					).put(
						"to", toEmail
					).build())
			});
		notificationQueueEntry.setSubject(() -> subject);
		notificationQueueEntry.setType(() -> "email");

		notificationQueueEntryResource.postNotificationQueueEntry(
			notificationQueueEntry);
	}

}