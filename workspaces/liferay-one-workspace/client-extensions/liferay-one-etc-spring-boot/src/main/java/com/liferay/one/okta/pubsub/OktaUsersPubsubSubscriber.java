/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.okta.pubsub;

import com.liferay.headless.admin.user.client.dto.v1_0.AccountBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.one.okta.model.OktaUser;
import com.liferay.one.pubsub.Message;
import com.liferay.one.pubsub.subscriber.BasePubsubSubscriber;
import com.liferay.one.service.AccountService;
import com.liferay.one.service.ProvisioningAssignmentService;
import com.liferay.one.service.ProvisioningEmailService;
import com.liferay.one.service.UserAccountService;
import com.liferay.one.util.UserAccountUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
	name = "liferay.one.okta.users.pubsub.subscriber.enabled"
)
public class OktaUsersPubsubSubscriber extends BasePubsubSubscriber {

	@Override
	public String getTopic() {
		return _topic;
	}

	@Override
	public void receive(Message message) throws Exception {
		if (_log.isDebugEnabled()) {
			_log.debug("Parsing message: " + message.getPayload());
		}

		try {
			JSONObject jsonObject = new JSONObject(message.getPayload());

			String eventType = jsonObject.optString("eventType");

			OktaUser oktaUser = new OktaUser(
				jsonObject.optJSONObject("user", new JSONObject()));

			if (Objects.equals(eventType, _EVENT_TYPE_LIFECYCLE_ACTIVATE) ||
				Objects.equals(eventType, _EVENT_TYPE_LIFECYCLE_CREATE)) {

				_syncContact(oktaUser);
			}
			else if (Objects.equals(
						eventType, _EVENT_TYPE_LIFECYCLE_DEACTIVATE)) {

				_unassignAllMemberships(oktaUser);
			}
			else if (Objects.equals(
						eventType, _EVENT_TYPE_ACCOUNT_UPDATE_PASSWORD) ||
					 Objects.equals(
						 eventType, _EVENT_TYPE_ACCOUNT_UPDATE_PROFILE)) {

				_updateContact(oktaUser);
			}
		}
		catch (Exception exception) {
			_log.error(
				"Unable to process Okta users message " + message.getPayload(),
				exception);
		}
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

	private UserAccount _fetchUserAccount(String emailAddress)
		throws Exception {

		if (Validator.isNull(emailAddress)) {
			return null;
		}

		return _userAccountService.fetchUserAccountByEmailAddress(emailAddress);
	}

	private void _removeAccountUserAccount(
			long accountEntryId, UserAccount userAccount)
		throws Exception {

		_accountService.removeAccountUserAccount(
			accountEntryId, userAccount.getId());

		_provisioningAssignmentService.unassignAccountMembership(
			accountEntryId, userAccount.getId());
	}

	private void _syncContact(OktaUser oktaUser) throws Exception {
		UserAccount userAccount = _fetchUserAccount(oktaUser.getEmail());

		if (userAccount == null) {
			return;
		}

		if (oktaUser.isEmailAddressVerified() &&
			!UserAccountUtil.isVerified(userAccount)) {

			_userAccountService.setVerified(userAccount.getId());
		}
	}

	private void _unassignAllMemberships(OktaUser oktaUser) throws Exception {
		UserAccount userAccount = _fetchUserAccount(oktaUser.getEmail());

		if (userAccount == null) {
			return;
		}

		AccountBrief[] accountBriefs = userAccount.getAccountBriefs();

		if (accountBriefs == null) {
			return;
		}

		for (AccountBrief accountBrief : accountBriefs) {
			try {
				_removeAccountUserAccount(accountBrief.getId(), userAccount);
			}
			catch (Exception exception) {
				_log.error(
					"Unable to unassign membership for " + oktaUser.getEmail(),
					exception);
			}
		}
	}

	private void _updateContact(OktaUser oktaUser) throws Exception {
		UserAccount userAccount = _fetchUserAccount(oktaUser.getEmail());

		if (userAccount == null) {
			return;
		}

		if (oktaUser.isEmailAddressVerified() &&
			!UserAccountUtil.isVerified(userAccount)) {

			_provisioningEmailService.sendVerifiedWelcomeEmail(userAccount);

			_userAccountService.setVerified(userAccount.getId());
		}
	}

	private static final String _EVENT_TYPE_ACCOUNT_UPDATE_PASSWORD =
		"user.account.update_password";

	private static final String _EVENT_TYPE_ACCOUNT_UPDATE_PROFILE =
		"user.account.update_profile";

	private static final String _EVENT_TYPE_LIFECYCLE_ACTIVATE =
		"user.lifecycle.activate";

	private static final String _EVENT_TYPE_LIFECYCLE_CREATE =
		"user.lifecycle.create";

	private static final String _EVENT_TYPE_LIFECYCLE_DEACTIVATE =
		"user.lifecycle.deactivate";

	private static final Log _log = LogFactory.getLog(
		OktaUsersPubsubSubscriber.class);

	@Autowired
	private AccountService _accountService;

	@Value("${liferay.one.okta.users.pubsub.subscriber.project.id}")
	private String _projectId;

	@Autowired
	private ProvisioningAssignmentService _provisioningAssignmentService;

	@Autowired
	private ProvisioningEmailService _provisioningEmailService;

	@Value("${liferay.one.okta.users.pubsub.subscriber.subscription}")
	private String _subscription;

	@Value("${liferay.one.okta.users.pubsub.subscriber.topic}")
	private String _topic;

	@Autowired
	private UserAccountService _userAccountService;

}