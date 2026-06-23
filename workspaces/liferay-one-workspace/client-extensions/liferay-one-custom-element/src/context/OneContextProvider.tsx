/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ReactNode, createContext, useContext} from 'react';
import useSWR, {KeyedMutator} from 'swr';
import {UserAccountModel} from '~/models/UserAccountModel';
import HeadlessAdminUser from '~/services/headless/HeadlessAdminUser';
import {Liferay} from '~/services/liferay/liferay';
import {Properties} from '~/utils/attributeUtils';

import type {UserAccount} from '~/types/accounts';
import type {Channel} from '~/types/commerce';

type Context = {
	channel: Channel;
	mutateMyUserAccount: KeyedMutator<UserAccount | undefined>;
	myUserAccount: UserAccount;
	properties: Properties;
	userAccountModel: UserAccountModel;
};

type OneContextProviderProps = {
	children: ReactNode;
	properties: Properties;
};

const channel = {
	channelId: Number(Liferay.CommerceContext?.commerceChannelId),
	currencyCode: Liferay.CommerceContext?.currency?.currencyCode,
	externalReferenceCode: 'MARKETPLACE',
	id: Number(Liferay.CommerceContext?.commerceChannelId),
} as Channel;

const OneContext = createContext<Context>({} as Context);

const OneContextProvider: React.FC<OneContextProviderProps> = ({
	children,
	properties,
}) => {
	const {data: myUserAccount, mutate} = useSWR(
		Liferay.ThemeDisplay.isSignedIn() ? '/one/my-user-account' : null,
		HeadlessAdminUser.getMyUserAccount
	);

	return (
		<OneContext.Provider
			value={
				{
					channel,
					mutateMyUserAccount: mutate as KeyedMutator<UserAccount>,
					myUserAccount,
					properties,
					userAccountModel: new UserAccountModel(
						myUserAccount as UserAccount
					),
				} as Context
			}
		>
			{children}
		</OneContext.Provider>
	);
};

const useOneContext = () => {
	return useContext(OneContext);
};

export {OneContext, useOneContext};

export default OneContextProvider;
