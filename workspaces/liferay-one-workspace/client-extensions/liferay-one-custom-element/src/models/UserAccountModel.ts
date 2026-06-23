/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Liferay} from '~/services/liferay/liferay';

import type {AccountRoleType, UserAccount} from '~/types/accounts';

export class UserAccountModel {
	constructor(protected userAccount: UserAccount) {}

	get accountBriefs() {
		return this.userAccount?.accountBriefs ?? [];
	}

	get accountName() {
		return this.userAccount.name;
	}

	get accountType() {
		return this.userAccount.type;
	}

	get isAdmin() {
		return this.hasRegularRole('Administrator');
	}

	get isSolutionPublisher() {
		return this.hasAccountRole('Solution Publisher');
	}

	get isSSAAdmin() {
		return this.hasAccountRole('SSA Admin') || this.isAdmin;
	}

	get isSSAUser() {
		return this.hasAccountRole('SSA User');
	}

	private hasAccountRole(roleName: AccountRoleType) {
		return this.accountBriefs.some(
			(accountBrief) =>
				accountBrief.id ===
					Liferay.CommerceContext.account?.accountId &&
				accountBrief.roleBriefs.some(
					(roleBrief) => roleBrief.name === roleName
				)
		);
	}

	private hasRegularRole(roleName: AccountRoleType) {
		return this.userAccount?.roleBriefs.some(
			(role) => role?.name === roleName
		);
	}
}
