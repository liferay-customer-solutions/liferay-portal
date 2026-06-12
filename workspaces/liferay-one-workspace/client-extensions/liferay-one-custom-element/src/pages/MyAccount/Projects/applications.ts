/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

export type Application = {
	customerAccount: string;
	id: string;
	logoColor: string;
	name: string;
	orderDate: string;
	orderId: string;
	projectType: string;
	providedBy: string;
	providedDate: string;
	publisher: string;
	purchaseNumber: string;
	saleType: string;
	status: string;
};

export const APPLICATIONS: Application[] = [
	{
		customerAccount: 'El Torero Spa',
		id: 'az-transport',
		logoColor: '#4b9fff',
		name: 'A&Z Transport',
		orderDate: 'Jun, 10 2026',
		orderId: '1971827',
		projectType: 'perpetual',
		providedBy: 'Mayven Hall',
		providedDate: 'Today',
		publisher: 'A&Z Logistics',
		purchaseNumber: '12972534',
		saleType: 'DXP',
		status: 'completed',
	},
	{
		customerAccount: 'El Torero Spa',
		id: 'data-generator-library',
		logoColor: '#7b61ff',
		name: 'Data Generator Library',
		orderDate: 'Feb, 16 2026',
		orderId: '1971827',
		projectType: 'perpetual',
		providedBy: 'Mayven Hall',
		providedDate: 'Feb 16, 2026',
		publisher: 'Bridgeworks',
		purchaseNumber: '12968110',
		saleType: 'DXP',
		status: 'completed',
	},
	{
		customerAccount: 'El Torero Spa',
		id: 'domos-deluxe',
		logoColor: '#ff7847',
		name: 'Domos Deluxe',
		orderDate: 'Feb, 16 2026',
		orderId: '1971827',
		projectType: 'perpetual',
		providedBy: 'Mayven Hall',
		providedDate: 'Feb 16, 2026',
		publisher: 'Osmos & Co',
		purchaseNumber: '12968111',
		saleType: 'DXP',
		status: 'completed',
	},
	{
		customerAccount: 'El Torero Spa',
		id: 'gilgamesh-edus',
		logoColor: '#00b8a3',
		name: 'Gilgamesh Edus',
		orderDate: 'Feb, 16 2026',
		orderId: '1971827',
		projectType: 'perpetual',
		providedBy: 'Mayven Hall',
		providedDate: 'Feb 16, 2026',
		publisher: 'Edus Group',
		purchaseNumber: '12968112',
		saleType: 'DXP',
		status: 'completed',
	},
	{
		customerAccount: 'El Torero Spa',
		id: 'object-sync-for-google',
		logoColor: '#f5b400',
		name: 'Object Sync for Google',
		orderDate: 'Jun, 10 2026',
		orderId: '1971827',
		projectType: 'perpetual',
		providedBy: 'Mayven Hall',
		providedDate: 'Today',
		publisher: 'Syncworks',
		purchaseNumber: '12972540',
		saleType: 'DXP',
		status: 'completed',
	},
	{
		customerAccount: 'El Torero Spa',
		id: 'search-security',
		logoColor: '#e9518a',
		name: 'Search Security',
		orderDate: 'Jun, 10 2026',
		orderId: '1971827',
		projectType: 'perpetual',
		providedBy: 'Mayven Hall',
		providedDate: 'Today',
		publisher: 'SecureSearch',
		purchaseNumber: '12972541',
		saleType: 'DXP',
		status: 'completed',
	},
];

export function getApplication(id: string): Application | undefined {
	return APPLICATIONS.find((application) => application.id === id);
}
