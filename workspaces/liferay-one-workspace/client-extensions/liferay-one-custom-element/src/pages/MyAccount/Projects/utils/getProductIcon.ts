/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ICON_BY_CATEGORY} from './constants';

export function getProductIcon(type: string): string {
	return ICON_BY_CATEGORY[type] ?? 'catalog';
}

export default getProductIcon;
