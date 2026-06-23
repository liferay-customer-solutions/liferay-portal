/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {STATUS_DOT_COLORS} from './constants';

export function getStatusColor(status: string): string {
	return STATUS_DOT_COLORS[status] ?? 'var(--color-neutral-6)';
}

export default getStatusColor;
