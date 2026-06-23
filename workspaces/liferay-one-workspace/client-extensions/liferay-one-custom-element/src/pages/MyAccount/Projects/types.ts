/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

export type ProjectItemKind = 'application' | 'product';

export type ProjectTabKey =
	| 'activation'
	| 'details'
	| 'download'
	| 'environment'
	| 'help-and-support'
	| 'orders'
	| 'utilization';

export type UserProject = {
	externalReferenceCode: string;
	id: number;
	name: string;
	unassigned?: boolean;
};
