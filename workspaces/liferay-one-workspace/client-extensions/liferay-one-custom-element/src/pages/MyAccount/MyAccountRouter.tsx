/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {HashRouter, Navigate, useRoutes} from 'react-router-dom';

import ContentLayout from '../../components/ContentLayout';
import {ProjectProvider} from '../../context/ProjectContext';
import {toRouteObjects} from '../../utils/routes';
import MyAccountIndex from './MyAccountIndex';
import MyAccountLayout from './MyAccountLayout';
import {accountRoutes, projectDetailRoutes} from './myAccountRoutes';

function MyAccountRoutes() {
	return useRoutes([
		{
			children: [
				{element: <MyAccountIndex />, index: true},
				{
					children: toRouteObjects(projectDetailRoutes),
					element: <MyAccountLayout />,
					path: 'project',
				},
				{
					children: toRouteObjects(accountRoutes),
					element: <ContentLayout />,
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
			<ProjectProvider>
				<MyAccountRoutes />
			</ProjectProvider>
		</HashRouter>
	);
}
