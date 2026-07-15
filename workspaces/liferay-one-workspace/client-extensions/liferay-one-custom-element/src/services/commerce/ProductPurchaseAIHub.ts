/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {z} from 'zod';
import {adminSchemas as zodSchema} from '~/schema/adminSchemas';
import {OrderCustomFields} from '~/utils/orderUtils';
import {getSiteURL} from '~/utils/siteUtils';

import ProductPurchase from './ProductPurchase';

import type {Account} from '~/types/accounts';
import type {Cart, OrderTypes} from '~/types/orders';
import type {DeliveryProduct} from '~/types/product';
import type {SalesforceProject} from '~/types/salesforceProject';

type AIHubForm = z.infer<typeof zodSchema.aiHubForm>;

export class ProductPurchaseAIHub extends ProductPurchase {
	private form?: AIHubForm;
	protected orderTypeExternalReferenceCode: OrderTypes = 'AI_HUB';

	constructor(
		account: Account,
		product: DeliveryProduct,
		private salesforceProject?: SalesforceProject | null
	) {
		super(account, product);
	}

	setForm(form: AIHubForm) {
		this.form = form;
	}

	protected getCart() {
		const baseCart = super.getCart();
		const cartItems = super.getCartItems();

		return {
			...baseCart,
			cartItems,
			customFields: {
				...baseCart?.customFields,
				[OrderCustomFields.ORDER_METADATA]: JSON.stringify({
					aiHubForm: this.form,
					...(this.salesforceProject
						? {
								salesforceProjectId:
									this.salesforceProject
										.externalReferenceCode,
							}
						: {}),
				}),
			},
		} as Cart;
	}

	public async createOrder() {
		if (!this.form) {
			throw new Error('Form is missing.');
		}

		const cart = this.getCart();

		const order = await super.createOrder(cart);

		return order;
	}

	public async getNextStepsLink(cart: Cart) {
		if (cart.orderTypeExternalReferenceCode !== 'AI_HUB') {
			return super.getNextStepsLink(cart);
		}

		return `${window.location.origin}${getSiteURL()}/next-steps?orderId=${cart.id}`;
	}
}
