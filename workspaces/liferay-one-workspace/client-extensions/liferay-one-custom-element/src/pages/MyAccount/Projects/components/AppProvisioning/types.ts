/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

export const InstallStatus = {
	EXPIRED: 'expired',
	IN_PROGRESS: 'in-progress',
	INSTALLED: 'installed',
	READY_TO_INSTALL: 'ready-to-install',
} as const;

export type InstallStatus = (typeof InstallStatus)[keyof typeof InstallStatus];

export type Deployment = {
	appId: string;
	createdAt: number;
	id: string;
	loading?: boolean;
	orderId: number;
	projectId: string;
};

export type Provisioning = {
	deployments: Deployment[];
	orderItemId: number;
	quantity: number;
	shippedQuantity: number;
	sku: string;
};
