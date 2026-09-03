/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {format} from 'date-fns';
import {useMemo} from 'react';
import {
	ProjectProduct,
	getSpecificationValue,
	getSpecificationValues,
	useChannelProducts,
} from '~/hooks/useProjectCommerce';
import {getProjectName, useProjectOrders} from '~/hooks/useProjectOrders';
import {isUnassignedProject} from '~/pages/MyAccount/Projects/utils/isUnassignedProject';
import {resolveProjectItemKind} from '~/pages/MyAccount/Projects/utils/resolveProjectItemKind';

export function useProjectApplications(
	projectId: string,
	projectName?: string
) {
	const {
		data: channelProducts,
		error,
		isLoading: productsLoading,
	} = useChannelProducts();
	const {loading: ordersLoading, placedOrders} =
		useProjectOrders(projectName);

	const scopedOrders = useMemo(
		() =>
			isUnassignedProject(projectId)
				? placedOrders.filter((order) => !getProjectName(order))
				: placedOrders,
		[placedOrders, projectId]
	);

	const applications = useMemo(() => {
		const productsByProductId = new Map(
			(channelProducts?.items ?? []).map((product) => [
				product.productId,
				product,
			])
		);

		const applicationsByExternalReferenceCode = new Map<
			string,
			ProjectProduct
		>();

		for (const order of scopedOrders) {
			for (const item of order.placedOrderItems ?? []) {
				const product = productsByProductId.get(item.productId);

				if (
					!product ||
					resolveProjectItemKind(
						product.productSpecifications ?? []
					) !== 'application' ||
					applicationsByExternalReferenceCode.has(
						product.externalReferenceCode
					)
				) {
					continue;
				}

				applicationsByExternalReferenceCode.set(
					product.externalReferenceCode,
					{
						description: product.description,
						endDate: '',
						externalReferenceCode: product.externalReferenceCode,
						id: String(product.productId ?? product.id),
						name: product.name,
						publisher: getSpecificationValue(
							product,
							'publisher-name'
						),
						saleType: getSpecificationValue(product, 'price-model'),
						specifications: product.productSpecifications ?? [],
						startDate: order.createDate
							? format(new Date(order.createDate), 'MMM d, yyyy')
							: '',
						status: 'active',
						type:
							getSpecificationValues(
								product,
								'liferay-products-categories'
							)[0] ??
							getSpecificationValue(product, 'price-model'),
					}
				);
			}
		}

		return [...applicationsByExternalReferenceCode.values()];
	}, [channelProducts, scopedOrders]);

	const orderIdByProductName = useMemo(() => {
		const map = new Map<string, string>();

		for (const order of scopedOrders) {
			for (const item of order.placedOrderItems ?? []) {
				if (!map.has(item.name)) {
					map.set(item.name, String(order.id));
				}
			}
		}

		return map;
	}, [scopedOrders]);

	return {
		applications,
		error,
		loading: productsLoading || ordersLoading,
		orderIdByProductName,
	};
}
