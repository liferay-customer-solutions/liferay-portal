/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Navigate} from 'react-router-dom';

import {AppRoute} from '../../utils/routes';
import {attachmentsRoutes} from './Attachments/attachmentsRoutes';
import {businessEventsRoutes} from './BusinessEvents/businessEventsRoutes';

export const supportRoutes: AppRoute[] = [
	...businessEventsRoutes,
	...attachmentsRoutes,
	{element: <Navigate replace to="." />, path: '*'},
];
