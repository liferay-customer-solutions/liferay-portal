/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useMemo} from 'react';
import AppLayout from '~/components/AppLayout/AppLayout';
import {buildNavItems} from '~/utils/routeUtils';

import {publisherDashboardRoutes} from '../../publisherDashboardRoutes';
import PublisherDashboardBreadcrumb from '../PublisherDashboardBreadcrumb/PublisherDashboardBreadcrumb';

export default function PublisherDashboardLayout() {
	const navItems = useMemo(() => buildNavItems(publisherDashboardRoutes), []);

	return (
		<AppLayout
			breadcrumb={<PublisherDashboardBreadcrumb />}
			navItems={navItems}
		/>
	);
}
