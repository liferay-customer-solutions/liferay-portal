/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {lazy} from 'react';
import {Navigate} from 'react-router-dom';

import {AppRoute} from '../../../utils/routes';

const BaseRedirect = lazy(() => import('./BaseRedirect'));
const BusinessEvents = lazy(() => import('./BusinessEvents'));
const BusinessEventsAdd = lazy(
	() => import('./pages/BusinessEventsAdd/BusinessEventsAdd')
);
const BusinessEventsItemActivityHistory = lazy(
	() =>
		import(
			'./pages/BusinessEventsItem/BusinessEventsItemActivityHistory/BusinessEventsItemActivityHistory'
		)
);
const BusinessEventsItemDetails = lazy(
	() =>
		import(
			'./pages/BusinessEventsItem/BusinessEventsItemDetails/BusinessEventsItemDetails'
		)
);
const BusinessEventsItemEdit = lazy(
	() =>
		import(
			'./pages/BusinessEventsItem/BusinessEventsItemEdit/BusinessEventsItemEdit'
		)
);

export const businessEventsRoutes: AppRoute[] = [
	{element: <BaseRedirect />, index: true},
	{
		children: [
			{element: <BusinessEvents />, index: true},
			{element: <BusinessEventsAdd />, path: 'add'},
			{
				children: [
					{element: <BusinessEventsItemDetails />, index: true},
					{element: <BusinessEventsItemEdit />, path: 'edit'},
					{
						element: <BusinessEventsItemActivityHistory />,
						path: 'activity-history',
					},
					{element: <Navigate replace to="." />, path: '*'},
				],
				path: ':id',
			},
			{element: <Navigate replace to="." />, path: '*'},
		],
		path: ':accountKey/business-events',
	},
];
