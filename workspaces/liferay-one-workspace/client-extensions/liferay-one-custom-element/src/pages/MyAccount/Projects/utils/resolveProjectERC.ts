/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import type {UserProject} from '~/pages/MyAccount/Projects/types';

export function resolveProjectERC(
	projects: UserProject[],
	cookieERC?: string
): string | undefined {
	const isAccessible = (externalReferenceCode?: string) =>
		Boolean(externalReferenceCode) &&
		projects.some(
			(project) =>
				project.externalReferenceCode === externalReferenceCode
		);

	if (isAccessible(cookieERC)) {
		return cookieERC;
	}

	return projects[0]?.externalReferenceCode;
}
