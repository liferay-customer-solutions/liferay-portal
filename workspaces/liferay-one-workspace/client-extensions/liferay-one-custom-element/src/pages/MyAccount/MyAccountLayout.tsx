/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useMemo} from 'react';

import AppLayout from '../../components/AppLayout';
import Breadcrumb from '../../components/Breadcrumb';
import {buildNavItems} from '../../utils/routes';
import ProjectHeader from './Projects/ProjectHeader';
import ProjectSelector from './Projects/ProjectSelector';
import {projectDetailRoutes} from './myAccountRoutes';

export default function MyAccountLayout() {
	const navItems = useMemo(
		() => buildNavItems(projectDetailRoutes, '/project'),
		[]
	);

	return (
		<AppLayout
			breadcrumb={<Breadcrumb />}
			contentHeader={<ProjectHeader />}
			header={<ProjectSelector />}
			navItems={navItems}
		/>
	);
}
