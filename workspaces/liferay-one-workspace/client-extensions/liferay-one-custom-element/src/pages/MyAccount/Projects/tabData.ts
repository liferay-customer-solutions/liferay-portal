/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Word} from '../../../i18n';
import {Application} from './applications';

export type ActivationKey = {
	badge?: Word;
	domain: string;
	expirationDate: string;
	id: string;
	name: string;
	startDate: string;
	status: string;
};

export type DownloadItem = {
	id: string;
	name: string;
};

export type Order = {
	date: string;
	id: string;
	orderId: string;
	status: string;
	total: string;
};

export type InvoiceType = 'credit-note' | 'invoice';

export type Invoice = {
	amount: string;
	date: string;
	id: string;
	invoiceId: string;
	status: string;
	type: InvoiceType;
};

export type HelpSupportLink = {
	href: string;
	label: Word;
	value: string;
};

export const STATUS_DOT_COLORS: {[key: string]: string} = {
	active: 'var(--color-success)',
	completed: 'var(--color-success)',
	expired: 'var(--color-danger)',
	paid: 'var(--color-success)',
	pending: 'var(--color-warning)',
	processing: 'var(--color-warning)',
};

export function getStatusColor(status: string): string {
	return STATUS_DOT_COLORS[status] ?? 'var(--color-neutral-6)';
}

export const ACTIVATION_KEYS: ActivationKey[] = [
	{
		badge: 'new-activation-key',
		domain: 'serenitygardens.io',
		expirationDate: 'Feb 20, 2029',
		id: 'foundation-key',
		name: 'Foundation Key',
		startDate: 'Feb 20, 2028',
		status: 'active',
	},
	{
		domain: 'bravekiwi.com',
		expirationDate: 'Jan 12, 2029',
		id: 'main-instance-key',
		name: 'Main Instance Key',
		startDate: 'Jan 12, 2028',
		status: 'active',
	},
	{
		badge: 'to-be-renewed',
		domain: 'phoenixtech.org',
		expirationDate: 'Feb 27, 2028',
		id: 'primary-key',
		name: 'Primary Key',
		startDate: 'Feb 27, 2027',
		status: 'active',
	},
	{
		domain: 'stellarluna.net',
		expirationDate: 'Jun 11, 2026',
		id: 'basic-tier-key',
		name: 'Basic Tier Key',
		startDate: 'Jun 11, 2025',
		status: 'expired',
	},
];

export const BUNDLES: DownloadItem[] = [
	{id: 'dxp-2026-q1-lts', name: 'DXP 2026.Q1 LTS'},
	{id: 'dxp-2026-q2-lts', name: 'DXP 2026.Q2 LTS'},
];

export const VERSIONS: DownloadItem[] = [
	{id: '2025-q4', name: '2025 Q4'},
	{id: '2026-q1', name: '2026 Q1'},
	{id: '2026-q2', name: '2026 Q2'},
];

export const ORDERS: Order[] = [
	{
		date: 'Jun 6, 2026',
		id: 'order-7841',
		orderId: 'ORD-7841',
		status: 'completed',
		total: '$4,350.00',
	},
	{
		date: 'May 19, 2026',
		id: 'order-7842',
		orderId: 'ORD-7842',
		status: 'completed',
		total: '$1,399.00',
	},
	{
		date: 'Apr 30, 2026',
		id: 'order-7843',
		orderId: 'ORD-7843',
		status: 'completed',
		total: '$322.50',
	},
	{
		date: 'Mar 14, 2026',
		id: 'order-7844',
		orderId: 'ORD-7844',
		status: 'completed',
		total: '$189.90',
	},
];

export const INVOICES: Invoice[] = [
	{
		amount: '$249.00',
		date: 'Jun 6, 2026',
		id: 'invoice-7841',
		invoiceId: 'INV-7841',
		status: 'paid',
		type: 'invoice',
	},
	{
		amount: '$249.00',
		date: 'May 24, 2026',
		id: 'invoice-7842',
		invoiceId: 'INV-7842',
		status: 'paid',
		type: 'credit-note',
	},
	{
		amount: '$249.00',
		date: 'Apr 30, 2026',
		id: 'invoice-7843',
		invoiceId: 'INV-7843',
		status: 'paid',
		type: 'invoice',
	},
	{
		amount: '$249.00',
		date: 'Mar 14, 2026',
		id: 'invoice-7844',
		invoiceId: 'INV-7844',
		status: 'paid',
		type: 'credit-note',
	},
];

function slugify(value: string): string {
	return value.toLowerCase().replace(/[^a-z0-9]/g, '');
}

export function getHelpSupportLinks(
	application: Application
): HelpSupportLink[] {
	const publisherSlug = slugify(application.publisher);
	const appSlug = slugify(application.name);

	return [
		{
			href: `https://${publisherSlug}support.com`,
			label: 'support-url',
			value: `${publisherSlug}support.com`,
		},
		{
			href: `https://${publisherSlug}.com`,
			label: 'publisher-website',
			value: `${publisherSlug}.com`,
		},
		{
			href: `mailto:support@${publisherSlug}.com`,
			label: 'support-email-address',
			value: `support@${publisherSlug}.com`,
		},
		{
			href: 'tel:+12678627172',
			label: 'support-phone-number',
			value: '+1 267 8627 172',
		},
		{
			href: `https://${publisherSlug}.eula.com`,
			label: 'app-usage-terms-eula-url',
			value: `${publisherSlug}.eula.com`,
		},
		{
			href: `https://${appSlug}documentation.com`,
			label: 'app-documentation-url',
			value: `${appSlug}documentation.com`,
		},
	];
}
