/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import type {BillingAddress} from '~/types/orders';

export const PaymentMethodType = {
	INVOICE: 0,
	PAY_NOW: 1,
} as const;

export type PaymentMethodType =
	(typeof PaymentMethodType)[keyof typeof PaymentMethodType];

export type ProductPurchaseInvoice = {
	email: string;
	purchaseOrderNumber: string;
};

export type ProductPurchasePayment = {
	billingAddress: BillingAddress;
	invoice: ProductPurchaseInvoice;
	taxId: string;
	type: PaymentMethodType;
};
