/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useMemo, useState} from 'react';
import useSWR from 'swr';
import {useOneContext} from '~/context/OneContextProvider';
import fetcher from '~/services/fetcher/fetcher';
import HeadlessAdminUser from '~/services/headless/HeadlessAdminUser';
import {Liferay} from '~/services/liferay/liferay';

import type {Account} from '~/types/accounts';

const useAccounts = () => {
	const {myUserAccount} = useOneContext();
	const [selectedAccount, setSelectedAccount] = useState<Account>({
		id: Liferay.CommerceContext.account?.accountId as number,
		name: Liferay.CommerceContext.account?.accountName as string,
	} as Account);

	const accountBriefs = useMemo(
		() => myUserAccount?.accountBriefs || [],
		[myUserAccount?.accountBriefs]
	);

	const {data: accounts = [], isLoading} = useSWR(
		{accountBriefs, key: '/accounts-briefs/'},
		() =>
			Promise.all(
				accountBriefs.map((accountBrief) =>
					fetcher<Account>(
						`/o/headless-admin-user/v1.0/accounts/${accountBrief.id}?nestedFields=accountUserAccounts`
					)
				)
			)
	);

	const {data: selectedAccountWithERC} = useSWR(
		selectedAccount.id && !selectedAccount.externalReferenceCode
			? {accounts, key: '/accounts-with-erc/', selectedAccount}
			: null,
		async ({selectedAccount}) => {
			const account = accounts.find(({id}) => selectedAccount.id === id);

			if (account) {
				return account;
			}

			return HeadlessAdminUser.getAccount(selectedAccount.id);
		}
	);

	return {
		accounts,
		isLoading,
		selectedAccount: (selectedAccountWithERC || selectedAccount) as Account,
		setSelectedAccount,
	};
};

export default useAccounts;
