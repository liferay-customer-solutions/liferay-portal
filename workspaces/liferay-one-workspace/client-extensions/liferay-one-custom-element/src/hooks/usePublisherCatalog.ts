/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import useSWR, {SWRConfiguration} from 'swr';

import SearchBuilder from '../core/SearchBuilder';
import {Liferay} from '../liferay/liferay';
import HeadlessCommerceAdminCatalog from '../services/rest/HeadlessCommerceAdminCatalog';

/**
 * Resolves the Commerce catalog owned by the currently selected account. The
 * publisher's catalog scopes every published app and solution, so it is the
 * anchor for the rest of the dashboard.
 */
const usePublisherCatalog = (swrOptions?: SWRConfiguration) => {
	const accountId = Liferay.CommerceContext.account?.accountId;

	return useSWR(
		accountId ? `/publisher-catalog/${accountId}` : null,
		async () => {
			const {items} = await HeadlessCommerceAdminCatalog.getCatalogs(
				new URLSearchParams({
					filter: SearchBuilder.unquote(
						SearchBuilder.eq('accountId', accountId!)
					),
				})
			);

			return items?.[0] ?? null;
		},
		swrOptions
	);
};

export default usePublisherCatalog;
