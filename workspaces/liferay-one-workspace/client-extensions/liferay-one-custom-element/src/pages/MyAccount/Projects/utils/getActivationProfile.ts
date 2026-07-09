/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

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

const ACTIVATION_PROFILE_BY_PRODUCT_NAME: {[name: string]: ActivationProfile} = {
	'AI Hub': 'none',
	'Add Ons': 'none',
	'Analytics Cloud': 'status',
	'Cloud Native': 'cloud-native',
	'Commerce': 'commerce',
	'Content Marketing Platform': 'licenses',
	'DXP': 'dxp-portal',
	'Digital Sales Room': 'licenses',
	'Enterprise Search': 'enterprise-search',
	'Liferay AI Hub': 'none',
	'Liferay DXP - Free Tier': 'keys-list',
	'Liferay Data Platform': 'licenses',
	'Low-code Configuration': 'none',
	'Other': 'none',
	'PaaS': 'status',
	'Portal': 'dxp-portal',
	'SaaS': 'status',
};

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

	return ACTIVATION_PROFILE_BY_PRODUCT_NAME[product.name] ?? 'none';
}

export default resolveActivationProfile;
