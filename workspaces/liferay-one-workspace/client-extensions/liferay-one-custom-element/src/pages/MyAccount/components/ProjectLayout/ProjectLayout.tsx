/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useMemo} from 'react';
import {useParams} from 'react-router-dom';
import AppLayout from '~/components/AppLayout/AppLayout';
import Breadcrumb from '~/components/Breadcrumb/Breadcrumb';
import {useProject} from '~/context/ProjectContext';
import i18n from '~/i18n';
import {buildNavItems} from '~/utils/routeUtils';

import ProjectHeader from '../../Projects/components/ProjectHeader/ProjectHeader';
import ProjectSelector from '../../Projects/components/ProjectSelector/ProjectSelector';
import {isUnassignedProject} from '../../Projects/projects';
import {projectDetailRoutes} from '../../myAccountRoutes';

export default function ProjectLayout() {
	const {accountERC} = useParams();
	const {loading, projectId, projects} = useProject();

	const navItems = useMemo(
		() =>
			buildNavItems(
				projectDetailRoutes,
				`/${accountERC}/project/${projectId}`
			),
		[accountERC, projectId]
	);

	const contentHeader = isUnassignedProject(projectId) ? undefined : (
		<ProjectHeader />
	);

	if (!loading && !projects.length) {
		return (
			<p className="text-neutral-7">
				{i18n.translate('no-projects-yet')}
			</p>
		);
	}

	return (
		<AppLayout
			breadcrumb={<Breadcrumb />}
			contentHeader={contentHeader}
			header={<ProjectSelector />}
			navItems={navItems}
		/>
	);
}
