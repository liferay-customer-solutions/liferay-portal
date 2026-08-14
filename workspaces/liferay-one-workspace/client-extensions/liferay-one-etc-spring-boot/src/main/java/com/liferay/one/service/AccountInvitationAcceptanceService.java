/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.admin.user.client.dto.v1_0.Account;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.one.model.AccountInvitation;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.Validator;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Pedro Oliveira
 */
@Component
public class AccountInvitationAcceptanceService {

	public void provisionAccountInvitation(AccountInvitation accountInvitation)
		throws Exception {

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

		_addProjectMembership(accountInvitation, userAccount);
	}

	private void _addAccountUserAccountRoles(
			Account account, AccountInvitation accountInvitation,
			UserAccount userAccount)
		throws Exception {

		List<String> roleExternalReferenceCodes =
			accountInvitation.getRoleExternalReferenceCodes();

		if (roleExternalReferenceCodes.isEmpty()) {
			return;
		}

		Map<String, Long> accountRoleIds =
			_accountService.getAccountRoleIdsByExternalReferenceCode(
				account.getId());

		for (String roleExternalReferenceCode : roleExternalReferenceCodes) {
			Long accountRoleId = accountRoleIds.get(roleExternalReferenceCode);

			if (accountRoleId == null) {
				_log.error(
					StringBundler.concat(
						"Unable to find account role ",
						roleExternalReferenceCode, " for account ",
						account.getExternalReferenceCode()));

				continue;
			}

			_accountService.addAccountUserAccountRole(
				account.getId(), accountRoleId, userAccount.getId());
		}
	}

	private void _addProjectMembership(
			AccountInvitation accountInvitation, UserAccount userAccount)
		throws Exception {

		String projectExternalReferenceCode =
			accountInvitation.getProjectExternalReferenceCode();

		if (Validator.isNull(projectExternalReferenceCode)) {
			return;
		}

		_projectMembershipService.addProjectMembership(
			projectExternalReferenceCode,
			accountInvitation.getProjectRoleExternalReferenceCode(),
			userAccount.getId());
	}

	private static final Log _log = LogFactory.getLog(
		AccountInvitationAcceptanceService.class);

	@Autowired
	private AccountService _accountService;

	@Autowired
	private ProjectMembershipService _projectMembershipService;

	@Autowired
	private UserAccountService _userAccountService;

}