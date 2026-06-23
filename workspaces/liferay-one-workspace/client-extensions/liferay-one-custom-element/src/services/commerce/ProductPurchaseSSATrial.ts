/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ProductPurchase from './ProductPurchase';

import type {OrderTypes} from '~/types/orders';

export default class ProductPurchaseSSATrial extends ProductPurchase {
	protected orderTypeExternalReferenceCode: OrderTypes = 'SSA_SAAS';
}
