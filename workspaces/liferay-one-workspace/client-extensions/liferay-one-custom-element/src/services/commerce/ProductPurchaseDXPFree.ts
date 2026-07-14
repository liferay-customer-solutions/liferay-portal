/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {z} from 'zod';
import commerceSchemas from '~/schema/commerceSchemas';
import DXPFreeActivationKeyRequests from '~/services/objects/DXPFreeActivationKeyRequests';
import LicenseKeys from '~/services/spring-boot/LicenseKeys';

import ProductPurchase from './ProductPurchase';

import type {Account} from '~/types/accounts';
import type {Cart, OrderTypes} from '~/types/orders';
import type {DeliveryProduct} from '~/types/product';

export type ActivationKeyFormData = z.infer<
	typeof commerceSchemas.activationKey
>;

export default class ProductPurchaseDXPFree extends ProductPurchase {
	protected orderTypeExternalReferenceCode: OrderTypes = 'DXP';

	constructor(
		account: Account,
		product: DeliveryProduct,
		private readonly form: ActivationKeyFormData
	) {
		super(account, product);
	}

	public async createOrder(cart?: Cart): Promise<Cart> {
		const order = await super.createOrder(cart);

		const owner = this.form.businessEmailAddress;

		await LicenseKeys.createLicenseKeyTypeFree({
			domains: this.form.domain,
			orderId: String(order.id),
			owner,
		});

		await DXPFreeActivationKeyRequests.createDXPFreeActivationKeyRequest({
			businessEmailAddress: this.form.businessEmailAddress,
			companyName: this.form.companyName,
			country: this.form.country,
			domain: this.form.domain,
			extension: this.form.extension,
			fullName: this.form.fullName,
			intlCode: this.form.intlCode?.code,
			jobTitle: this.form.jobTitle,
			notifyMe: this.form.notifyMeAboutProducts,
			phoneNumber: this.form.phoneNumber,
			purpose: this.form.purpose,
			r_orderToDXPFreeActivationKeyRequest_commerceOrderId: String(
				order.id
			),
		}).catch(console.error);

		return order;
	}

	public async getNextStepsLink(cart: Cart) {
		return `/purchase-completed?orderId=${cart.id}`;
	}
}
