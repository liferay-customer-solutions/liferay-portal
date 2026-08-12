/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import useSWR, {SWRConfiguration} from 'swr';
import HeadlessCommerceAdminCatalog from '~/services/headless/HeadlessCommerceAdminCatalog';
import {Liferay} from '~/services/liferay/liferay';

import type {Catalog} from '~/types/commerce';

const MAX_PAGES = 20;
const PAGE_SIZE = 100;

async function findCatalogByAccountId(accountId: number) {
	for (let page = 1; page <= MAX_PAGES; page++) {
		const response = await HeadlessCommerceAdminCatalog.getCatalogs(
			new URLSearchParams({
				page: `${page}`,
				pageSize: `${PAGE_SIZE}`,
			})
		);

		const catalog = response.items?.find(
			(item: Catalog) => item.accountId === accountId
		);

		if (catalog) {
			return catalog;
		}

		if (!response.items?.length || page >= response.lastPage) {
			return null;
		}
	}

	return null;
}

const usePublisherCatalog = (swrOptions?: SWRConfiguration) => {
	const accountId = Liferay.CommerceContext.account?.accountId;

	return useSWR(
		accountId ? `/publisher-catalog/${accountId}` : null,
		() => findCatalogByAccountId(Number(accountId)),
		swrOptions
	);
};

export default usePublisherCatalog;
