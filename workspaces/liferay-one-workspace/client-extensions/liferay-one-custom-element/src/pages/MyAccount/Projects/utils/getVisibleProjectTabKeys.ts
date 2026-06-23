/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {getSpecificationValues} from '~/hooks/useProjectCommerce';

import {PROJECT_TAB_ORDER, TAB_VISIBILITY} from './constants';

import type {OrderTypes} from '~/types/orders';
import type {DeliveryProduct} from '~/types/product';

import type {ProjectItemKind, ProjectTabKey} from '../types';

const FALLBACK_TABS: Record<ProjectItemKind, ProjectTabKey[]> = {
	application: [
		'details',
		'activation',
		'download',
		'orders',
		'help-and-support',
	],
	product: ['details', 'activation', 'orders'],
};

const ORDER_TYPE_BY_PRODUCT_CATEGORY: {[category: string]: OrderTypes} = {
	'Artificial Intelligence': 'AI_HUB',
	'Customer Data': 'CMP_BETA',
	'Platform': 'DXP',
};

function resolveOrderType(
	orderType: string | undefined,
	product?: DeliveryProduct
): OrderTypes | undefined {
	if (orderType && orderType in TAB_VISIBILITY) {
		return orderType as OrderTypes;
	}

	if (product) {
		const categories = getSpecificationValues(
			product,
			'liferay-products-categories'
		);

		for (const category of categories) {
			if (ORDER_TYPE_BY_PRODUCT_CATEGORY[category]) {
				return ORDER_TYPE_BY_PRODUCT_CATEGORY[category];
			}
		}
	}

	return undefined;
}

export function getVisibleProjectTabKeys({
	kind,
	orderType,
	product,
}: {
	kind: ProjectItemKind;
	orderType?: string;
	product?: DeliveryProduct;
}): ProjectTabKey[] {
	const resolvedOrderType = resolveOrderType(orderType, product);

	const allowedTabs =
		(resolvedOrderType && TAB_VISIBILITY[resolvedOrderType]) ??
		FALLBACK_TABS[kind];

	return PROJECT_TAB_ORDER.filter((tabKey) => allowedTabs.includes(tabKey));
}

export default getVisibleProjectTabKeys;
