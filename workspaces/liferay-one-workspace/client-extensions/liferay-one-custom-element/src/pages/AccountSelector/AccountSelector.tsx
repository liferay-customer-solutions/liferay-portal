/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayIcon from '@clayui/icon';
import {useEffect, useState} from 'react';

import EntitySelector, {
	SelectorItem,
} from '../../components/EntitySelector/EntitySelector';
import {useFetch} from '../../hooks/useFetch';
import i18n from '../../i18n';
import {Liferay} from '../../liferay/liferay';

const SEARCH_DELAY = 400;

function AccountAvatar({
	logoURL,
	size = 32,
	type,
}: {
	logoURL?: string;
	size?: number;
	type?: string;
}) {

	// Liferay always returns a logoURL, falling back to a default placeholder
	// portrait (img_id=0) when no logo is set. Treat that as no logo so the
	// account-type icon shows instead.

	const hasLogo = Boolean(logoURL) && !logoURL?.includes('img_id=0');

	if (hasLogo) {
		return (
			<img
				alt=""
				className="rounded-circle"
				src={logoURL}
				style={{
					flexShrink: 0,
					height: size,
					objectFit: 'cover',
					width: size,
				}}
			/>
		);
	}

	return (
		<span
			className="align-items-center bg-light d-flex justify-content-center rounded-circle text-neutral-6"
			style={{flexShrink: 0, height: size, width: size}}
		>
			<ClayIcon
				symbol={
					type?.toLowerCase() === 'business' ? 'briefcase' : 'user'
				}
			/>
		</span>
	);
}

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

	const {data, loading} = useFetch<APIResponse<Account>>(
		currentAccountId ? '/o/headless-admin-user/v1.0/accounts' : null,
		{
			params: {
				fields: 'id,logoURL,name,type',
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

		const body = new FormData();

		body.append('accountId', accountId);

		await fetch(
			`/o/commerce-ui/set-current-account?groupId=${Liferay.ThemeDisplay.getScopeGroupId()}&p_auth=${Liferay.authToken}`,
			{
				body,
				headers: {'x-csrf-token': Liferay.authToken},
				method: 'POST',
			}
		);

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
