/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {addYears, format} from 'date-fns';
import {useMemo} from 'react';
import {
	LicenseType,
	ProductLicenseType,
	ProductSpecificationKey,
} from '~/enums/Product';
import useGetProductByOrderId from '~/hooks/useGetProductByOrderId';
import useGetResourceInfo from '~/hooks/useGetResourceInfo';
import i18n from '~/i18n';
import {parseProjectId} from '~/utils/parseProjectId';
import {getProductSpecification} from '~/utils/productUtils';

import {
	getDeploymentsByOrderItemId,
	hasDeploymentInProgress,
} from '../provisioning';
import {InstallStatus} from '../types';

import type {PlacedOrder} from '~/types/orders';
import type {DeliveryProduct} from '~/types/product';

import type {Deployment} from '../types';

export type ProvisioningData = ReturnType<typeof useProvisioningData>;

export type ProvisioningRow = ProvisioningData['provisioningTableData'][0];

const ACTIVE_REFRESH_INTERVAL = 60 * 1000;
const DEFAULT_REFRESH_INTERVAL = 240 * 1000;

const getExpirationDate = (createdDate: Date, licenseType: string) => {
	if (licenseType === ProductLicenseType.PERPETUAL) {
		return i18n.translate('does-not-expire');
	}

	return format(addYears(createdDate, 1), 'MMM dd, yyyy');
};

const getStatus = (
	deployment: Deployment | undefined,
	licenseType: string,
	order: PlacedOrder
) => {
	if (deployment?.loading) {
		return InstallStatus.IN_PROGRESS;
	}

	if (
		licenseType.toLowerCase() === LicenseType.SUBSCRIPTION &&
		new Date() > addYears(new Date(order.createDate), 1)
	) {
		return InstallStatus.EXPIRED;
	}

	return deployment
		? InstallStatus.INSTALLED
		: InstallStatus.READY_TO_INSTALL;
};

const useProvisioningData = (orderId: string) => {
	const {data, mutate: mutateOrder} = useGetProductByOrderId(orderId, {
		refreshInterval: (latestData?: {placedOrder?: PlacedOrder}) =>
			hasDeploymentInProgress(latestData?.placedOrder)
				? ACTIVE_REFRESH_INTERVAL
				: DEFAULT_REFRESH_INTERVAL,
	});

	const order = useMemo(
		() => data?.placedOrder ?? ({} as PlacedOrder),
		[data?.placedOrder]
	);

	const orderItems = useMemo(() => order.placedOrderItems ?? [], [order]);

	const product = data?.product;

	const resourceRequirements = useGetResourceInfo();

	const productLicenseType = useMemo(
		() =>
			getProductSpecification(
				ProductSpecificationKey.APP_LICENSING_TYPE,
				product as DeliveryProduct
			)?.value || '',
		[product]
	);

	const provisioningTableData = useMemo(() => {
		const items = [];

		const deploymentsByOrderItemId = getDeploymentsByOrderItemId(order);

		for (const orderItem of orderItems) {
			const deployments =
				deploymentsByOrderItemId.get(orderItem.id) ?? [];

			for (let i = 0; i < orderItem.quantity; i++) {
				const deployment = deployments[i];

				const {environment, projectName} = parseProjectId(
					deployment?.projectId
				);

				items.push({
					environment: environment.toUpperCase(),
					expirationDate: getExpirationDate(
						new Date(order.createDate),
						productLicenseType
					),
					id: deployment?.id ?? i,
					loading: deployment?.loading,
					orderItemId: orderItem.id,
					project: projectName.toUpperCase(),
					projectId: deployment?.projectId ?? '',
					startDate: format(
						new Date(order.createDate),
						'MMM dd, yyyy'
					),
					status: getStatus(deployment, productLicenseType, order),
					type: productLicenseType,
				});
			}
		}

		return items;
	}, [order, orderItems, productLicenseType]);

	return {
		mutateOrder,
		order,
		provisioningTableData,
		resourceRequirements,
	};
};

export default useProvisioningData;
