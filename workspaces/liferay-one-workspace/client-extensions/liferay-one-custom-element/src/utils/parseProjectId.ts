/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

export function parseProjectId(projectId?: string) {
	const separatorIndex = projectId?.lastIndexOf('-') ?? -1;

	if (!projectId || separatorIndex <= 0) {
		return {environment: '', projectName: projectId ?? ''};
	}

	return {
		environment: projectId.slice(separatorIndex + 1),
		projectName: projectId.slice(0, separatorIndex),
	};
}

export default parseProjectId;
