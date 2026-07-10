/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Word} from '~/i18n';

import {PROJECT_TAB_KEYS} from '../types';

import type {OrderTypes} from '~/types/orders';

import type {ProjectTabKey} from '../types';

export const LAST_PROJECT_STORAGE_KEY = 'liferay-one:last-project';

export const PRODUCT_CATEGORY = {
	APP: 'app',
	LIFERAY_PRODUCT: 'liferay-product',
} as const;

export const PROJECT_TAB_LABELS: Record<ProjectTabKey, Word> = {
	'activation': 'activation',
	'details': 'details',
	'download': 'download',
	'environment': 'environment',
	'help-and-support': 'help-and-support',
	'orders': 'orders',
	'utilization': 'utilization',
};

export const PROJECT_TAB_ORDER: ProjectTabKey[] = [...PROJECT_TAB_KEYS];

export const STATUS_DOT_COLORS: {[key: string]: string} = {
	active: 'var(--color-state-success)',
	completed: 'var(--color-state-success)',
	expired: 'var(--color-state-error)',
	paid: 'var(--color-state-success)',
	pending: 'var(--color-state-warning)',
	processing: 'var(--color-state-warning)',
};

export type SupportLink = {
	href: (value: string) => string;
	label: Word;
	specificationKey: string;
};

function withProtocol(value: string): string {
	return /^https?:\/\//.test(value) ? value : `https://${value}`;
}

export const SUPPORT_LINKS: SupportLink[] = [
	{
		href: withProtocol,
		label: 'support-url',
		specificationKey: 'support-url',
	},
	{
		href: withProtocol,
		label: 'publisher-website',
		specificationKey: 'publisher-web-site-url',
	},
	{
		href: (value) => `mailto:${value}`,
		label: 'support-email-address',
		specificationKey: 'support-email-address',
	},
	{
		href: (value) => `tel:${value.replace(/\s/g, '')}`,
		label: 'support-phone-number',
		specificationKey: 'support-phone',
	},
	{
		href: withProtocol,
		label: 'app-usage-terms-eula-url',
		specificationKey: 'app-usage-terms-url',
	},
	{
		href: withProtocol,
		label: 'app-documentation-url',
		specificationKey: 'app-documentation-url',
	},
];

export const SUPPORT_SPECIFICATION_KEYS = SUPPORT_LINKS.map(
	(link) => link.specificationKey
);

export const TAB_VISIBILITY: Partial<Record<OrderTypes, ProjectTabKey[]>> = {
	ADDONS: ['details', 'orders'],
	AI_HUB: ['details', 'environment', 'orders'],
	CLIENT_EXTENSION: ['details', 'activation', 'orders', 'help-and-support'],
	CLOUD_APP: [
		'details',
		'activation',
		'environment',
		'orders',
		'help-and-support',
	],
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
