/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {productPurchaseStore} from '~/pages/ProductPurchase/store/AppPurchaseStore';
import {OrderCustomFields} from '~/utils/orderUtils';
import {Analytics} from '~/services/liferay/Analytics';
import GetAppInformations from '~/services/objects/GetAppInformations';
import {getProductOrderTypes} from '~/utils/getProductOrderTypes';
import {getProductSpecificationValues} from '~/utils/getProductSpecificationValues';
import {
	ProductSpecificationKey,
	getProductPriceModel,
	getProductSpecificationValue,
	getSkuByOptionValueKey,
} from '~/utils/productUtils';
import {getSiteURL} from '~/utils/siteUtils';

import ProductPurchase from './ProductPurchase';

import type {Cart} from '~/types/orders';
import type {DeliveryProduct} from '~/types/product';

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

		await GetAppInformations.postGetAppInformation({
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
		const snapshot = productPurchaseStore.getSnapshot();
		const salesforceProject = snapshot.context.salesforceProject;

		const baseCart = {
			...cart,
			customFields: {
				...cart?.customFields,
				...(salesforceProject
					? {
							[OrderCustomFields.ORDER_METADATA]: JSON.stringify({
								salesforceProjectId:
									salesforceProject.externalReferenceCode,
							}),
						}
					: {}),
			},
			orderTypeExternalReferenceCode:
				ProductPurchaseApp.getOrderTypeExternalReferenceCode(
					this.product
				),
		} as Cart;

		if (cart) {
			return baseCart;
		}

		return {
			...baseCart,
			cartItems: this.getCartItems(
				getSkuByOptionValueKey(this.product, 'standard')?.id
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
