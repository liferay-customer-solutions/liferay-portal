/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import useSWR, {SWRConfiguration} from 'swr';

import {Liferay} from '../liferay/liferay';
import HeadlessAdminUser from '../services/rest/HeadlessAdminUser';

const useAccountDetails = (swrOptions?: SWRConfiguration) => {
	const accountId = Liferay.CommerceContext.account?.accountId;

	return useSWR(
		accountId ? `/account-details/${accountId}` : null,
		async () => {
			const [account, postalAddresses] = await Promise.all([
				HeadlessAdminUser.getAccount(accountId!),
				HeadlessAdminUser.getAccountPostalAddresses(accountId!),
			]);

			return {account, postalAddresses};
		},
		swrOptions
	);
};

export default useAccountDetails;
