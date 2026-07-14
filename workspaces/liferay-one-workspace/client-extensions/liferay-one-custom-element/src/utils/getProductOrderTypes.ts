/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import type {OrderType} from '~/types/orders';
import type {ProductType} from '~/types/product';

const productTypeERC = {
	'ai-hub': 'AI_HUB',
	'client-extension': 'CLIENT_EXTENSION',
	'cloud': 'CLOUD_APP',
	'composite-app': 'COMPOSITE_APP',
	'dxp': 'DXP_APP',
	'low-code-configuration': 'LOW_CODE_CONFIGURATION',
	'other': 'OTHER',
	'ssa-saas': 'SSA_SAAS',
} as const;

export function getProductOrderTypes(productSpecificationValue: string) {
	const productSpecification = productSpecificationValue.toLowerCase();

	return {
		externalReferenceCode:
			productTypeERC[productSpecification as ProductType] || 'NOTYPE',
	} as OrderType;
}

export default getProductOrderTypes;
