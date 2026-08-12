/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ReactNode, createContext, useContext} from 'react';
import useSWR, {KeyedMutator} from 'swr';
import {MarketplaceUserAccount} from '~/models/MarketplaceUserAccount';
import HeadlessAdminUser from '~/services/headless/HeadlessAdminUser';
import {Liferay} from '~/services/liferay/liferay';
import {Properties} from '~/utils/attributeUtils';

import type {UserAccount} from '~/types/accounts';
import type {Channel} from '~/types/commerce';

type Context = {
	channel: Channel;
	marketplaceUserAccount: MarketplaceUserAccount;
	mutateMyUserAccount: KeyedMutator<UserAccount | undefined>;
	myUserAccount: UserAccount;
	properties: Properties;
};

type MarketplaceContextProviderProps = {
	children: ReactNode;
	properties: Properties;
};

const channel = {
	channelId: Number(Liferay.CommerceContext.commerceChannelId),
	currencyCode: Liferay.CommerceContext.currency.currencyCode,
	externalReferenceCode: 'MARKETPLACE',
	id: Number(Liferay.CommerceContext.commerceChannelId),
} as Channel;

const MarketplaceContext = createContext<Context>({} as Context);

const MarketplaceContextProvider: React.FC<MarketplaceContextProviderProps> = ({
	children,
	properties,
}) => {
	const {data: myUserAccount, mutate} = useSWR(
		Liferay.ThemeDisplay.isSignedIn()
			? '/marketplace/my-user-account'
			: null,
		HeadlessAdminUser.getMyUserAccount
	);

	return (
		<MarketplaceContext.Provider
			value={
				{
					channel,
					marketplaceUserAccount: new MarketplaceUserAccount(
						myUserAccount as UserAccount
					),
					mutateMyUserAccount: mutate as KeyedMutator<UserAccount>,
					myUserAccount,
					properties,
				} as Context
			}
		>
			{children}
		</MarketplaceContext.Provider>
	);
};

const useMarketplaceContext = () => {
	return useContext(MarketplaceContext);
};

export {useMarketplaceContext, MarketplaceContext};

export default MarketplaceContextProvider;
