/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useOneContext} from '~/context/OneContextProvider';

const ADMINISTRATOR_ROLE = 'Administrator';
const PROVISIONING_ADMINISTRATOR_ROLE = 'Provisioning Administrator';

export default function useHasAdminPermissions(): {
	hasAdminPermissions: boolean;
	loading: boolean;
} {
	const {myUserAccount} = useOneContext();

	if (!myUserAccount) {
		return {hasAdminPermissions: false, loading: true};
	}

	const hasAdminPermissions = Boolean(
		myUserAccount.roleBriefs?.some(
			(role) =>
				role.name === ADMINISTRATOR_ROLE ||
				role.name === PROVISIONING_ADMINISTRATOR_ROLE
		)
	);

	return {hasAdminPermissions, loading: false};
}
