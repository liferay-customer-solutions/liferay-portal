/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ReactNode, lazy} from 'react';

import i18n from '../../i18n';

const AccountSelection = lazy(() => import('./pages/AccountSelection'));
const License = lazy(() => import('./pages/License'));
const Summary = lazy(() => import('./pages/Summary'));

export type ProductPurchaseRoute = {
	element: ReactNode;
	index?: boolean;
	isPaidOnly?: boolean;
	path?: string;
	title: string;
};

export function getProductPurchaseRoutes(isPaidApp: boolean) {
	const routes: ProductPurchaseRoute[] = [
		{
			element: <AccountSelection />,
			index: true,
			title: i18n.translate('account'),
		},
		{
			element: <License />,
			isPaidOnly: true,
			path: 'license',
			title: i18n.translate('license-selection'),
		},
		{
			element: <Summary />,
			path: 'summary',
			title: i18n.translate('summary'),
		},
	];

	return routes.filter((route) => {
		if (isPaidApp) {
			return true;
		}

		return !route.isPaidOnly;
	});
}
