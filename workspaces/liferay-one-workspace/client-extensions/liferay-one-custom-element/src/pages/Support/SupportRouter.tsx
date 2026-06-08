/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {HashRouter, useRoutes} from 'react-router-dom';

import {toRouteObjects} from '../../utils/routes';
import SupportLayout from './SupportLayout';
import {supportRoutes} from './supportRoutes';

function SupportRoutes() {
	return useRoutes([
		{
			children: toRouteObjects(supportRoutes),
			element: <SupportLayout />,
			path: '/',
		},
	]);
}

export default function SupportRouter() {
	return (
		<HashRouter>
			<SupportRoutes />
		</HashRouter>
	);
}
