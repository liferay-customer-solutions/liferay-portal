/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

export type ProjectItemKind = 'application' | 'product';

export const PROJECT_TAB_KEYS = [
	'details',
	'utilization',
	'environment',
	'activation',
	'download',
	'orders',
	'help-and-support',
] as const;

export type ProjectTabKey = (typeof PROJECT_TAB_KEYS)[number];

export type UserProject = {
	externalReferenceCode: string;
	id: number;
	name: string;
	unassigned?: boolean;
};
