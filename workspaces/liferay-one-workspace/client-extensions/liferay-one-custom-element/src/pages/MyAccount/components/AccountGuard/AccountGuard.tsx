/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayLoadingIndicator from '@clayui/loading-indicator';
import {useEffect, useState} from 'react';
import {Outlet, useParams} from 'react-router-dom';
import EmptyState from '~/components/EmptyState/EmptyState';
import {useFetch} from '~/hooks/useFetch';
import {translate} from '~/i18n';
import {Liferay} from '~/services/liferay/liferay';
import {setCurrentAccount} from '~/utils/setCurrentAccount';

import type {Account} from '~/types/accounts';

export default function AccountGuard() {
	const {accountERC} = useParams();

	const currentAccountId = Liferay.CommerceContext.account?.accountId;

	const [switching, setSwitching] = useState(false);

	const {
		data: account,
		error,
		isLoading: loading,
	} = useFetch<Account>(
		accountERC
			? `/o/headless-admin-user/v1.0/accounts/by-external-reference-code/${accountERC}`
			: null
	);

	const needsSwitch =
		account !== undefined &&
		currentAccountId !== undefined &&
		String(account.id) !== String(currentAccountId);

	useEffect(() => {
		if (!needsSwitch || !account) {
			return;
		}

		setSwitching(true);

		setCurrentAccount(String(account.id))
			.then(() => window.location.reload())
			.catch(() => setSwitching(false));
	}, [account, needsSwitch]);

	if (error) {
		return (
			<EmptyState
				className="mt-5"
				title={translate('you-do-not-have-access-to-this-account')}
				type="NO_ACCESS"
			/>
		);
	}

	if (loading || switching || needsSwitch) {
		return (
			<div className="mx-auto p-4">
				<ClayLoadingIndicator size="sm" />
			</div>
		);
	}

	return <Outlet />;
}
