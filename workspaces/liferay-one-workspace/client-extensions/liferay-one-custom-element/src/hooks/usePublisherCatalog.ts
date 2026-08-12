/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import useSWR, {SWRConfiguration} from 'swr';
import HeadlessCommerceAdminCatalog from '~/services/headless/HeadlessCommerceAdminCatalog';
import {Liferay} from '~/services/liferay/liferay';

import type {Catalog} from '~/types/commerce';

const usePublisherCatalog = (swrOptions?: SWRConfiguration) => {
	const accountId = Liferay.CommerceContext.account?.accountId;

	return useSWR(
		accountId ? `/publisher-catalog/${accountId}` : null,
		async () => {
			const {items} = await HeadlessCommerceAdminCatalog.getCatalogs(
				new URLSearchParams({
					pageSize: '-1',
				})
			);

			return (
				items?.find(
					(item: Catalog) => item.accountId === Number(accountId)
				) ?? null
			);
		},
		swrOptions
	);
};

export default usePublisherCatalog;
