/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useLayoutEffect} from 'react';
import {HashRouter, useRoutes} from 'react-router-dom';
import MarketplaceContextProvider from '~/context/MarketplaceContextProvider';
import {useOneContext} from '~/context/OneContextProvider';
import {toRouteObjects} from '~/utils/routeUtils';

import PublisherDashboardLayout from './components/PublisherDashboardLayout/PublisherDashboardLayout';
import {
	PUBLISH_FLOW_PATHS,
	publisherDashboardRoutes,
} from './publisherDashboardRoutes';

import type {AppRoute} from '~/utils/routeUtils';

const isPublishFlowRoute = (route: AppRoute) =>
	!!route.path && PUBLISH_FLOW_PATHS.includes(route.path);

function PublisherDashboardRoutes() {
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
			children: toRouteObjects(
				publisherDashboardRoutes.filter(
					(route) => !isPublishFlowRoute(route)
				)
			),
			element: <PublisherDashboardLayout />,
			path: '/',
		},
		{
			children: toRouteObjects(
				publisherDashboardRoutes.filter(isPublishFlowRoute)
			),
			path: '/',
		},
	]);
}

export default function PublisherDashboardRouter() {
	const {properties} = useOneContext();

	return (
		<MarketplaceContextProvider properties={properties}>
			<HashRouter>
				<PublisherDashboardRoutes />
			</HashRouter>
		</MarketplaceContextProvider>
	);
}
