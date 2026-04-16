/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.customer.util;

import com.liferay.customer.constants.ContactRoleConstants;
import com.liferay.customer.constants.RoleConstants;
import com.liferay.headless.admin.user.client.dto.v1_0.AccountRole;
import com.liferay.osb.koroneiki.phloem.rest.client.dto.v1_0.ContactRole;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Felipe Franca
 */
public class AccountRoleUtil {

	public static Map<String, Long> getContactAccountRoleIdMap(
		Collection<AccountRole> accountRoles) {

		Map<String, Long> accountRoleIdMap = new HashMap<>();

		for (AccountRole accountRole : accountRoles) {
			if (ArrayUtil.contains(
					RoleConstants.NAMES_CONTACT_ACCOUNT_ROLES,
					accountRole.getName())) {

				accountRoleIdMap.put(
					accountRole.getName(), accountRole.getId());
			}
		}

		return accountRoleIdMap;
	}

	public static List<Long> getContactAccountRoleIds(
		Map<String, Long> accountRoleIdMap, ContactRole[] contactRoles) {

		List<Long> accountRoleIds = new ArrayList<>();

		if (ArrayUtil.isEmpty(contactRoles)) {
			return accountRoleIds;
		}

		for (ContactRole contactRole : _filterContactRoles(contactRoles)) {
			Long accountRoleId = _getAccountRoleId(
				accountRoleIdMap, contactRole.getName());

			if (accountRoleId != null) {
				accountRoleIds.add(accountRoleId);
			}
		}

		return accountRoleIds;
	}

	private static ContactRole[] _filterContactRoles(
		ContactRole[] contactRoles) {

		List<ContactRole> filteredContactRoles = new ArrayList<>();

		for (ContactRole contactRole : contactRoles) {
			if (ArrayUtil.contains(
					ContactRoleConstants.NAMES_CONTACT_ROLES,
					contactRole.getName())) {

				filteredContactRoles.add(contactRole);
			}
		}

		return filteredContactRoles.toArray(new ContactRole[0]);
	}

	private static Long _getAccountRoleId(
		Map<String, Long> accountRoleIdMap, String contactRoleName) {

		if (Validator.isNull(contactRoleName)) {
			return null;
		}

		if (contactRoleName.equals(
				ContactRoleConstants.NAME_LIFERAY_CUSTOMER_SUCCESS) ||
			contactRoleName.equals(ContactRoleConstants.NAME_LIFERAY_SALES)) {

			return accountRoleIdMap.get(RoleConstants.NAME_PROVISIONING);
		}
		else if (contactRoleName.equals(
					ContactRoleConstants.NAME_SUPPORT_ADMINISTRATOR)) {

			return accountRoleIdMap.get(
				RoleConstants.NAME_ACCOUNT_ADMINISTRATOR);
		}
		else if (contactRoleName.equals(
					ContactRoleConstants.NAME_SUPPORT_REQUESTER)) {

			return accountRoleIdMap.get(RoleConstants.NAME_REQUESTER);
		}
		else if (contactRoleName.equals(
					ContactRoleConstants.NAME_SUPPORT_USER)) {

			return accountRoleIdMap.get(RoleConstants.NAME_ACCOUNT_MEMBER);
		}

		return accountRoleIdMap.get(contactRoleName);
	}

}