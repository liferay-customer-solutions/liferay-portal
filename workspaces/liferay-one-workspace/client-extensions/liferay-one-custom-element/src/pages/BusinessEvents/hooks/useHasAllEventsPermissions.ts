/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useOneContext} from '~/context/OneContextProvider';

const ACCOUNT_ADMIN_ROLE = 'Account Administrator';
const ACCOUNT_REQUESTER_ROLE = 'Account Requester';
const LIFERAY_STAFF_ROLE = 'Liferay Staff';

export default function useHasAllEventsPermissions(accountKey?: string): {
	hasAllEventsPermissions: boolean;
	loading: boolean;
} {
	const {myUserAccount} = useOneContext();

	if (!myUserAccount) {
		return {hasAllEventsPermissions: false, loading: true};
	}

	const isAdministrator = myUserAccount.roleBriefs?.some(
		(role) => role.name === 'Administrator'
	);

	const isLiferayStaff = myUserAccount.roleBriefs?.some(
		(role) => role.name === LIFERAY_STAFF_ROLE
	);

	const hasAccountRole = myUserAccount.accountBriefs?.some(
		(accountBrief) =>
			accountBrief.externalReferenceCode === accountKey &&
			accountBrief.roleBriefs?.some((role) =>
				[ACCOUNT_ADMIN_ROLE, ACCOUNT_REQUESTER_ROLE].includes(role.name)
			)
	);

	return {
		hasAllEventsPermissions: Boolean(
			isAdministrator || isLiferayStaff || hasAccountRole
		),
		loading: false,
	};
}
