/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {HashRouter, useRoutes} from 'react-router-dom';
import {toRouteObjects} from '~/utils/routeUtils';

import PublisherDashboardLayout from './components/PublisherDashboardLayout/PublisherDashboardLayout';
import {publisherDashboardRoutes} from './publisherDashboardRoutes';

function PublisherDashboardRoutes() {
	return useRoutes([
		{
			children: toRouteObjects(publisherDashboardRoutes),
			element: <PublisherDashboardLayout />,
			path: '/',
		},
	]);
}

export default function PublisherDashboardRouter() {
	return (
		<HashRouter>
			<PublisherDashboardRoutes />
		</HashRouter>
	);
}
