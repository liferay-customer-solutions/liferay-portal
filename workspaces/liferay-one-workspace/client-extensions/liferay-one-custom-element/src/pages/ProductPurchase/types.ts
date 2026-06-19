/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

export enum PaymentMethodType {
	INVOICE,
	PAY_NOW,
}

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
