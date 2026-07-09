/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

export type ActivationKeyStatus = 'activated' | 'expired' | 'not-activated';

export type ActivationKeyType = 'Cluster' | 'On-Premise' | 'Virtual Cluster';

export type ActivationKeyRow = {
	clusterNodes?: number;
	complimentary: boolean;
	description: string;
	environmentName: string;
	environmentType: string;
	expirationDate: string;
	hostName: string;
	id: string;
	keyType: ActivationKeyType;
	startDate: string;
	status: ActivationKeyStatus;
};

export const ACTIVATION_KEYS_MOCK: ActivationKeyRow[] = [
	{
		complimentary: true,
		description: 'Upgrade testing',
		environmentName: 'test',
		environmentType: 'DXP Non-Production',
		expirationDate: 'Apr 22, 2026',
		hostName: 'test',
		id: 'MOCK-DXP-01',
		keyType: 'On-Premise',
		startDate: 'Mar 23, 2026',
		status: 'expired',
	},
	{
		clusterNodes: 1,
		complimentary: true,
		description: 'test',
		environmentName: 'testNonProdVC',
		environmentType: 'DXP Non-Production',
		expirationDate: 'Sep 02, 2024',
		hostName: '',
		id: 'MOCK-DXP-02',
		keyType: 'Virtual Cluster',
		startDate: 'Aug 03, 2024',
		status: 'expired',
	},
	{
		clusterNodes: 3,
		complimentary: false,
		description: 'Primary production cluster',
		environmentName: 'prodCluster',
		environmentType: 'DXP Production',
		expirationDate: 'Jan 05, 2027',
		hostName: 'prod.acme.com',
		id: 'MOCK-DXP-03',
		keyType: 'Cluster',
		startDate: 'Jan 05, 2026',
		status: 'activated',
	},
	{
		complimentary: false,
		description: 'Staging environment',
		environmentName: 'staging',
		environmentType: 'DXP Non-Production',
		expirationDate: 'Jan 05, 2027',
		hostName: 'staging.acme.com',
		id: 'MOCK-DXP-04',
		keyType: 'On-Premise',
		startDate: 'Jan 05, 2026',
		status: 'activated',
	},
	{
		clusterNodes: 6,
		complimentary: false,
		description: 'Future disaster-recovery site',
		environmentName: 'drSite',
		environmentType: 'DXP Production',
		expirationDate: 'Dec 01, 2028',
		hostName: 'dr.acme.com',
		id: 'MOCK-DXP-05',
		keyType: 'Virtual Cluster',
		startDate: 'Dec 01, 2027',
		status: 'not-activated',
	},
];

export type ActivationStatusState = 'active' | 'in-progress' | 'not-activated';

export type ActivationStatusMock = {
	dateRange: string;
	status: ActivationStatusState;
};

export const ACTIVATION_STATUS_MOCK: ActivationStatusMock = {
	dateRange: 'Invalid Date - Invalid Date',
	status: 'not-activated',
};

export type CommerceInstructionLink = {
	label: string;
	url: string;
};

export type CommerceInstructionRow = {
	detail?: string;
	detailLink?: CommerceInstructionLink;
	instructions: string;
	version: string;
};

export const COMMERCE_INSTRUCTIONS_MOCK: CommerceInstructionRow[] = [
	{
		instructions: 'All Commerce modules are enabled by default.',
		version: 'DXP 7.4 GA1+',
	},
	{
		detail: 'More details: ',
		detailLink: {
			label: 'Activating Liferay Commerce',
			url: '',
		},
		instructions: 'Commerce is activated using a portal property.',
		version: 'DXP 7.3 FP3/SP2+',
	},
	{
		detail: 'To request a new or replacement activation key, please ',
		detailLink: {
			label: 'open a support ticket',
			url: '',
		},
		instructions: 'Commerce requires an activation key.',
		version: 'DXP 7.3 FP2/SP1',
	},
];

export type CloudNativeEnvironmentRow = {
	environment: string;
	maxClusterNodes: number;
	subscriptionId: string;
};

export type CloudNativeSubscription = {
	id: string;
	rows: CloudNativeEnvironmentRow[];
};

export const CLOUD_NATIVE_ENVIRONMENTS_MOCK: CloudNativeSubscription[] = [
	{
		id: 'jcnt1prodSubUuid',
		rows: [
			{
				environment: 'production',
				maxClusterNodes: 6,
				subscriptionId: 'jcnt1prodSubUuid',
			},
			{
				environment: 'uat',
				maxClusterNodes: 6,
				subscriptionId: 'jcnt1prodSubUuid',
			},
			{
				environment: 'non-production',
				maxClusterNodes: 1,
				subscriptionId: 'jcnt1nonProdSubUuid',
			},
		],
	},
	{
		id: 'jcnt1prodSubUuid2',
		rows: [
			{
				environment: 'production',
				maxClusterNodes: 11,
				subscriptionId: 'jcnt1prodSubUuid2',
			},
			{
				environment: 'uat',
				maxClusterNodes: 11,
				subscriptionId: 'jcnt1prodSubUuid2',
			},
			{
				environment: 'non-production',
				maxClusterNodes: 1,
				subscriptionId: 'jcnt1nonProdSubUuid2',
			},
		],
	},
];
