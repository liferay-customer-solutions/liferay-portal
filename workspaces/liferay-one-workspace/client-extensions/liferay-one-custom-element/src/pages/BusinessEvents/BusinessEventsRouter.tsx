/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Suspense} from 'react';
import {HashRouter, useRoutes} from 'react-router-dom';
import {toRouteObjects} from '~/utils/routeUtils';

import {businessEventsRoutes} from './businessEventsRoutes';

function BusinessEventsRoutes() {
	return useRoutes(toRouteObjects(businessEventsRoutes));
}

export default function BusinessEventsRouter() {
	return (
		<HashRouter>
			<Suspense fallback={null}>
				<BusinessEventsRoutes />
			</Suspense>
		</HashRouter>
	);
}
