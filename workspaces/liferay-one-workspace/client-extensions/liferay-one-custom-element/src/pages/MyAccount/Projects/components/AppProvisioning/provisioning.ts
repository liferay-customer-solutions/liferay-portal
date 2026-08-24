/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {OrderCustomFields} from '~/utils/orderUtils';
import {safeJSONParse} from '~/utils/safeJSONParse';

import type {PlacedOrder} from '~/types/orders';

import type {Deployment, Provisioning} from './types';

export function getCloudProvisioning(order?: PlacedOrder): Provisioning[] {
	const value =
		(order?.customFields ?? {})[OrderCustomFields.CLOUD_PROVISIONING] ??
		null;

	const cloudProvisioning = safeJSONParse<Provisioning[]>(value, []);

	return Array.isArray(cloudProvisioning) ? cloudProvisioning : [];
}

export function getDeploymentsByOrderItemId(
	order?: PlacedOrder
): Map<number, Deployment[]> {
	return new Map(
		getCloudProvisioning(order).map((provisioning) => [
			provisioning.orderItemId,
			provisioning.deployments ?? [],
		])
	);
}

export function hasDeploymentInProgress(order?: PlacedOrder) {
	return getCloudProvisioning(order).some((provisioning) =>
		provisioning.deployments?.some((deployment) => deployment.loading)
	);
}
