/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {getSpecificationValues} from '~/hooks/useProjectCommerce';

import type {DeliveryProduct} from '~/types/product';

import type {ProjectItemKind} from '../types';

export type DetailsProfile =
	| 'analytics'
	| 'basic'
	| 'basic-incident'
	| 'dates-status'
	| 'env-commerce'
	| 'env-instance'
	| 'paas'
	| 'saas';

const DETAILS_PROFILE_BY_PRODUCT_NAME: {[name: string]: DetailsProfile} = {
	'Add Ons': 'dates-status',
	'Analytics Cloud': 'analytics',
	'Cloud Native': 'dates-status',
	'Commerce': 'env-commerce',
	'Content Marketing Platform': 'basic',
	'DXP': 'env-instance',
	'Digital Sales Room': 'basic',
	'Enterprise Search': 'dates-status',
	'Liferay AI Hub': 'basic',
	'Liferay DXP - Free Tier': 'basic',
	'Liferay Data Platform': 'basic-incident',
	'Other': 'dates-status',
	'PaaS': 'paas',
	'Portal': 'env-instance',
	'SaaS': 'saas',
};

export function resolveDetailsProfile({
	kind,
	product,
}: {
	kind: ProjectItemKind;
	product: DeliveryProduct;
}): DetailsProfile {

	if (kind === 'application') {
		return 'basic';
	}

	const profileByName = DETAILS_PROFILE_BY_PRODUCT_NAME[product.name];

	if (profileByName) {
		return profileByName;
	}

	const categories = getSpecificationValues(
		product,
		'liferay-products-categories'
	);

	if (categories.includes('Platform')) {
		return 'env-instance';
	}

	return 'basic';
}

export default resolveDetailsProfile;
