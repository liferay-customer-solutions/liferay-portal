/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useFetch} from '~/hooks/useFetch';
import {
	MANAGEABLE_ACCOUNT_ROLES,
	PARTNER_ACCOUNT_ROLES,
	STANDARD_ACCOUNT_ROLES,
} from '~/pages/MyAccount/AccountMembers/accountRoles';
import {useHasProject} from '~/pages/MyAccount/Projects/hooks/useHasProject';
import {Liferay} from '~/services/liferay/liferay';

import type {Account} from '~/types/accounts';

const PARTNER_ACCOUNT_TYPES = [
	'Marketplace Developer',
	'Strategic Partner',
	'Technology Partner',
];

type AccountType = {
	isHybrid: boolean;
	isPartner: boolean;
	loading: boolean;
	roleNames: string[];
};

export function useAccountType(): AccountType {
	const accountId = Liferay.CommerceContext?.account?.accountId;

	const {data: account, isLoading} = useFetch<Account>(
		accountId ? `/o/headless-admin-user/v1.0/accounts/${accountId}` : null
	);

	const {hasProject, loading: projectsLoading} = useHasProject();

	const accountTypeCustomField = account?.customFields?.find(
		({name}) => name === 'AccountType'
	);

	const accountTypeValue = accountTypeCustomField?.customValue?.data;

	const isPartner = PARTNER_ACCOUNT_TYPES.some((partnerAccountType) =>
		Array.isArray(accountTypeValue)
			? accountTypeValue.includes(partnerAccountType)
			: accountTypeValue === partnerAccountType
	);

	const isHybrid = isPartner && hasProject;

	let roleNames = STANDARD_ACCOUNT_ROLES;

	if (isHybrid) {
		roleNames = MANAGEABLE_ACCOUNT_ROLES;
	}
	else if (isPartner) {
		roleNames = PARTNER_ACCOUNT_ROLES;
	}

	return {
		isHybrid,
		isPartner,
		loading: isLoading || projectsLoading,
		roleNames,
	};
}
