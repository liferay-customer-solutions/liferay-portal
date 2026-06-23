/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {LAST_PROJECT_STORAGE_KEY} from './constants';

export function resolveProjectId(): string {
	return localStorage.getItem(LAST_PROJECT_STORAGE_KEY) ?? '';
}

export default resolveProjectId;
