/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import useSWR, {SWRConfiguration} from 'swr';
import DeliveryOrderModel from '~/models/DeliveryOrderModel';
import {DeliveryProductModel} from '~/models/DeliveryProductModel';
import HeadlessCommerceDeliveryCatalog from '~/services/headless/HeadlessCommerceDeliveryCatalog';
import HeadlessCommerceDeliveryOrder from '~/services/headless/HeadlessCommerceDeliveryOrder';
import {Liferay} from '~/services/liferay/liferay';
import {
	getProductFallback,
	getProductImageFallback,
} from '~/utils/productUtils';

const useGetProductByOrderId = (
	orderId: string,
	swrOptions?: SWRConfiguration
) => {
	return useSWR(
		`/placed-order/${orderId}/product`,
		async () => {
			const placedOrder =
				await HeadlessCommerceDeliveryOrder.getPlacedOrder(orderId);

			if (placedOrder.placedOrderBillingAddressId > 0) {
				placedOrder.placedOrderBillingAddress =
					await HeadlessCommerceDeliveryOrder.getPlacedOrderBillingAddress(
						orderId
					);
			}

			let product;

			try {
				product = await HeadlessCommerceDeliveryCatalog.getProduct(
					Liferay.CommerceContext.commerceChannelId,
					placedOrder.placedOrderItems[0].productId,
					new URLSearchParams({
						'accountId': '-1',
						'attachments.accountId': '-1',
						'images.accountId': '-1',
						'nestedFields':
							'attachments,categories,images,productSpecifications',
						'skus.accountId': '-1',
					})
				);
			}
			catch (error) {
				console.error('Failed to fetch product:', error);

				product = getProductFallback();
				placedOrder.placedOrderItems[0].thumbnail =
					getProductImageFallback('productImage');
			}

			return {
				orderModel: new DeliveryOrderModel(placedOrder),
				placedOrder,
				product,
				productModel: new DeliveryProductModel(product),
			};
		},
		swrOptions
	);
};

export default useGetProductByOrderId;
