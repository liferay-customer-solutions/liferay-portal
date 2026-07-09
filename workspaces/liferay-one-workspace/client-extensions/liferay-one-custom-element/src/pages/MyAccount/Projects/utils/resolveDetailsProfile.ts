/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {
	getSpecificationValue,
	getSpecificationValues,
} from '~/hooks/useProjectCommerce';

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

const DETAILS_PROFILES: DetailsProfile[] = [
	'analytics',
	'basic',
	'basic-incident',
	'dates-status',
	'env-commerce',
	'env-instance',
	'paas',
	'saas',
];

function isDetailsProfile(value: string): value is DetailsProfile {
	return (DETAILS_PROFILES as string[]).includes(value);
}

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

	const profile = getSpecificationValue(product, 'project-details-profile');

	if (isDetailsProfile(profile)) {
		return profile;
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
