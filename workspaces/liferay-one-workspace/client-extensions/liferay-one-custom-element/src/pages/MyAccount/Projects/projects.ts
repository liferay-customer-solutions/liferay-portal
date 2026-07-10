/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

export {useHasProject} from './hooks/useHasProject';
export {useSelectedProject} from './hooks/useSelectedProject';
export {useUserProjects} from './hooks/useUserProjects';
export type {ProjectItemKind, ProjectTabKey, UserProject} from './types';
export {
	LAST_PROJECT_STORAGE_KEY,
	UNASSIGNED_PROJECT_ERC,
} from './utils/constants';
export {isUnassignedProject} from './utils/isUnassignedProject';
export {
	getCurrentUserId,
	getSelectedAccountId,
} from './utils/projectContextUtils';
export {
	getLastViewedProjectCookie,
	setLastViewedProjectCookie,
} from './utils/projectCookieUtils';
export {resolveDefaultProject} from './utils/resolveDefaultProject';
export {resolveProjectERC} from './utils/resolveProjectERC';
export {resolveProjectId} from './utils/resolveProjectId';
