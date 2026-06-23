/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayLoadingIndicator from '@clayui/loading-indicator';
import {Navigate} from 'react-router-dom';
import {useFetch} from '~/hooks/useFetch';
import {Liferay} from '~/services/liferay/liferay';

import type {Account} from '~/types/accounts';

export default function MyAccount() {
	const currentAccountId = Liferay.CommerceContext.account?.accountId;

	const {data: account, isLoading: loading} = useFetch<Account>(
		currentAccountId
			? `/o/headless-admin-user/v1.0/accounts/${currentAccountId}`
			: null
	);

	if (account) {
		return (
			<Navigate
				replace
				to={`/${account.externalReferenceCode}/project`}
			/>
		);
	}

	if (!currentAccountId || !loading) {
		return null;
	}

	return (
		<div className="mx-auto p-4">
			<ClayLoadingIndicator size="sm" />
		</div>
	);
}
