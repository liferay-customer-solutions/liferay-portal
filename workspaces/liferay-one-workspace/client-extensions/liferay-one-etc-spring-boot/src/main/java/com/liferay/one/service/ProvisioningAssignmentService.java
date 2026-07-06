/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.admin.user.client.dto.v1_0.Account;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.one.constants.PropertyConstants;
import com.liferay.one.constants.RoleConstants;
import com.liferay.one.okta.service.OktaService;
import com.liferay.one.util.UserAccountUtil;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.workflow.WorkflowConstants;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Felipe Veloso
 */
@Component
public class ProvisioningAssignmentService {

	public void assignAccountRole(
			Account account, long userId, String accountRoleName)
		throws Exception {

		UserAccount userAccount = _userAccountService.getUserAccount(userId);

		String emailAddress = userAccount.getEmailAddress();

		_addOktaGroupMembership(_OKTA_GROUP_CUSTOMERS, emailAddress);

		if (ArrayUtil.contains(
				RoleConstants.NAMES_PARTNER_ACCOUNT_ROLES, accountRoleName)) {

			_addOktaGroupMembership(_OKTA_GROUP_PARTNERS, emailAddress);

			_provisioningEmailService.sendPartnerUserUpdateEmail(
				account, userAccount, accountRoleName, _ROLE_ACTION_ASSIGNED);
		}

		if (Objects.equals(
				accountRoleName, RoleConstants.NAME_CLOUD_NATIVE_CONTACT)) {

			String oktaApplicationId = _propertyService.getPropertyValue(
				account.getId(), PropertyConstants.NAME_OKTA_APPLICATION);

			if (Validator.isNotNull(oktaApplicationId)) {
				_oktaService.assignUserToApplication(
					oktaApplicationId, emailAddress);
			}
		}
	}

	public void assignCustomerGroup(long userId) throws Exception {
		UserAccount userAccount = _userAccountService.getUserAccount(userId);

		_addOktaGroupMembership(
			_OKTA_GROUP_CUSTOMERS, userAccount.getEmailAddress());
	}

	public void unassignAccountMembership(long accountId, long userId)
		throws Exception {

		UserAccount userAccount = _userAccountService.getUserAccount(userId);

		String emailAddress = userAccount.getEmailAddress();

		_subscriptionEntryService.deleteAccountLicenseKeySubscriptionEntries(
			accountId, userId);

		if (!_isCustomer(userAccount)) {
			_oktaService.removeMembership(_OKTA_GROUP_CUSTOMERS, emailAddress);
		}

		if (!_isPartner(userAccount)) {
			_oktaService.removeMembership(_OKTA_GROUP_PARTNERS, emailAddress);
		}
	}

	public void unassignAccountRole(
			Account account, long userId, String accountRoleName)
		throws Exception {

		UserAccount userAccount = _userAccountService.getUserAccount(userId);

		String emailAddress = userAccount.getEmailAddress();

		if (ArrayUtil.contains(
				RoleConstants.NAMES_PARTNER_ACCOUNT_ROLES, accountRoleName)) {

			if (!_isPartner(userAccount)) {
				_oktaService.removeMembership(
					_OKTA_GROUP_PARTNERS, emailAddress);
			}

			_provisioningEmailService.sendPartnerUserUpdateEmail(
				account, userAccount, accountRoleName, _ROLE_ACTION_UNASSIGNED);
		}

		if (Objects.equals(
				accountRoleName, RoleConstants.NAME_CLOUD_NATIVE_CONTACT)) {

			String oktaApplicationId = _propertyService.getPropertyValue(
				account.getId(), PropertyConstants.NAME_OKTA_APPLICATION);

			if (Validator.isNotNull(oktaApplicationId)) {
				_oktaService.unassignUserFromApplication(
					oktaApplicationId, emailAddress);
			}
		}
	}

	private void _addOktaGroupMembership(String groupName, String emailAddress)
		throws Exception {

		Integer status = _oktaService.fetchContactStatusByEmailAddress(
			emailAddress);

		if (status == null) {
			return;
		}

		if (status == WorkflowConstants.STATUS_INACTIVE) {
			_oktaService.activateUser(emailAddress);
		}
		else {
			_oktaService.addMembership(groupName, emailAddress);
		}
	}

	private boolean _isCustomer(UserAccount userAccount) {
		return ArrayUtil.isNotEmpty(userAccount.getAccountBriefs());
	}

	private boolean _isPartner(UserAccount userAccount) {
		return UserAccountUtil.hasAccountRole(
			userAccount, RoleConstants.NAMES_PARTNER_ACCOUNT_ROLES);
	}

	private static final String _OKTA_GROUP_CUSTOMERS = "Customers";

	private static final String _OKTA_GROUP_PARTNERS = "Partners";

	private static final String _ROLE_ACTION_ASSIGNED = "Assigned";

	private static final String _ROLE_ACTION_UNASSIGNED = "Unassigned";

	@Autowired
	private OktaService _oktaService;

	@Autowired
	private PropertyService _propertyService;

	@Autowired
	private ProvisioningEmailService _provisioningEmailService;

	@Autowired
	private SubscriptionEntryService _subscriptionEntryService;

	@Autowired
	private UserAccountService _userAccountService;

}