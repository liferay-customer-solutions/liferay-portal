/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import CommerceUI from '~/services/headless/CommerceUI';
import HeadlessCommerceDeliveryCart from '~/services/headless/HeadlessCommerceDeliveryCart';
import {Analytics} from '~/services/liferay/Analytics';
import {Liferay} from '~/services/liferay/liferay';

import type {Account} from '~/types/accounts';
import type {Cart, OrderTypes} from '~/types/orders';
import type {DeliveryProduct} from '~/types/product';

export default class ProductPurchase {
	protected orderTypeExternalReferenceCode?: OrderTypes;
	protected HeadlessCommerceDeliveryCart = HeadlessCommerceDeliveryCart;

	constructor(
		protected readonly account: Account,
		protected readonly product: DeliveryProduct
	) {}

	protected getCart() {
		return {
			accountId: this.account?.id,
			cartItems: this.getCartItems(),
			currencyCode: Liferay.CommerceContext.currency.currencyCode,
			orderTypeExternalReferenceCode: this.orderTypeExternalReferenceCode,
		} as Cart;
	}

	public async getNextStepsLink(cart: Cart) {
		return `/next-steps?orderId=${cart.id}`;
	}

	protected getCartItems(skuId = this.product.skus[0]?.id) {
		return [
			{
				price: {
					currency: Liferay.CommerceContext.currency.currencyCode,
					discount: 0,
				},
				productId: this.product.productId,
				quantity: 1,
				settings: {
					maxQuantity: 1,
				},
				skuId,
			},
		];
	}

	protected analyticsTrack() {
		Analytics.track('ORDER_CREATION', {
			accountId: this.account.id,
			orderTypeExternalReferenceCode: this.orderTypeExternalReferenceCode,
			productName: this.product.name,
		});
	}

	public async createOrder(cart?: Cart, _options?: unknown): Promise<Cart> {
		const body = {
			...this.getCart(),
			...cart,
		};

		const newCart = await (cart?.id
			? HeadlessCommerceDeliveryCart.updateCart(cart.id, body)
			: HeadlessCommerceDeliveryCart.createCart(
					Liferay.CommerceContext.commerceChannelId,
					body
				));

		await Promise.all([
			CommerceUI.selectAccount(this.account.id),
			HeadlessCommerceDeliveryCart.checkoutCart(newCart.id),
		]);

		this.analyticsTrack();

		return newCart;
	}
}
