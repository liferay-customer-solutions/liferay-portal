/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useMemo} from 'react';
import AppLayout from '~/components/AppLayout/AppLayout';
import {buildNavItems} from '~/utils/routeUtils';

import {adminRoutes} from './adminRoutes';

import './Admin.css';

export default function AdminLayout() {
	const adminNav = useMemo(() => buildNavItems(adminRoutes), []);

	return <AppLayout navItems={adminNav} />;
}
