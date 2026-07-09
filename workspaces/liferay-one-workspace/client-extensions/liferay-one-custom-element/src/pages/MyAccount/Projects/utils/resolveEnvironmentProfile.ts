/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {getSpecificationValue} from '~/hooks/useProjectCommerce';

import type {DeliveryProduct} from '~/types/product';

export type EnvironmentProfile =
	| 'ac-token'
	| 'none'
	| 'paas'
	| 'saas'
	| 'workspace';

const ENVIRONMENT_PROFILES: EnvironmentProfile[] = [
	'ac-token',
	'none',
	'paas',
	'saas',
	'workspace',
];

function isEnvironmentProfile(value: string): value is EnvironmentProfile {
	return (ENVIRONMENT_PROFILES as string[]).includes(value);
}

export function resolveEnvironmentProfile(
	product: DeliveryProduct
): EnvironmentProfile {
	const profile = getSpecificationValue(
		product,
		'project-environment-profile'
	);

	return isEnvironmentProfile(profile) ? profile : 'none';
}

export default resolveEnvironmentProfile;
