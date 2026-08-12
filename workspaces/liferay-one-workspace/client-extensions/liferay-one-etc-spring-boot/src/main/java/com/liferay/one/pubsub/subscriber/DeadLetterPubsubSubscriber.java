/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.pubsub.subscriber;

import com.liferay.one.pubsub.Message;
import com.liferay.one.service.NotificationQueueEntryService;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.HtmlUtil;
import com.liferay.portal.kernel.util.Validator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @author Felipe Franca
 */
@Component
@ConditionalOnProperty(
	havingValue = "true",
	name = "liferay.one.dead.letter.pubsub.subscriber.enabled"
)
public class DeadLetterPubsubSubscriber extends BaseDeadLetterPubsubSubscriber {

	@Override
	public String getTopic() {
		return getDeadLetterTopic();
	}

	@Override
	protected String getProjectId() {
		return _projectId;
	}

	@Override
	protected String getSubscriptionName() {
		return _subscription;
	}

	@Override
	protected boolean isAutoCreateTopic() {
		return false;
	}

	@Override
	protected void onDeadLetter(
		int deliveryAttempt, Message message, String sourceSubscriptionName) {

		try {
			_log.error(
				StringBundler.concat(
					"Unable to process message from source subscription ",
					sourceSubscriptionName, " after ", deliveryAttempt,
					" delivery attempts ", message));

			_sendNotificationEmail(
				deliveryAttempt, message, sourceSubscriptionName);
		}
		catch (Exception exception) {
			_log.error("Unable to report the dead letter message", exception);
		}
	}

	private void _sendNotificationEmail(
			int deliveryAttempt, Message message, String sourceSubscriptionName)
		throws Exception {

		if (Validator.isNull(_notificationRecipient)) {
			return;
		}

		String body = StringBundler.concat(
			"<p>A message was moved to the dead letter topic after ",
			deliveryAttempt, " delivery attempts.</p><p>Source Subscription: ",
			HtmlUtil.escape(sourceSubscriptionName),
			"</p><p>Attributes:</p><pre>",
			HtmlUtil.escape(String.valueOf(message.getAttributes())),
			"</pre><p>Payload:</p><pre>", HtmlUtil.escape(message.getPayload()),
			"</pre>");

		_notificationQueueEntryService.addNotificationQueueEntry(
			_emailAddressGlobal, "Liferay One", _notificationRecipient,
			"Liferay One Dead Letter Notification", body);
	}

	private static final Log _log = LogFactory.getLog(
		DeadLetterPubsubSubscriber.class);

	@Value("${liferay.one.provisioning.email.address.global}")
	private String _emailAddressGlobal;

	@Autowired
	private NotificationQueueEntryService _notificationQueueEntryService;

	@Value("${liferay.one.dead.letter.notification.recipient}")
	private String _notificationRecipient;

	@Value("${liferay.one.dead.letter.pubsub.subscriber.project.id}")
	private String _projectId;

	@Value("${liferay.one.dead.letter.pubsub.subscriber.subscription}")
	private String _subscription;

}