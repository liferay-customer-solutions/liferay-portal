/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {lazy} from 'react';
import {Navigate} from 'react-router-dom';
import {AppRoute} from '~/utils/routeUtils';

const AccountDetails = lazy(() => import('./AccountDetails/AccountDetails'));
const AccountMembers = lazy(() => import('./AccountMembers/AccountMembers'));
const OrderDetails = lazy(() => import('./Orders/OrderDetails/OrderDetails'));
const OrderHistory = lazy(() => import('./Orders/OrderHistory/OrderHistory'));
const Orders = lazy(() => import('./Orders/Orders'));
const Applications = lazy(() => import('./Projects/Applications/Applications'));
const ProjectItemDetails = lazy(
	() => import('./Projects/ProjectItemDetails/ProjectItemDetails')
);
const Products = lazy(() => import('./Projects/Products/Products'));

export const projectDetailRoutes: AppRoute[] = [
	{element: <Navigate replace to="products" />, index: true},
	{
		children: [
			{element: <Products />, index: true},
			{
				element: <ProjectItemDetails kind="product" />,
				path: ':productERC',
			},
			{element: <Navigate replace to="." />, path: '*'},
		],
		nav: {icon: 'products', label: 'Products'},
		path: 'products',
	},
	{
		children: [
			{element: <Applications />, index: true},
			{
				element: <ProjectItemDetails kind="application" />,
				path: ':applicationERC',
			},
			{element: <Navigate replace to="." />, path: '*'},
		],
		nav: {icon: 'applications', label: 'Applications'},
		path: 'applications',
	},
	{element: <Navigate replace to="products" />, path: '*'},
];

const orderRoutes: AppRoute[] = [
	{element: <Orders />, index: true},
	{element: <OrderHistory />, path: 'history'},
	{element: <OrderDetails />, path: ':orderId'},
	{element: <Navigate replace to="." />, path: '*'},
];

export const accountRoutes: AppRoute[] = [
	{children: orderRoutes, path: 'orders'},
	{element: <AccountDetails />, path: 'account-details'},
	{element: <AccountMembers />, path: 'account-members'},
];
