/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {HashRouter, Navigate, useRoutes} from 'react-router-dom';
import {ProjectProvider} from '~/context/ProjectContext';
import {toRouteObjects} from '~/utils/routeUtils';

import MyAccount from './MyAccount';
import MyAccountRedirect from './MyAccountRedirect';
import AccountGuard from './components/AccountGuard/AccountGuard';
import AccountLayout from './components/AccountLayout/AccountLayout';
import ProjectLayout from './components/ProjectLayout/ProjectLayout';
import {accountRoutes, projectDetailRoutes} from './myAccountRoutes';

function MyAccountRoutes() {
	return useRoutes([
		{
			children: [
				{element: <MyAccount />, index: true},
				{element: <MyAccountRedirect />, path: 'account-details'},
				{element: <MyAccountRedirect />, path: 'account-members'},
				{element: <MyAccountRedirect />, path: 'orders/*'},
				{
					children: [
						{
							element: <Navigate replace to="project" />,
							index: true,
						},
						{
							children: [
								{
									children:
										toRouteObjects(projectDetailRoutes),
									path: ':projectERC',
								},
							],
							element: (
								<ProjectProvider>
									<ProjectLayout />
								</ProjectProvider>
							),
							path: 'project',
						},
						{
							children: toRouteObjects(accountRoutes),
							element: <AccountLayout />,
						},
					],
					element: <AccountGuard />,
					path: ':accountERC',
				},
				{element: <Navigate replace to="/" />, path: '*'},
			],
			path: '/',
		},
	]);
}

export default function MyAccountRouter() {
	return (
		<HashRouter>
			<MyAccountRoutes />
		</HashRouter>
	);
}
