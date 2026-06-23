/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {UNASSIGNED_PROJECT_ERC} from './constants';

export function isUnassignedProject(
	projectExternalReferenceCode: string
): boolean {
	return projectExternalReferenceCode === UNASSIGNED_PROJECT_ERC;
}

export default isUnassignedProject;
