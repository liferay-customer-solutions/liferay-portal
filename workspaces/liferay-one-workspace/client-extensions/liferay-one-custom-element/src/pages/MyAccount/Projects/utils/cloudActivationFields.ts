/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Word} from '~/i18n';
import getKebabCase from '~/utils/getKebabCase';

import type {ProjectEnvironment} from '~/hooks/useProjectEnvironments';

export type CloudActivationOption = {
	label: string;
	value: string;
};

export type CloudActivationField = {
	environmentField: string;
	helpMessage?: Word;
	kind: 'admins' | 'email' | 'select' | 'text';
	label: Word;
	options?: CloudActivationOption[];
	placeholder?: string;
	required: boolean;
};

export type CloudActivationAdminFieldName =
	| 'emailAddress'
	| 'firstName'
	| 'githubUsername'
	| 'lastName'
	| 'name';

export type CloudActivationAdminField = {
	kind: 'email' | 'text';
	label: Word;
	name: CloudActivationAdminFieldName;
	required: boolean;
};

export type CloudActivationProfile = 'analytics-cloud' | 'paas' | 'saas';

const ANALYTICS_CLOUD_REGION_NAMES = [
	'Frankfurt, Germany',
	'London, England',
	'Mumbai, India',
	'Oregon, USA',
	'São Paulo, Brazil',
];

const DATA_CENTER_REGION_NAMES = [
	'Dammam, Saudi Arabia',
	'Delhi, India',
	'Doha, Qatar',
	'Frankfurt, Germany',
	'Hamina, Finland',
	'Iowa, USA',
	'London, England',
	'Montreal, Canada',
	'Mumbai, India',
	'Oregon, USA',
	'Osaka, Japan',
	'São Paulo, Brazil',
	'Sydney, Australia',
	'Tokyo, Japan',
	'Zurich, Switzerland',
];

const ANALYTICS_CLOUD_REGION_OPTIONS: CloudActivationOption[] =
	ANALYTICS_CLOUD_REGION_NAMES.map((name) => ({
		label: getKebabCase(name) as string,
		value: name,
	}));

const PAAS_REGION_OPTIONS: CloudActivationOption[] =
	DATA_CENTER_REGION_NAMES.map((name) => {
		const value = getKebabCase(name) as string;

		return {label: value, value};
	});

const CLOUD_ACTIVATION_FIELDS_BY_PROFILE: Record<
	CloudActivationProfile,
	CloudActivationField[]
> = {
	'analytics-cloud': [
		{
			environmentField: 'ownerEmailAddress',
			kind: 'email',
			label: 'owner-email',
			required: true,
		},
		{
			environmentField: 'workspaceName',
			kind: 'text',
			label: 'workspace-name',
			required: true,
		},
		{
			environmentField: 'region',
			kind: 'select',
			label: 'data-center-location',
			options: ANALYTICS_CLOUD_REGION_OPTIONS,
			required: true,
		},
		{
			environmentField: 'disasterRecoveryRegion',
			kind: 'select',
			label: 'disaster-recovery-data-center-location',
			options: ANALYTICS_CLOUD_REGION_OPTIONS,
			required: true,
		},
		{
			environmentField: 'friendlyURL',
			kind: 'text',
			label: 'workspace-friendly-url',
			required: false,
		},
		{
			environmentField: 'allowedEmailDomains',
			helpMessage:
				'anyone-with-an-email-address-at-the-provided-domains-can-request-access-to-your-workspace-if-multiple-separate-domains-by-commas',
			kind: 'text',
			label: 'allowed-email-domains',
			placeholder: '@mycompany.com',
			required: false,
		},
		{
			environmentField: 'timeZone',
			kind: 'text',
			label: 'time-zone',
			required: false,
		},
	],
	'paas': [
		{
			environmentField: 'projectId',
			kind: 'text',
			label: 'project-id',
			required: true,
		},
		{
			environmentField: 'dxpVersion',
			kind: 'select',
			label: 'liferay-dxp-version',
			required: true,
		},
		{
			environmentField: 'region',
			kind: 'select',
			label: 'primary-data-center-region',
			options: PAAS_REGION_OPTIONS,
			required: true,
		},
		{
			environmentField: 'disasterRecoveryRegion',
			kind: 'select',
			label: 'disaster-recovery-data-center-region',
			options: PAAS_REGION_OPTIONS,
			required: true,
		},
		{
			environmentField: 'admins',
			kind: 'admins',
			label: 'system-admins',
			required: true,
		},
	],
	'saas': [
		{
			environmentField: 'projectId',
			kind: 'text',
			label: 'project-id',
			required: true,
		},
		{
			environmentField: 'region',
			kind: 'select',
			label: 'primary-region',
			required: true,
		},
		{
			environmentField: 'admins',
			kind: 'admins',
			label: 'project-admins',
			required: true,
		},
		{
			environmentField: 'analyticsCloudOwnerEmailAddress',
			kind: 'text',
			label: 'analytics-cloud-owner-s-email-address',
			required: true,
		},
	],
};

const CLOUD_ACTIVATION_ADMIN_FIELDS_BY_PROFILE: Partial<
	Record<CloudActivationProfile, CloudActivationAdminField[]>
> = {
	paas: [
		{
			kind: 'email',
			label: 'system-admin-email',
			name: 'emailAddress',
			required: true,
		},
		{
			kind: 'text',
			label: 'system-admin-first-name',
			name: 'firstName',
			required: true,
		},
		{
			kind: 'text',
			label: 'system-admin-last-name',
			name: 'lastName',
			required: true,
		},
		{
			kind: 'text',
			label: 'github-username',
			name: 'githubUsername',
			required: true,
		},
	],
	saas: [
		{
			kind: 'text',
			label: 'project-admin-name',
			name: 'name',
			required: true,
		},
		{
			kind: 'email',
			label: 'project-admin-email',
			name: 'emailAddress',
			required: true,
		},
	],
};

export function getCloudActivationAdminFields(
	profile: CloudActivationProfile
): CloudActivationAdminField[] {
	return CLOUD_ACTIVATION_ADMIN_FIELDS_BY_PROFILE[profile] ?? [];
}

export function getCloudActivationFields(
	profile: CloudActivationProfile
): CloudActivationField[] {
	return CLOUD_ACTIVATION_FIELDS_BY_PROFILE[profile];
}

export function getCloudConsoleURL(
	profile: CloudActivationProfile,
	environment: ProjectEnvironment
): string {
	if (profile === 'paas') {
		return 'https://console.liferay.cloud';
	}

	if (profile === 'saas') {
		if (environment.hostName) {
			return `https://${environment.hostName}`;
		}

		if (environment.projectId) {
			return `https://${environment.projectId}.liferay.net`;
		}

		return '';
	}

	return 'https://analytics.liferay.com';
}

export default getCloudActivationFields;
