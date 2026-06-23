/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

export type Facets = {
	[key: string]: unknown;
};

export type ObjectActions = {
	[key: string]: {
		href: string;
		method: string;
	};
};

export type APIResponse<Query = unknown> = {
	actions: ObjectActions;
	facets: Facets[];
	items: Query[];
	lastPage: number;
	page: number;
	pageSize: number;
	results: Query[];
	totalCount: number;
};
