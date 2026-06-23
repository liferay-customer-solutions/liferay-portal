/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useEffect, useState} from 'react';
import AccountAvatar from '~/components/AccountAvatar/AccountAvatar';
import EntitySelector, {
	SelectorItem,
} from '~/components/EntitySelector/EntitySelector';
import {useFetch} from '~/hooks/useFetch';
import i18n from '~/i18n';
import {Liferay} from '~/services/liferay/liferay';
import {setCurrentAccount} from '~/utils/setCurrentAccount';

import type {Account} from '~/types/accounts';
import type {APIResponse} from '~/types/api';

const SEARCH_DELAY = 400;

export default function AccountSelector() {
	const account = Liferay.CommerceContext?.account;
	const currentAccountId = account?.accountId;

	const [searchValue, setSearchValue] = useState('');
	const [debouncedSearch, setDebouncedSearch] = useState('');

	useEffect(() => {
		const timeout = setTimeout(
			() => setDebouncedSearch(searchValue.trim()),
			SEARCH_DELAY
		);

		return () => clearTimeout(timeout);
	}, [searchValue]);

	const {data: currentAccount} = useFetch<Account>(
		currentAccountId
			? `/o/headless-admin-user/v1.0/accounts/${currentAccountId}`
			: null
	);

	const {data, isLoading: loading} = useFetch<APIResponse<Account>>(
		currentAccountId ? '/o/headless-admin-user/v1.0/accounts' : null,
		{
			params: {
				fields: 'externalReferenceCode,id,logoURL,name,type',
				filter: debouncedSearch
					? `contains(name, '${debouncedSearch.replace(/'/g, "''")}')`
					: undefined,
				pageSize: 20,
				sort: 'name:asc',
			},
		}
	);

	if (!Liferay.ThemeDisplay.isSignedIn() || !currentAccountId) {
		return null;
	}

	const items: SelectorItem[] = (data?.items ?? []).map((item) => ({
		icon: (
			<AccountAvatar logoURL={item.logoURL} size={24} type={item.type} />
		),
		id: String(item.id),
		name: item.name,
		subtitle: item.type,
	}));

	const name = currentAccount?.name ?? account?.accountName ?? '';

	async function handleSelect(accountId: string) {
		if (accountId === String(currentAccountId)) {
			return;
		}

		await setCurrentAccount(accountId);

		const externalReferenceCode = (data?.items ?? []).find(
			(item) => String(item.id) === accountId
		)?.externalReferenceCode;

		if (
			externalReferenceCode &&
			document.querySelector(
				'liferay-one-custom-element[route="my-account"]'
			)
		) {
			window.location.hash = `#/${externalReferenceCode}`;
		}

		window.location.reload();
	}

	return (
		<EntitySelector
			ariaLabel={i18n.translate('select-account')}
			items={items}
			label={i18n.translate('account')}
			loading={loading}
			name={name}
			onSearchChange={setSearchValue}
			onSelect={handleSelect}
			searchValue={searchValue}
			selectedId={String(currentAccountId)}
			triggerIcon={
				<AccountAvatar
					logoURL={currentAccount?.logoURL}
					size={32}
					type={currentAccount?.type}
				/>
			}
			variant="compact"
		/>
	);
}
