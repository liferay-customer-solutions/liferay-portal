/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {getSpecificationValue} from '~/hooks/useProjectCommerce';

import type {OrderTypes} from '~/types/orders';
import type {DeliveryProduct} from '~/types/product';

import type {ProjectItemKind} from '../types';

export type ActivationProfile =
	| 'app-licenses'
	| 'cloud-native'
	| 'commerce'
	| 'dxp-portal'
	| 'enterprise-search'
	| 'keys-list'
	| 'licenses'
	| 'none'
	| 'status';

const ACTIVATION_PROFILES: ActivationProfile[] = [
	'app-licenses',
	'cloud-native',
	'commerce',
	'dxp-portal',
	'enterprise-search',
	'keys-list',
	'licenses',
	'none',
	'status',
];

const ACTIVATION_PROFILE_BY_ORDER_TYPE: Partial<
	Record<OrderTypes, ActivationProfile>
> = {
	CLIENT_EXTENSION: 'app-licenses',
	CLOUD_APP: 'app-licenses',
	COMPOSITE_APP: 'app-licenses',
	DXP_APP: 'app-licenses',
	LOW_CODE_CONFIGURATION: 'none',
	OTHER: 'none',
};

function isActivationProfile(value: string): value is ActivationProfile {
	return (ACTIVATION_PROFILES as string[]).includes(value);
}

export function resolveActivationProfile({
	kind,
	orderType,
	product,
}: {
	kind: ProjectItemKind;
	orderType?: string;
	product: DeliveryProduct;
}): ActivationProfile {
	if (kind === 'application') {
		return (
			(orderType &&
				ACTIVATION_PROFILE_BY_ORDER_TYPE[orderType as OrderTypes]) ||
			'app-licenses'
		);
	}

	const profile = getSpecificationValue(
		product,
		'project-activation-profile'
	);

	return isActivationProfile(profile) ? profile : 'none';
}

export default resolveActivationProfile;
