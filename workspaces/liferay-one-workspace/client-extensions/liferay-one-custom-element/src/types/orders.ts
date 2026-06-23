/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

export type BillingAddress = {
	city?: string;
	country?: string;
	countryISOCode?: string;
	description?: string;
	name?: string;
	phoneNumber?: string;
	regionISOCode?: string;
	saveAddress?: boolean;
	street1?: string;
	street2?: string;
	vatNumber?: string;
	zip?: string;
};

export type Cart = {
	accountId: number;
	author?: string;
	billingAddress: BillingAddress;
	cartItems: CartItem[];
	currencyCode: string;
	customFields: Record<string, unknown>;
	id: number;
	orderStatusInfo: {
		[key: string]: string;
	};
	orderTypeExternalReferenceCode: string;
	orderTypeId: number;
	paymentMethod?: string;
	paymentMethodLabel?: string;
	paymentStatusInfo: {
		[key: string]: string;
	};
	paymentStatusLabel?: string;
	purchaseOrderNumber?: string;
	shippingAddress: BillingAddress;
	summary: {
		subtotalFormatted: string;
		taxValueFormatted: string;
		totalFormatted: string;
	};
};

export type CartItem = {
	customFields?: {};
	price: {
		currency: string;
		discount: number;
		finalPrice?: number;
		price?: number;
	};
	productId?: number;
	quantity: number;
	settings: {
		maxQuantity: number;
	};
	sku?: string;
	skuId: number;
};

export type Order = {
	account: {
		id: number;
		name: string;
		taxId: string;
		type: string;
	};
	accountExternalReferenceCode?: string;
	accountId: number;
	billingAddress?: BillingAddress;
	billingAddressId?: number;
	channel: {
		currencyCode?: string;
		id: number;
		type: string;
	};
	channelExternalReferenceCode?: string;
	channelId: number;
	createDate?: string;
	creatorEmailAddress?: string;
	currencyCode: string;
	customFields?: {
		[key: string]: string;
	};
	externalReferenceCode?: string;
	id: number;
	marketplaceOrderType?: string;
	modifiedDate?: string;
	orderDate?: string;
	orderItems: OrderItem[];
	orderStatus: number;
	orderStatusInfo: {
		code: number;
		label: string;
		label_i18n: string;
	};
	orderTypeExternalReferenceCode?: string;
	orderTypeId?: number;
	paymentMethod?: string;
	paymentStatus?: number;
	paymentStatusInfo: PaymentStatusInfo;
	placedOrderBillingAddress?: BillingAddress;
	placedOrderBillingAddressId?: number;
	placedOrderItems?: PlacedOrderItems[];
	projectName?: string;
	shippingAmount?: number;
	shippingWithTaxAmount?: number;
	subtotalAmount?: number;
	subtotalFormatted?: string;
	subtotalWithTaxAmountValue?: number;
	taxAmountFormatted?: string;
	taxAmountValue?: number;
	totalAmount?: number;
	totalFormatted: string;
	totalWithTaxAmountFormatted: string;
	transactionId: string;
};

export type OrderItem = {
	id?: number;
	name?: {
		en_US: string;
	};
	quantity?: number;
	skuId: number;
	unitPriceWithTaxAmount?: number;
};

export type OrderType = {
	externalReferenceCode: string;
	id: number;
	name: {[key: string]: string};
};

export type PaymentMethodSelector = 'order' | 'pay' | 'trial' | 'free';

export type PaymentStatusInfo = {
	code: number;
	label: string;
	label_i18n: string;
};

export type PlacedOrder = {
	account: string;
	accountId: number;
	author: string;
	createDate: string;
	customFields: {[key: string]: string};
	id: number;
	orderStatus: number | string;
	orderStatusInfo: {
		code: number;
		label: string;
		label_i18n: string;
	};
	orderType: String;
	orderTypeExternalReferenceCode: string;
	paymentStatus: number;
	placedOrderBillingAddress: BillingAddress;
	placedOrderBillingAddressId: number;
	placedOrderItems: PlacedOrderItems[];
	purchaseOrderNumber?: string;
	workflowStatusInfo: {
		code: number;
		label: string;
		label_i18n: string;
	};
};

export type PlacedOrderItems = {
	id: number;
	name: string;
	options: string;
	price: {
		price: number;
		priceFormatted: string;
	};
	productId: number;
	quantity: number;
	sku: string;
	skuId: number;
	subscription: boolean;
	thumbnail: string;
	version: string;
	virtualItemURLs: string;
	virtualItems: VirtualItem[];
};

export type VirtualItem = {
	productVersion?: String;
	url: string;
	usages: number;
	version: string;
};

export type OrderStatus =
	| 'approved'
	| 'cancelled'
	| 'completed'
	| 'in-progress'
	| 'on-hold'
	| 'pending'
	| 'processing';

export type OrderTypes =
	| 'ADDONS'
	| 'AI_HUB'
	| 'CLIENT_EXTENSION'
	| 'CLOUD_APP'
	| 'CMP_BETA'
	| 'COMPOSITE_APP'
	| 'DSR'
	| 'DXP'
	| 'DXP_APP'
	| 'LOW_CODE_CONFIGURATION'
	| 'OTHER'
	| 'SOLUTIONS30'
	| 'SOLUTIONS7'
	| 'SSA_SAAS';
