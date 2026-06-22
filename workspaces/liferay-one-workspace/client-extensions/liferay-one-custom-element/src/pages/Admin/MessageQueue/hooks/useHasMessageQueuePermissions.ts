/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useOneContext} from '../../../../context/OneContext';
import {AccountRoleType} from '../../../../enums/Account';

const PROVISIONING_ADMINISTRATOR_ROLE = 'Provisioning Administrator';

export default function useHasMessageQueuePermissions(): {
	hasMessageQueuePermissions: boolean;
	loading: boolean;
} {
	const {myUserAccount} = useOneContext();

	if (!myUserAccount) {
		return {hasMessageQueuePermissions: false, loading: true};
	}

	const hasMessageQueuePermissions = Boolean(
		myUserAccount.roleBriefs?.some(
			(role) =>
				role.name === AccountRoleType.ADMINISTRATOR ||
				role.name === PROVISIONING_ADMINISTRATOR_ROLE
		)
	);

	return {hasMessageQueuePermissions, loading: false};
}
