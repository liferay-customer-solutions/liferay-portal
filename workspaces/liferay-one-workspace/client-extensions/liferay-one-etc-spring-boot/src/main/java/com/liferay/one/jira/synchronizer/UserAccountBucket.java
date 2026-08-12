/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.jira.synchronizer;

import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Drew Brokke
 */
public class UserAccountBucket {

	public void addCustomerUserAccount(UserAccount userAccount) {
		_customerUserAccounts.add(userAccount);
	}

	public void addWorkerUserAccount(UserAccount userAccount) {
		_workerUserAccounts.add(userAccount);
	}

	public List<UserAccount> getCustomerUserAccounts() {
		return Collections.unmodifiableList(_customerUserAccounts);
	}

	public List<UserAccount> getWorkerUserAccounts() {
		return Collections.unmodifiableList(_workerUserAccounts);
	}

	private final List<UserAccount> _customerUserAccounts = new ArrayList<>();
	private final List<UserAccount> _workerUserAccounts = new ArrayList<>();

}