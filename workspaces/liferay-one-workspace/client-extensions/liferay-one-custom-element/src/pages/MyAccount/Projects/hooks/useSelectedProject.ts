/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useCallback, useEffect, useState} from 'react';
import {
	getCurrentUserId,
	getSelectedAccountId,
} from '~/pages/MyAccount/Projects/utils/projectContext';
import {
	getLastViewedProjectCookie,
	setLastViewedProjectCookie,
} from '~/pages/MyAccount/Projects/utils/projectCookieUtils';
import {resolveProjectERC} from '~/pages/MyAccount/Projects/utils/resolveProjectERC';

import type {UserProject} from '~/pages/MyAccount/Projects/types';

export function useSelectedProject(
	loading: boolean,
	projects: UserProject[]
) {
	const [projectERC, setProjectERC] = useState('');

	const userId = getCurrentUserId();
	const accountId = getSelectedAccountId();

	useEffect(() => {
		if (loading || projectERC) {
			return;
		}

		const resolvedProjectERC = resolveProjectERC(
			projects,
			getLastViewedProjectCookie(accountId, userId)
		);

		if (resolvedProjectERC) {
			setProjectERC(resolvedProjectERC);
		}
	}, [accountId, loading, projectERC, projects, userId]);

	const selectProject = useCallback(
		(id: string) => {
			setProjectERC(id);

			setLastViewedProjectCookie(accountId, id, userId);
		},
		[accountId, userId]
	);

	return {projectERC, selectProject};
}
