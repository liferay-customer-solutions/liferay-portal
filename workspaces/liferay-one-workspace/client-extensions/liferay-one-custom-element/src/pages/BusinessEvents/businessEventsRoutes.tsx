/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {lazy} from 'react';
import {Navigate} from 'react-router-dom';
import {AppRoute} from '~/utils/routeUtils';

const BusinessEventsRedirect = lazy(() => import('./BusinessEventsRedirect'));
const BusinessEvents = lazy(() => import('./BusinessEvents'));
const BusinessEventsAdd = lazy(
	() => import('./BusinessEventsAdd/BusinessEventsAdd')
);
const BusinessEventsActivityHistory = lazy(
	() =>
		import('./BusinessEventsActivityHistory/BusinessEventsActivityHistory')
);
const BusinessEventsDetails = lazy(
	() => import('./BusinessEventsDetails/BusinessEventsDetails')
);
const BusinessEventsEdit = lazy(
	() => import('./BusinessEventsEdit/BusinessEventsEdit')
);

export const businessEventsRoutes: AppRoute[] = [
	{element: <BusinessEventsRedirect />, index: true},
	{
		children: [
			{element: <BusinessEvents />, index: true},
			{element: <BusinessEventsAdd />, path: 'add'},
			{
				children: [
					{element: <BusinessEventsDetails />, index: true},
					{element: <BusinessEventsEdit />, path: 'edit'},
					{
						element: <BusinessEventsActivityHistory />,
						path: 'activity-history',
					},
					{element: <Navigate replace to="." />, path: '*'},
				],
				path: ':id',
			},
			{element: <Navigate replace to="." />, path: '*'},
		],
		element: <BusinessEventsRedirect />,
		path: ':projectERC/business-events',
	},
];
