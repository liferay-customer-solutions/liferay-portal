/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {OrderCustomFields, OrderWorkflowStatusCode} from '~/utils/orderUtils';

import type {OrderTypes, PlacedOrder} from '~/types/orders';

type CustomFields = {
	[key in keyof typeof OrderCustomFields]: string;
};

export default class DeliveryOrderModel {
	constructor(private order: PlacedOrder) {}

	get canDownload() {
		return [
			'CLIENT_EXTENSION',
			'COMPOSITE_APP',
			'DXP_APP',
			'LOW_CODE_CONFIGURATION',
			'OTHER',
		].includes(this.order.orderTypeExternalReferenceCode as OrderTypes);
	}

	get createDate() {
		return this.order.createDate;
	}

	get canGenerateLicenses() {
		return (
			['CLIENT_EXTENSION', 'COMPOSITE_APP', 'DXP_APP'].includes(
				this.order.orderTypeExternalReferenceCode as OrderTypes
			) && !this.isFreeApp
		);
	}
	get customFields() {
		const customFields = {} as CustomFields;

		for (const key in OrderCustomFields) {
			const keyValue =
				OrderCustomFields[key as keyof typeof OrderCustomFields];

			(customFields as Record<string, unknown>)[key] =
				this.order?.customFields?.[keyValue];
		}

		return customFields;
	}

	get isCancelled() {
		return (
			this.order?.orderStatusInfo?.code ===
			OrderWorkflowStatusCode.CANCELLED
		);
	}

	get isFreeApp() {
		return this.order.placedOrderItems?.[0]?.price?.price === 0;
	}

	get isOrderCompleted() {
		return (
			this.order.orderStatusInfo?.code ===
			OrderWorkflowStatusCode.COMPLETED
		);
	}

	get placedOrderItems() {
		return this.order?.placedOrderItems ?? [];
	}

	get productThumbnail() {
		return this.placedOrderItems[0]?.thumbnail;
	}
}
