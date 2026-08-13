/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.liferay.headless.admin.user.client.dto.v1_0.Account;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.one.constants.AccountInvitationConstants;
import com.liferay.one.jira.util.JiraSyncLock;
import com.liferay.one.model.AccountInvitation;
import com.liferay.one.service.AccountInvitationService;
import com.liferay.one.service.AccountService;
import com.liferay.one.service.UserAccountService;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.Validator;

import java.net.URI;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Pedro Oliveira
 */
@RequestMapping(AccountInvitationConstants.INVITATIONS_PATH)
@RestController
public class AccountInvitationsRestController extends OneBaseRestController {

	@GetMapping(AccountInvitationConstants.ACCEPT_SEGMENT)
	public ResponseEntity<Void> getAccept(@RequestParam("token") String token)
		throws Exception {

		if (!_isValidToken(token)) {
			return _redirect(AccountInvitationConstants.STATUS_INVALID);
		}

		return _jiraSyncLock.withLock(
			token,
			() -> {
				AccountInvitation accountInvitation =
					_accountInvitationService.fetchAccountInvitationByToken(
						token);

				if (accountInvitation == null) {
					return _redirect(AccountInvitationConstants.STATUS_INVALID);
				}

				if (accountInvitation.isAccepted()) {
					return _redirect(
						AccountInvitationConstants.STATUS_ACCEPTED);
				}

				try {
					_accept(accountInvitation);
				}
				catch (Exception exception) {
					_log.error(
						"Unable to accept the invitation " +
							accountInvitation.getExternalReferenceCode(),
						exception);

					return _redirect(AccountInvitationConstants.STATUS_ERROR);
				}

				return _redirect(AccountInvitationConstants.STATUS_ACCEPTED);
			});
	}

	private void _accept(AccountInvitation accountInvitation) throws Exception {
		Account account = _accountService.getAccount(
			accountInvitation.getAccountExternalReferenceCode());

		String emailAddress = accountInvitation.getEmailAddress();

		UserAccount userAccount =
			_userAccountService.fetchUserAccountByEmailAddress(emailAddress);

		if (userAccount == null) {
			userAccount = _userAccountService.addUserAccount(
				emailAddress, accountInvitation.getFamilyName(),
				accountInvitation.getGivenName());
		}

		_accountService.addAccountUserAccountByEmailAddress(
			account.getId(), emailAddress, null);

		_addAccountUserAccountRoles(account, accountInvitation, userAccount);

		_accountInvitationService.updateAccepted(
			accountInvitation.getAccountInvitationId());
	}

	private void _addAccountUserAccountRoles(
			Account account, AccountInvitation accountInvitation,
			UserAccount userAccount)
		throws Exception {

		List<String> roleNames = accountInvitation.getRoleNames();

		if (roleNames.isEmpty()) {
			return;
		}

		Map<String, Long> accountRoleIds = new HashMap<>();

		for (Map.Entry<Long, String> entry :
				_accountService.getAccountRoleNames(
					account.getId()
				).entrySet()) {

			accountRoleIds.put(entry.getValue(), entry.getKey());
		}

		for (String roleName : roleNames) {
			Long accountRoleId = accountRoleIds.get(roleName);

			if (accountRoleId == null) {
				_log.error(
					StringBundler.concat(
						"Unable to find account role ", roleName,
						" for account ", account.getExternalReferenceCode()));

				continue;
			}

			_accountService.addAccountUserAccountRole(
				account.getId(), accountRoleId, userAccount.getId());
		}
	}

	private boolean _isValidToken(String token) {
		if (Validator.isNull(token)) {
			return false;
		}

		try {
			UUID.fromString(token);

			return true;
		}
		catch (IllegalArgumentException illegalArgumentException) {
			if (_log.isDebugEnabled()) {
				_log.debug(
					"Unable to read the invitation token as a UUID",
					illegalArgumentException);
			}

			return false;
		}
	}

	private ResponseEntity<Void> _redirect(String status) {
		return ResponseEntity.status(
			HttpStatus.FOUND
		).location(
			URI.create(_portalURL + "/?invitation=" + status)
		).build();
	}

	private static final Log _log = LogFactory.getLog(
		AccountInvitationsRestController.class);

	@Autowired
	private AccountInvitationService _accountInvitationService;

	@Autowired
	private AccountService _accountService;

	@Autowired
	private JiraSyncLock _jiraSyncLock;

	@Value("${liferay.one.portal.url}")
	private String _portalURL;

	@Autowired
	private UserAccountService _userAccountService;

}