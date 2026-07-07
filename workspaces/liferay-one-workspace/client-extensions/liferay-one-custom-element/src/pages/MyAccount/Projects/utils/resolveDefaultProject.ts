/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import resolveProjectId from './resolveProjectId';

import type {UserProject} from '~/pages/MyAccount/Projects/types';

export function resolveDefaultProject(
	projects: UserProject[]
): UserProject | undefined {
	const lastProjectId = resolveProjectId();

	return (
		projects.find(
			(project) => project.externalReferenceCode === lastProjectId
		) ?? projects[0]
	);
}

export default resolveDefaultProject;
