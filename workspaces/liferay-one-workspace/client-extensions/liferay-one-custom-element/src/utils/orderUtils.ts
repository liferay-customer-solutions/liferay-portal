/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {formatCurrency} from '~/utils/formatCurrency';

import type {Order, OrderTypes, PlacedOrder} from '~/types/orders';

type NumericKeys<T> = {
	[K in keyof T]: T[K] extends number | undefined ? K : never;
}[keyof T];

export function getTotalByOrderKey(
	field: NumericKeys<Order>,
	orders: Order[],
	multiplier = 1
) {
	if (!orders?.length) {
		return 0;
	}

	const total = orders.reduce((sum, order) => {
		const value = order[field as keyof Order];

		if (order.currencyCode !== 'USD' || typeof value !== 'number') {
			return sum;
		}

		return sum + value * multiplier;
	}, 0);

	return formatCurrency(total, 'USD');
}

export const OrderCustomFields = {
	CLOUD_PROJECT_NAME: 'cloudProjectName',
	KORONEIKI_PROJECT: 'koroneiki-project',
	PROJECT_NAME: 'projectName',
	TRIAL_END_DATE: 'trial-end-date',
	TRIAL_ERROR: 'trial-error',
	TRIAL_SETTINGS: 'trial-settings',
	TRIAL_START_DATE: 'trial-start-date',
	TRIAL_VIRTUAL_HOST: 'trial-virtual-host',
} as const;

export type OrderCustomFields =
	(typeof OrderCustomFields)[keyof typeof OrderCustomFields];

export const OrderWorkflowStatusCode = {
	CANCELLED: 8,
	COMPLETED: 0,
	IN_PROGRESS: 6,
	ON_HOLD: 20,
	PENDING: 1,
	PENDING_PAYMENT: 99,
	PROCESSING: 10,
};

export type OrderWorkflowStatusCode =
	(typeof OrderWorkflowStatusCode)[keyof typeof OrderWorkflowStatusCode];

export const PaymentStatus = {
	CANCELED: 8,
	FAILED: 4,
	PAID: 0,
	PAYMENT_PENDING: 2,
	PENDING: 1,
};

export type PaymentStatus = (typeof PaymentStatus)[keyof typeof PaymentStatus];

export const APP_ORDER_TYPES: readonly OrderTypes[] = [
	'CLIENT_EXTENSION',
	'CLOUD_APP',
	'COMPOSITE_APP',
	'DXP_APP',
	'LOW_CODE_CONFIGURATION',
	'OTHER',
];

export const LIFERAY_PRODUCT_ORDER_TYPES: readonly OrderTypes[] = [
	'ADDONS',
	'AI_HUB',
	'CMP_BETA',
	'DXP',
];

export const orderTypeLabel = {
	ADDONS: 'Add-Ons',
	AI_HUB: 'AI Hub',
	CLIENT_EXTENSION: 'Client Extension',
	CLOUD_APP: 'Cloud',
	CMP_BETA: 'Content Marketing Platform',
	COMPOSITE_APP: 'Composite App',
	DSR: 'Digital Sales Room',
	DXP: 'DXP Free',
	DXP_APP: 'DXP',
	LOW_CODE_CONFIGURATION: 'Low-Code Configuration',
	OTHER: 'Other',
	SOLUTIONS7: 'Solutions 7',
	SOLUTIONS30: 'Solutions 30',
	SSA_SAAS: 'SSA SaaS',
} as const;

export const orderWorkflowDisplayType = {
	[OrderWorkflowStatusCode.CANCELLED]: 'warning',
	[OrderWorkflowStatusCode.COMPLETED]: 'success',
	[OrderWorkflowStatusCode.IN_PROGRESS]: 'info',
	[OrderWorkflowStatusCode.ON_HOLD]: 'secondary',
	[OrderWorkflowStatusCode.PENDING]: 'warning',
	[OrderWorkflowStatusCode.PROCESSING]: 'secondary',
} as const;

export const orderWorkflowStatusCodeLabels = {
	[OrderWorkflowStatusCode.CANCELLED]: 'Cancelled',
	[OrderWorkflowStatusCode.COMPLETED]: 'Completed',
	[OrderWorkflowStatusCode.IN_PROGRESS]: 'In Progress',
	[OrderWorkflowStatusCode.ON_HOLD]: 'On Hold',
	[OrderWorkflowStatusCode.PENDING]: 'Pending',
	[OrderWorkflowStatusCode.PENDING_PAYMENT]: 'Pending Payment',
	[OrderWorkflowStatusCode.PROCESSING]: 'Processing',
} as const;

export const paymentStatusLabels = {
	[PaymentStatus.CANCELED]: 'canceled',
	[PaymentStatus.FAILED]: 'failed',
	[PaymentStatus.PAID]: 'paid',
	[PaymentStatus.PAYMENT_PENDING]: 'pending',
	[PaymentStatus.PENDING]: 'unpaid',
} as const;

export const paymentWorkflowDisplayType = {
	[PaymentStatus.PAID]: 'success',
	[PaymentStatus.PAYMENT_PENDING]: 'warning',
	[PaymentStatus.PENDING]: 'secondary',
} as const;

export function getOrderStatusLabel(order: PlacedOrder) {
	const statusLabel =
		order.orderStatusInfo?.label ||
		order.workflowStatusInfo?.label_i18n ||
		order.workflowStatusInfo?.label ||
		'';

	const expirableOrderTypes: OrderTypes[] = [
		'ADDONS',
		'CMP_BETA',
		'DSR',
		'DXP',
	];

	if (
		expirableOrderTypes.includes(
			order.orderTypeExternalReferenceCode as OrderTypes
		)
	) {
		return (
			{
				[OrderWorkflowStatusCode.CANCELLED]: 'Expired',
				[OrderWorkflowStatusCode.COMPLETED]: 'Active',
				[OrderWorkflowStatusCode.IN_PROGRESS]: 'Active',
				[OrderWorkflowStatusCode.ON_HOLD]: 'Pending',
				[OrderWorkflowStatusCode.PENDING]: 'Pending',
				[OrderWorkflowStatusCode.PROCESSING]: 'Pending',
			}[order.orderStatusInfo.code] || statusLabel
		);
	}

	if (order.orderTypeExternalReferenceCode === 'AI_HUB') {
		if (order.orderStatusInfo.code !== OrderWorkflowStatusCode.COMPLETED) {
			return 'Requested';
		}
	}

	return statusLabel;
}
