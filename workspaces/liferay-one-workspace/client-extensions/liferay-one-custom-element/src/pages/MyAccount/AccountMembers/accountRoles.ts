/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import type {UserAccountModel} from '~/models/UserAccountModel';
import type {RoleBrief} from '~/types/accounts';

export const ACCOUNT_ADMINISTRATOR = 'Account Administrator';
export const ACCOUNT_BUYER = 'Account Buyer';
export const ACCOUNT_MEMBER = 'Account Member';
export const PARTNER_ACCOUNT_ADMIN = 'Partner Account Admin';
export const PARTNER_MANAGER = 'Partner Manager';
export const PARTNER_MARKETING_USER = 'Partner Marketing User';
export const PARTNER_MEMBER = 'Partner Member';
export const PARTNER_SALES_USER = 'Partner Sales User';
export const PARTNER_TECHNICAL_USER = 'Partner Technical User';

export const STANDARD_ACCOUNT_ROLES = [
	ACCOUNT_ADMINISTRATOR,
	ACCOUNT_BUYER,
	ACCOUNT_MEMBER,
];

export const PARTNER_ACCOUNT_ROLES = [
	PARTNER_ACCOUNT_ADMIN,
	PARTNER_MANAGER,
	PARTNER_MARKETING_USER,
	PARTNER_MEMBER,
	PARTNER_SALES_USER,
	PARTNER_TECHNICAL_USER,
];

export const MANAGEABLE_ACCOUNT_ROLES = [
	...STANDARD_ACCOUNT_ROLES,
	...PARTNER_ACCOUNT_ROLES,
];

export const CRITICAL_INCIDENT_CONTACT = 'Critical Incident Contact';
export const PAAS_USER = 'PaaS User';
export const PRIVACY_BREACH_CONTACT = 'Privacy Breach Contact';
export const SECURITY_BREACH_CONTACT = 'Security Breach Contact';

export const CLOUD_CONTACT_DESIGNATIONS = [
	CRITICAL_INCIDENT_CONTACT,
	PAAS_USER,
	PRIVACY_BREACH_CONTACT,
	SECURITY_BREACH_CONTACT,
];

export function isAdministratorRole(roleName: string) {
	return (
		roleName === ACCOUNT_ADMINISTRATOR || roleName === PARTNER_ACCOUNT_ADMIN
	);
}

export function getMembershipRoleNames(roleBriefs: RoleBrief[] = []) {
	const roleNames = new Set(roleBriefs.map(({name}) => name));

	return MANAGEABLE_ACCOUNT_ROLES.filter((roleName) =>
		roleNames.has(roleName)
	);
}

export function hasAdministratorRole(roleBriefs: RoleBrief[] = []) {
	return roleBriefs.some(({name}) => isAdministratorRole(name));
}

export function hasAnyAccountRole(userAccountModel?: UserAccountModel | null) {
	return MANAGEABLE_ACCOUNT_ROLES.some((roleName) =>
		userAccountModel?.hasAccountRoleName(roleName)
	);
}

export function isAccountManager(userAccountModel?: UserAccountModel | null) {
	return Boolean(
		userAccountModel?.hasAccountRoleName(ACCOUNT_ADMINISTRATOR) ||
			userAccountModel?.hasAccountRoleName(PARTNER_ACCOUNT_ADMIN) ||
			userAccountModel?.isAdmin
	);
}

export function canEditAccountDetails(
	userAccountModel?: UserAccountModel | null
) {
	return isAccountManager(userAccountModel);
}

export function canAccessAccountMembers(
	userAccountModel?: UserAccountModel | null
) {
	if (isAccountManager(userAccountModel)) {
		return true;
	}

	return !(
		userAccountModel?.hasAccountRoleName(ACCOUNT_BUYER) &&
		!userAccountModel?.hasAccountRoleName(ACCOUNT_MEMBER)
	);
}

export function canAccessOrders(userAccountModel?: UserAccountModel | null) {
	return (
		isAccountManager(userAccountModel) ||
		hasAnyAccountRole(userAccountModel)
	);
}
