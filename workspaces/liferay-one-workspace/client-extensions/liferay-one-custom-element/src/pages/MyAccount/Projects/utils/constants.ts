/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Word} from '~/i18n';

import type {OrderTypes} from '~/types/orders';

import type {ProjectTabKey} from '../types';

export const LAST_PROJECT_STORAGE_KEY = 'liferay-one:last-project';

export const PROJECT_TAB_LABELS: Record<ProjectTabKey, Word> = {
	'activation': 'activation',
	'details': 'details',
	'download': 'download',
	'environment': 'environment',
	'help-and-support': 'help-and-support',
	'orders': 'orders',
	'utilization': 'utilization',
};

export const PROJECT_TAB_ORDER: ProjectTabKey[] = [
	'details',
	'activation',
	'download',
	'utilization',
	'environment',
	'orders',
	'help-and-support',
];

export const STATUS_DOT_COLORS: {[key: string]: string} = {
	active: 'var(--color-success)',
	completed: 'var(--color-success)',
	expired: 'var(--color-danger)',
	paid: 'var(--color-success)',
	pending: 'var(--color-warning)',
	processing: 'var(--color-warning)',
};

export const TAB_VISIBILITY: Partial<Record<OrderTypes, ProjectTabKey[]>> = {
	ADDONS: ['details', 'orders'],
	AI_HUB: ['details', 'activation', 'orders'],
	CLIENT_EXTENSION: ['details', 'activation', 'orders', 'help-and-support'],
	CLOUD_APP: ['details', 'environment', 'orders', 'help-and-support'],
	CMP_BETA: ['details', 'activation', 'download', 'orders'],
	COMPOSITE_APP: [
		'details',
		'activation',
		'download',
		'orders',
		'help-and-support',
	],
	DSR: ['details', 'activation', 'download', 'environment', 'orders'],
	DXP: ['details', 'activation', 'download', 'orders'],
	DXP_APP: [
		'details',
		'activation',
		'download',
		'orders',
		'help-and-support',
	],
	LOW_CODE_CONFIGURATION: [
		'details',
		'download',
		'orders',
		'help-and-support',
	],
	OTHER: ['details', 'orders', 'help-and-support'],
	SOLUTIONS7: ['details', 'orders'],
	SOLUTIONS30: ['details', 'orders'],
	SSA_SAAS: ['details', 'activation', 'utilization', 'environment', 'orders'],
};

export const UNASSIGNED_PROJECT_ERC = 'one-time-purchases';

export const ICON_BY_CATEGORY: {[key: string]: string} = {
	'Analytics': 'analytics',
	'Artificial Intelligence': 'magic',
	'Commerce': 'shopping-cart',
	'Customer Data': 'users',
	'Platform': 'globe',
};

export const LOGO_COLORS = [
	'#00b8a3',
	'#1b95e0',
	'#4b9fff',
	'#7b61ff',
	'#9b59b6',
	'#e9518a',
	'#f5b400',
	'#ff7847',
];
