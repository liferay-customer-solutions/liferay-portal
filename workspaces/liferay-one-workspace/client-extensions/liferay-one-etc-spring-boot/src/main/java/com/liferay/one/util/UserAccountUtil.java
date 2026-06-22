/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.util;

import com.liferay.headless.admin.user.client.custom.field.CustomField;
import com.liferay.headless.admin.user.client.custom.field.CustomValue;
import com.liferay.headless.admin.user.client.dto.v1_0.AccountBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.RoleBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.portal.kernel.util.GetterUtil;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author Felipe Veloso
 */
public class UserAccountUtil {

	public static Set<String> getAccountRoleNames(
		UserAccount userAccount, long accountId) {

		Set<String> roleNames = new HashSet<>();

		AccountBrief[] accountBriefs = userAccount.getAccountBriefs();

		if (accountBriefs == null) {
			return roleNames;
		}

		for (AccountBrief accountBrief : accountBriefs) {
			Long briefAccountId = accountBrief.getId();

			if ((briefAccountId == null) || (briefAccountId != accountId)) {
				continue;
			}

			RoleBrief[] roleBriefs = accountBrief.getRoleBriefs();

			if (roleBriefs == null) {
				continue;
			}

			for (RoleBrief roleBrief : roleBriefs) {
				roleNames.add(roleBrief.getName());
			}
		}

		return roleNames;
	}

	public static boolean hasAccountRole(
		UserAccount userAccount, long accountId, String[] roleNames) {

		Set<String> accountRoleNames = getAccountRoleNames(
			userAccount, accountId);

		for (String roleName : roleNames) {
			if (accountRoleNames.contains(roleName)) {
				return true;
			}
		}

		return false;
	}

	public static boolean isVerified(UserAccount userAccount) {
		CustomField[] customFields = userAccount.getCustomFields();

		if (customFields == null) {
			return false;
		}

		for (CustomField customField : customFields) {
			if (!Objects.equals(customField.getName(), "verified")) {
				continue;
			}

			CustomValue customValue = customField.getCustomValue();

			if (customValue == null) {
				return false;
			}

			return GetterUtil.getBoolean(customValue.getData());
		}

		return false;
	}

}