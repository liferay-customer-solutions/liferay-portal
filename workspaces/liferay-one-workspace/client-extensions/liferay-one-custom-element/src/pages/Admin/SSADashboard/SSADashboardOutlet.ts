/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import useSWR, {KeyedMutator} from 'swr';

import {useOneContext} from '../../../context/OneContext';
import SearchBuilder from '../../../core/SearchBuilder';
import {OrderTypes, OrderWorkflowStatusCode} from '../../../enums/Order';
import {usePlacedOrders} from '../../../hooks/data/usePlacedOrder';
import HeadlessAdminUser from '../../../services/rest/HeadlessAdminUser';
import {useSSATrialsExtend} from './hooks/useSSATrialsExtend';

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
			properties.accountExternalReferenceCode
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
			.eq('orderTypeExternalReferenceCode', OrderTypes.SSA_SAAS)
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
