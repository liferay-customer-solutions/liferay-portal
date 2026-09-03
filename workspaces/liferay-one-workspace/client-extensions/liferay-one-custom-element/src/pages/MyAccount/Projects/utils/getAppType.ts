/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {getSpecificationValue} from '~/hooks/useProjectCommerce';

import type {DeliveryProduct} from '~/types/product';

export type AppType =
	| 'client-extension'
	| 'cloud'
	| 'composite-app'
	| 'dxp'
	| 'low-code-configuration'
	| 'other';

const APP_TYPES: AppType[] = [
	'client-extension',
	'cloud',
	'composite-app',
	'dxp',
	'low-code-configuration',
	'other',
];

export function getAppType(product: DeliveryProduct): AppType | undefined {
	const value = getSpecificationValue(product, 'type').toLowerCase();

	return (APP_TYPES as string[]).includes(value)
		? (value as AppType)
		: undefined;
}

export default getAppType;
