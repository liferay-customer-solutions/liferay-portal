/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Word} from '../../../i18n';

export type ProductType = 'add-on' | 'primary';

export type Product = {
	badge?: Word;
	customerAccount: string;
	description: string;
	icon: string;
	id: string;
	logoColor: string;
	name: string;
	orderDate: string;
	orderId: string;
	publisher: string;
	purchaseNumber: string;
	purchasedBy: string;
	startDate: string;
	status: string;
	type: ProductType;
};

export const PRODUCTS: Product[] = [
	{
		customerAccount: 'El Torero Spa',
		description:
			'Manage your service by downloading software bundles, retrieving specific activation keys, and renewing your free plan directly when it nears expiration at no additional cost.',
		icon: 'globe',
		id: 'liferay-dxp-free-tier',
		logoColor: 'var(--color-brand-primary)',
		name: 'Liferay DXP - Free Tier',
		orderDate: 'Jun, 10 2026',
		orderId: '1971927',
		publisher: 'Liferay',
		purchaseNumber: '12972534',
		purchasedBy: 'Mauren Hall',
		startDate: 'June 14, 2026',
		status: 'active',
		type: 'primary',
	},
	{
		badge: 'beta',
		customerAccount: 'El Torero Spa',
		description:
			'Plan, create, and publish on-brand content across every channel with AI-assisted authoring, editorial workflows, and campaign analytics.',
		icon: 'documents-and-media',
		id: 'content-marketing-platform',
		logoColor: 'var(--color-info)',
		name: 'Content Marketing Platform',
		orderDate: 'Jun, 10 2026',
		orderId: '1971927',
		publisher: 'Liferay',
		purchaseNumber: '12972535',
		purchasedBy: 'Mauren Hall',
		startDate: 'June 14, 2026',
		status: 'active',
		type: 'primary',
	},
	{
		badge: 'beta',
		customerAccount: 'El Torero Spa',
		description:
			'Give sales teams a personalized digital space to share content, collaborate with buyers, and move deals forward.',
		icon: 'shopping-cart',
		id: 'digital-sales-room',
		logoColor: 'var(--color-success)',
		name: 'Digital Sales Room',
		orderDate: 'Jun, 10 2026',
		orderId: '1971927',
		publisher: 'Liferay',
		purchaseNumber: '12972536',
		purchasedBy: 'Mauren Hall',
		startDate: 'June 14, 2026',
		status: 'active',
		type: 'primary',
	},
	{
		badge: 'beta',
		customerAccount: 'El Torero Spa',
		description:
			'Unify customer data across systems to power segmentation, personalization, and reporting from a single trusted source.',
		icon: 'archive',
		id: 'liferay-data-platform',
		logoColor: 'var(--color-warning)',
		name: 'Liferay Data Platform',
		orderDate: 'Jun, 10 2026',
		orderId: '1971927',
		publisher: 'Liferay',
		purchaseNumber: '12972537',
		purchasedBy: 'Mauren Hall',
		startDate: 'June 14, 2026',
		status: 'active',
		type: 'primary',
	},
	{
		customerAccount: 'El Torero Spa',
		description:
			'Resell Liferay solutions to your own customers and manage their entitlements under your partner account.',
		icon: 'grid',
		id: 'partner-reseller',
		logoColor: 'var(--color-brand-secondary)',
		name: 'Partner Reseller',
		orderDate: 'Jun, 10 2026',
		orderId: '1971927',
		publisher: 'Liferay',
		purchaseNumber: '12972538',
		purchasedBy: 'Mauren Hall',
		startDate: 'June 14, 2026',
		status: 'active',
		type: 'add-on',
	},
	{
		customerAccount: 'El Torero Spa',
		description:
			'Increase your plan capacity with entitlements for up to 1,000,000 annual page views.',
		icon: 'password-policies',
		id: 'entitlements-up-to-one-million-apvs',
		logoColor: 'var(--color-danger)',
		name: 'Entitlements for up to 1,000,000 APVs',
		orderDate: 'Jun, 10 2026',
		orderId: '1971927',
		publisher: 'Liferay',
		purchaseNumber: '12972539',
		purchasedBy: 'Mauren Hall',
		startDate: 'June 14, 2026',
		status: 'active',
		type: 'add-on',
	},
];

export function getProduct(id: string): Product | undefined {
	return PRODUCTS.find((product) => product.id === id);
}
