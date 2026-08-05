/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {lazy} from 'react';
import {Navigate, Outlet} from 'react-router-dom';

import NewAppContextProvider from '~/context/NewAppContext';
import usePublisherCatalog from '~/hooks/usePublisherCatalog';
import {AppRoute} from '~/utils/routeUtils';

const PublishedApps = lazy(() => import('./PublishedApps/PublishedApps'));
const PublishedSolutions = lazy(
	() => import('./PublishedSolutions/PublishedSolutions')
);
const PublisherProfile = lazy(
	() => import('./PublisherProfile/PublisherProfile')
);
const PublisherProfileEdit = lazy(
	() => import('./PublisherProfileEdit/PublisherProfileEdit')
);

const PublishAppOutlet = lazy(
	() => import('./pages/NewAppFlow/PublishAppOutlet')
);
const Create = lazy(() => import('./pages/NewAppFlow/pages/Create'));
const AppProfile = lazy(() => import('./pages/NewAppFlow/pages/Profile'));
const Build = lazy(() => import('./pages/NewAppFlow/pages/Build'));
const Storefront = lazy(() => import('./pages/NewAppFlow/pages/Storefront'));
const Version = lazy(() => import('./pages/NewAppFlow/pages/Version'));
const Pricing = lazy(() => import('./pages/NewAppFlow/pages/Pricing'));
const Licensing = lazy(() => import('./pages/NewAppFlow/pages/Licensing'));
const LicensePrices = lazy(
	() => import('./pages/NewAppFlow/pages/Licensing/LicensePrices')
);
const Support = lazy(() => import('./pages/NewAppFlow/pages/Support'));
const SubmitApp = lazy(() => import('./pages/NewAppFlow/pages/Submit'));

function NewAppContextWrapper() {
	const {data: catalog} = usePublisherCatalog();

	return (
		<NewAppContextProvider catalog={catalog}>
			<Outlet />
		</NewAppContextProvider>
	);
}

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
	{
		children: [
			{
				children: [
					{
						children: [
							{element: <Create />, index: true},
							{element: <AppProfile />, path: 'profile'},
							{element: <Build />, path: 'build'},
							{element: <Storefront />, path: 'storefront'},
							{element: <Version />, path: 'version'},
							{element: <Pricing />, path: 'pricing'},
							{element: <Licensing />, path: 'licensing'},
							{
								element: <LicensePrices />,
								path: 'licensing-prices',
							},
							{element: <Support />, path: 'support'},
							{element: <SubmitApp />, path: 'submit'},
						],
						element: <PublishAppOutlet />,
						path: 'publisher',
					},
				],
				element: <NewAppContextWrapper />,
				path: ':productId?',
			},
		],
		path: 'newapp',
	},
	{element: <Navigate replace to="published-apps" />, path: '*'},
];
