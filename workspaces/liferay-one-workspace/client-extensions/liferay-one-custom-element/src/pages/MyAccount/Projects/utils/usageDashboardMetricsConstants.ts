/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import type {Word} from '~/i18n';

export type UsageMetricConfig = {
	label: Word;
	metric: string;
	totalLabel: Word;
};

export const EXPERIENCE_RESOURCE_METRICS: UsageMetricConfig[] = [
	{
		label: 'extensions-ram',
		metric: 'clientExtensionsRAM',
		totalLabel: 'total-ram',
	},
	{
		label: 'extensions-vcpu',
		metric: 'clientExtensionsCPU',
		totalLabel: 'total-vcpu',
	},
	{
		label: 'storage',
		metric: 'documentLibraryAndBackupStorage',
		totalLabel: 'total-storage',
	},
	{
		label: 'database',
		metric: 'databaseStorage',
		totalLabel: 'total-storage',
	},
	{
		label: 'traffic-networking',
		metric: 'networkTraffic',
		totalLabel: 'monthly-inbound-and-outbound',
	},
	{
		label: 'logs',
		metric: 'logStorage',
		totalLabel: 'total-volume',
	},
];

export const SAAS_RESOURCE_METRICS: UsageMetricConfig[] = [
	{
		label: 'extensions-ram',
		metric: 'clientExtensionsCapacityRAM',
		totalLabel: 'total-ram',
	},
	{
		label: 'extensions-vcpu',
		metric: 'clientExtensionsCapacityCPU',
		totalLabel: 'total-vcpu',
	},
	{
		label: 'storage',
		metric: 'storageCapacityDocumentLibrary',
		totalLabel: 'total-storage',
	},
];

export const SAAS_SITE_AND_USER_METRICS: UsageMetricConfig[] = [
	{
		label: 'number-of-sites',
		metric: 'sites',
		totalLabel: 'number-of-sites',
	},
	{
		label: 'authenticated-logins-malus',
		metric: 'monthlyActiveLoggedInUsers',
		totalLabel: 'authenticated-logins-malus',
	},
	{
		label: 'anonymous-page-views-apv',
		metric: 'anonymousPageViews',
		totalLabel: 'anonymous-page-views-apv',
	},
];
