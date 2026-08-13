/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.okta.pubsub;

import com.liferay.headless.admin.user.client.dto.v1_0.Account;
import com.liferay.one.constants.PropertyConstants;
import com.liferay.one.jira.exception.AccountNotFoundException;
import com.liferay.one.pubsub.Message;
import com.liferay.one.pubsub.subscriber.BasePubsubSubscriber;
import com.liferay.one.service.AccountService;
import com.liferay.one.service.PropertyService;
import com.liferay.portal.kernel.util.Validator;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @author Kyle Bischof
 */
@Component
@ConditionalOnProperty(
	havingValue = "true",
	name = "liferay.one.okta.app.created.pubsub.subscriber.enabled"
)
public class OktaAppCreatedPubsubSubscriber extends BasePubsubSubscriber {

	@Override
	public String getTopic() {
		return _topic;
	}

	@Override
	public void receive(Message message) throws Exception {
		JSONObject jsonObject = new JSONObject(message.getPayload());

		String accountKey = jsonObject.optString("accountKey");
		String appId = jsonObject.optString("appId");

		if (Validator.isNull(accountKey) || Validator.isNull(appId)) {
			throw new IllegalArgumentException(
				"Missing accountKey or appId in the Okta app created message");
		}

		Account account = _accountService.fetchAccountByExternalReferenceCode(
			accountKey);

		if (account == null) {
			throw new AccountNotFoundException(
				"No account exists for external reference code " + accountKey);
		}

		String oktaApplicationId = _propertyService.getPropertyValue(
			account.getId(), PropertyConstants.NAME_OKTA_APPLICATION);

		if (Validator.isNotNull(oktaApplicationId)) {
			return;
		}

		_propertyService.addProperty(
			account.getId(), PropertyConstants.NAME_OKTA_APPLICATION, appId);
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

	@Autowired
	private AccountService _accountService;

	@Value("${liferay.one.okta.app.created.pubsub.subscriber.project.id}")
	private String _projectId;

	@Autowired
	private PropertyService _propertyService;

	@Value("${liferay.one.okta.app.created.pubsub.subscriber.subscription}")
	private String _subscription;

	@Value("${liferay.one.okta.app.created.pubsub.subscriber.topic}")
	private String _topic;

}