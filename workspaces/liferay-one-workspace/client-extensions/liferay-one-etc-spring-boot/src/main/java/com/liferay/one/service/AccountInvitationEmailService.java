/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.admin.user.client.dto.v1_0.Account;
import com.liferay.one.constants.AccountInvitationConstants;
import com.liferay.one.model.AccountInvitation;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.HtmlUtil;
import com.liferay.portal.kernel.util.Validator;

import java.net.URLEncoder;

import java.nio.charset.StandardCharsets;

import java.time.Year;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Pedro Oliveira
 */
@Component
public class AccountInvitationEmailService extends OneBaseService {

	public void sendInvitationEmail(
			Account account, AccountInvitation accountInvitation,
			String inviterName, String projectName)
		throws Exception {

		String notificationTemplateExternalReferenceCode =
			AccountInvitationConstants.
				NOTIFICATION_TEMPLATE_EXTERNAL_REFERENCE_CODE;

		if (Validator.isNotNull(projectName)) {
			notificationTemplateExternalReferenceCode =
				AccountInvitationConstants.
					PROJECT_NOTIFICATION_TEMPLATE_EXTERNAL_REFERENCE_CODE;
		}

		JSONObject processedTemplateJSONObject =
			_notificationTemplateService.getAndProcessTemplateJSONObject(
				notificationTemplateExternalReferenceCode, _DEFAULT_LANGUAGE_ID,
				HashMapBuilder.put(
					"ACCEPT_URL", _getAcceptURL(accountInvitation.getToken())
				).put(
					"ACCOUNT_NAME", HtmlUtil.escape(account.getName())
				).put(
					"INVITER_NAME", HtmlUtil.escape(inviterName)
				).put(
					"PROJECT_NAME", HtmlUtil.escape(projectName)
				).put(
					"USER_FIRST_NAME",
					HtmlUtil.escape(accountInvitation.getGivenName())
				).put(
					"YEAR",
					Year.now(
					).toString()
				).build());

		_notificationQueueEntryService.addNotificationQueueEntry(
			_emailAddressGlobal, "Liferay One",
			accountInvitation.getEmailAddress(),
			processedTemplateJSONObject.getString("subject"),
			processedTemplateJSONObject.getString("body"));
	}

	private String _getAcceptURL(String token) {
		return StringBundler.concat(
			_portalURL, AccountInvitationConstants.PAGE_PATH, "?token=",
			URLEncoder.encode(token, StandardCharsets.UTF_8));
	}

	private static final String _DEFAULT_LANGUAGE_ID = "en_US";

	@Value("${liferay.one.provisioning.email.address.global}")
	private String _emailAddressGlobal;

	@Autowired
	private NotificationQueueEntryService _notificationQueueEntryService;

	@Autowired
	private NotificationTemplateService _notificationTemplateService;

	@Value("${liferay.one.portal.url}")
	private String _portalURL;

}