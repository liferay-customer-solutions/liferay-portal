/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {lazy} from 'react';
import {Navigate} from 'react-router-dom';

import {AppRoute} from '../../utils/routes';

const AccountDetails = lazy(() => import('./AccountDetails'));
const AccountMembers = lazy(() => import('./AccountMembers'));
const OrderHistory = lazy(() => import('./Orders/OrderHistory'));
const Orders = lazy(() => import('./Orders/Orders'));
const ApplicationDetails = lazy(() => import('./Projects/ApplicationDetails'));
const Applications = lazy(() => import('./Projects/Applications'));
const ProductDetails = lazy(() => import('./Projects/ProductDetails'));
const Products = lazy(() => import('./Projects/Products'));

export const projectDetailRoutes: AppRoute[] = [
	{element: <Navigate replace to="products" />, index: true},
	{
		children: [
			{element: <Products />, index: true},
			{element: <ProductDetails />, path: ':productId'},
			{element: <Navigate replace to="." />, path: '*'},
		],
		nav: {icon: 'products', label: 'Products'},
		path: 'products',
	},
	{
		children: [
			{element: <Applications />, index: true},
			{element: <ApplicationDetails />, path: ':applicationId'},
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
	{element: <Navigate replace to="." />, path: '*'},
];

export const accountRoutes: AppRoute[] = [
	{children: orderRoutes, path: 'orders'},
	{element: <AccountDetails />, path: 'account-details'},
	{element: <AccountMembers />, path: 'account-members'},
];
