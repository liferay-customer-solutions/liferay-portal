/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {getSpecificationValue} from '~/hooks/useProjectCommerce';

import type {DeliveryProduct} from '~/types/product';

export type UtilizationProfile =
	| 'none'
	| 'paas-dashboard'
	| 'saas-dashboard'
	| 'usage-metrics';

const UTILIZATION_PROFILES: UtilizationProfile[] = [
	'none',
	'paas-dashboard',
	'saas-dashboard',
	'usage-metrics',
];

function isUtilizationProfile(value: string): value is UtilizationProfile {
	return (UTILIZATION_PROFILES as string[]).includes(value);
}

export function resolveUtilizationProfile(
	product: DeliveryProduct
): UtilizationProfile {
	const profile = getSpecificationValue(
		product,
		'project-utilization-profile'
	);

	return isUtilizationProfile(profile) ? profile : 'none';
}

export default resolveUtilizationProfile;
