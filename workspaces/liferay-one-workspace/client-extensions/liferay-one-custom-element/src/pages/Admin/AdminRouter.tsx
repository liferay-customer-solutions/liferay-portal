/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useLayoutEffect} from 'react';
import {HashRouter, useRoutes} from 'react-router-dom';
import {toRouteObjects} from '~/utils/routeUtils';

import AdminLayout from './AdminLayout';
import {adminRoutes} from './adminRoutes';

function AdminRoutes() {
	useLayoutEffect(() => {
		if (!window.location.pathname.endsWith('/')) {
			window.history.replaceState(
				null,
				'',
				`${window.location.pathname}/${window.location.hash}`
			);
		}
	}, []);

	return useRoutes([
		{
			children: toRouteObjects(adminRoutes),
			element: <AdminLayout />,
			path: '/',
		},
	]);
}

export default function AdminRouter() {
	return (
		<HashRouter>
			<AdminRoutes />
		</HashRouter>
	);
}
