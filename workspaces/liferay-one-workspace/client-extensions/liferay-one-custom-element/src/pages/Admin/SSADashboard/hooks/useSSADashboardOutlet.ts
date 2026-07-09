/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import useSWR, {KeyedMutator} from 'swr';
import {useOneContext} from '~/context/OneContextProvider';
import {usePlacedOrders} from '~/hooks/usePlacedOrder';
import HeadlessAdminUser from '~/services/headless/HeadlessAdminUser';
import SearchBuilder from '~/utils/SearchBuilder';
import {OrderWorkflowStatusCode} from '~/utils/orderUtils';

import {useSSATrialsExtend} from './useSSATrialsExtend';

import type {Account} from '~/types/accounts';
import type {APIResponse} from '~/types/api';
import type {TrialExtend} from '~/types/trial';

type SSADashboardOutletContext = {
	myTrialsInProgress: number;
	selectedAccountId: number;
	ssaAccount: Account;
	ssaTrialExtend: APIResponse<TrialExtend>;
	ssaTrialExtendMutate: KeyedMutator<APIResponse<TrialExtend>>;
};

export function useSSADashboardOutlet(): SSADashboardOutletContext {
	const {myUserAccount, properties} = useOneContext();

	const {data: ssaAccount} = useSWR('/ssa-account', () =>
		HeadlessAdminUser.getAccountByExternalReferenceCode(
			properties.ssaAccountExternalReferenceCode
		)
	);

	const isFilterByAuthorIdEnabled =
		properties.featureFlags?.includes('LPD-63837');

	const authorFilter = isFilterByAuthorIdEnabled ? 'authorId' : 'author';

	const authorFilterValue = isFilterByAuthorIdEnabled
		? myUserAccount?.id
		: myUserAccount?.name;

	const {data: inProgressTrialResponse = {totalCount: 0}} = usePlacedOrders({
		accountId: ssaAccount?.id as number,
		filter: new SearchBuilder()
			.eq(authorFilter, authorFilterValue, {
				unquote: isFilterByAuthorIdEnabled,
			})
			.and()
			.eq('orderTypeExternalReferenceCode', 'SSA_SAAS')
			.and()
			.lambda('orderStatus', OrderWorkflowStatusCode.IN_PROGRESS, {
				unquote: true,
			})
			.build(),
		page: 1,
		pageSize: 1,
		shouldFetch: !!ssaAccount,
	});

	const {data: ssaTrialExtend, mutate: ssaTrialExtendMutate} =
		useSSATrialsExtend(ssaAccount!);

	return {
		myTrialsInProgress: inProgressTrialResponse.totalCount,
		selectedAccountId: ssaAccount?.id as number,
		ssaAccount: ssaAccount as Account,
		ssaTrialExtend: ssaTrialExtend as APIResponse<TrialExtend>,
		ssaTrialExtendMutate,
	};
}
