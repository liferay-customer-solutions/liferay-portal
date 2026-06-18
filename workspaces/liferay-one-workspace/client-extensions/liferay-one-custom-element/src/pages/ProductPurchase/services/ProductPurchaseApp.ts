/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Analytics} from '../../../core/Analytics';
import {ProductSpecificationKey, SkuOptions} from '../../../enums/Product';
import GetAppInformation from '../../../services/rest/GetAppInformation';
import {getProductOrderTypes} from '../../../utils/getProductOrderTypes';
import {getProductSpecificationValues} from '../../../utils/getProductSpecificationValues';
import {
	getProductPriceModel,
	getProductSpecificationValue,
	getSkuByOptionValueKey,
} from '../../../utils/productUtils';
import {getSiteURL} from '../../../utils/site';
import ProductPurchase from './ProductPurchase';

export default class ProductPurchaseApp extends ProductPurchase {
	protected analyticsTrack(): void {
		const {isFreeApp} = getProductPriceModel(this.product);

		Analytics.track('APP_PURCHASE', {
			isFreeApp,
			productName: this.product.name,
		});
	}

	public async createOrder(cart?: Cart): Promise<Cart> {
		const order = await super.createOrder(this.getAppPurchaseCart(cart));

		const {priceModel} = getProductPriceModel(this.product);

		await GetAppInformation.postGetAppInformation({
			dashboardLink: getSiteURL() + '/my-account',
			orderId: String(order.id),
			priceModel,
			productName: this.product.name,
			productType: getProductSpecificationValue(
				ProductSpecificationKey.APP_TYPE,
				this.product
			),
		}).catch(console.error);

		return order;
	}

	private getAppPurchaseCart(cart?: Cart) {
		const baseCart = {
			...cart,
			orderTypeExternalReferenceCode:
				ProductPurchaseApp.getOrderTypeExternalReferenceCode(
					this.product
				),
		} as Cart;

		if (cart) {

			// Only requests with cart are processed with payment

			return baseCart;
		}

		return {
			...baseCart,
			cartItems: this.getCartItems(
				getSkuByOptionValueKey(this.product, SkuOptions.STANDARD)?.id
			),
		} as Cart;
	}

	public async getNextStepsLink(cart: Cart) {
		return `/purchase-completed?orderId=${cart.id}`;
	}

	static getOrderTypeExternalReferenceCode(product: DeliveryProduct) {
		return getProductOrderTypes(
			getProductSpecificationValues(product?.productSpecifications || [])
		).externalReferenceCode;
	}
}
