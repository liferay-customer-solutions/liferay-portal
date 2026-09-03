/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import type {OrderTypes} from '~/types/orders';

const APPLICATION_ORDER_TYPES: OrderTypes[] = [
	'CLIENT_EXTENSION',
	'CLOUD_APP',
	'COMPOSITE_APP',
	'DXP_APP',
	'LOW_CODE_CONFIGURATION',
	'OTHER',
];

export function isApplicationOrderType(orderType?: string): boolean {
	return (APPLICATION_ORDER_TYPES as string[]).includes(orderType ?? '');
}

export default isApplicationOrderType;
