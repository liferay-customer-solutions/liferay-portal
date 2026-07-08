/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.admin.user.client.dto.v1_0.Account;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.one.constants.RoleConstants;
import com.liferay.one.okta.service.OktaService;
import com.liferay.one.salesforce.model.Project;
import com.liferay.one.salesforce.model.ProjectContactRole;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Kyle Bischof
 */
@Component
public class ProvisioningContactService {

	public List<Long> addProjectContacts(
		Account account, List<ProjectContactRole> projectContactRoles,
		Project salesforceProject, List<String> warningMessages) {

		List<Long> userIds = new ArrayList<>();

		boolean hasDesignatedAdministrator = _hasDesignatedAdministrator(
			projectContactRoles);

		for (ProjectContactRole projectContactRole : projectContactRoles) {
			try {
				_addProjectContact(
					account, hasDesignatedAdministrator, projectContactRole,
					salesforceProject, userIds, warningMessages);
			}
			catch (Exception exception) {
				_addWarning(
					warningMessages,
					"Unable to process project contact " +
						projectContactRole.getEmailAddress());

				_log.error(
					"Unable to process project contact " +
						projectContactRole.getEmailAddress(),
					exception);
			}
		}

		return userIds;
	}

	private void _addProjectContact(
			Account account, boolean hasDesignatedAdministrator,
			ProjectContactRole projectContactRole, Project salesforceProject,
			List<Long> userIds, List<String> warningMessages)
		throws Exception {

		String emailAddress = projectContactRole.getEmailAddress();

		if (Validator.isNull(emailAddress) ||
			_emailAddressValidatorService.isLiferayDomain(emailAddress)) {

			return;
		}

		UserAccount userAccount =
			_userAccountService.fetchUserAccountByEmailAddress(emailAddress);

		if (userAccount == null) {
			userAccount = _userAccountService.addUserAccount(
				emailAddress, projectContactRole.getLastName(),
				projectContactRole.getFirstName());

			try {
				_oktaService.createContact(
					emailAddress, projectContactRole.getFirstName(), null,
					projectContactRole.getLastName());
			}
			catch (Exception exception) {
				_log.error(
					"Unable to create Okta contact " + emailAddress, exception);
			}
		}

		if (_userAccountService.hasAccountUserAccount(
				account.getId(), userAccount.getId())) {

			return;
		}

		boolean firstAccountUser = !_userAccountService.hasAccountUserAccounts(
			account.getId());

		Long accountRoleId = _accountService.fetchAccountRoleId(
			account.getId(), projectContactRole.getContactRole());

		if (accountRoleId == null) {
			_addWarning(
				warningMessages,
				"Unable to find account role " +
					projectContactRole.getContactRole());
		}

		_accountService.addAccountUserAccount(
			account.getId(), accountRoleId, userAccount.getId());

		if (firstAccountUser && !hasDesignatedAdministrator) {
			Long administratorAccountRoleId =
				_accountService.fetchAccountRoleId(
					account.getId(), RoleConstants.NAME_ACCOUNT_ADMINISTRATOR);

			if (administratorAccountRoleId != null) {
				_accountService.addAccountUserAccountRole(
					account.getId(), administratorAccountRoleId,
					userAccount.getId());
			}
			else {
				_addWarning(
					warningMessages,
					"Unable to find account role " +
						RoleConstants.NAME_ACCOUNT_ADMINISTRATOR);
			}
		}

		if (accountRoleId != null) {
			try {
				_provisioningAssignmentService.assignAccountRole(
					account, userAccount.getId(),
					projectContactRole.getContactRole());
			}
			catch (Exception exception) {
				_log.error(
					"Unable to assign provisioning side effects for " +
						emailAddress,
					exception);
			}
		}

		if (salesforceProject != null) {
			_projectMembershipService.addProjectMembership(
				account.getId(), salesforceProject.getId(),
				userAccount.getId());
		}

		userIds.add(userAccount.getId());
	}

	private void _addWarning(
		List<String> warningMessages, String warningMessage) {

		warningMessages.add(warningMessage);

		if (_log.isWarnEnabled()) {
			_log.warn(warningMessage);
		}
	}

	private boolean _hasDesignatedAdministrator(
		List<ProjectContactRole> projectContactRoles) {

		for (ProjectContactRole projectContactRole : projectContactRoles) {
			String emailAddress = projectContactRole.getEmailAddress();

			if (Validator.isNull(emailAddress) ||
				_emailAddressValidatorService.isLiferayDomain(emailAddress)) {

				continue;
			}

			if (RoleConstants.NAME_ACCOUNT_ADMINISTRATOR.equals(
					projectContactRole.getContactRole())) {

				return true;
			}
		}

		return false;
	}

	private static final Log _log = LogFactory.getLog(
		ProvisioningContactService.class);

	@Autowired
	private AccountService _accountService;

	@Autowired
	private EmailAddressValidatorService _emailAddressValidatorService;

	@Autowired
	private OktaService _oktaService;

	@Autowired
	private ProjectMembershipService _projectMembershipService;

	@Autowired
	private ProvisioningAssignmentService _provisioningAssignmentService;

	@Autowired
	private UserAccountService _userAccountService;

}