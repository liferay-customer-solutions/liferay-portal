/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {LOGO_COLORS} from './constants';

export function getLogoColor(seed: string): string {
	let hash = 0;

	for (let index = 0; index < seed.length; index++) {
		hash = (hash * 31 + seed.charCodeAt(index)) % 9973;
	}

	return LOGO_COLORS[hash % LOGO_COLORS.length];
}

export default getLogoColor;
