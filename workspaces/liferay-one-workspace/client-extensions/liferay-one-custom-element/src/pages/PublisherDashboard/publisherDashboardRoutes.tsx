/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {lazy} from 'react';
import {Navigate} from 'react-router-dom';

import {AppRoute} from '../../utils/routes';

const PublishedApps = lazy(() => import('./PublishedApps'));
const PublishedSolutions = lazy(() => import('./PublishedSolutions'));
const PublisherProfile = lazy(() => import('./PublisherProfile'));
const PublisherProfileEdit = lazy(() => import('./PublisherProfileEdit'));

export const publisherDashboardRoutes: AppRoute[] = [
	{element: <Navigate replace to="published-apps" />, index: true},
	{
		element: <PublishedApps />,
		nav: {icon: 'catalog', label: 'Published Apps'},
		path: 'published-apps',
	},
	{
		element: <PublishedSolutions />,
		nav: {icon: 'list', label: 'Published Solutions'},
		path: 'published-solutions',
	},
	{
		children: [
			{element: <PublisherProfile />, index: true},
			{element: <PublisherProfileEdit />, path: 'edit'},
			{element: <Navigate replace to="." />, path: '*'},
		],
		nav: {icon: 'user', label: 'Publisher Profile'},
		path: 'publisher-profile',
	},
	{element: <Navigate replace to="published-apps" />, path: '*'},
];
