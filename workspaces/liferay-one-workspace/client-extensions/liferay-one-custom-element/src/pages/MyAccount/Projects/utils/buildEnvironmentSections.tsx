/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import i18n, {Word} from '~/i18n';

import {DetailsSection} from '../components/SectionedDetailsCard/SectionedDetailsCard';

import type {ProjectEnvironment} from '~/hooks/useProjectEnvironments';
import type {EnvironmentProfile} from './resolveEnvironmentProfile';

type EnvironmentField = {
	label: Word;
	value: (environment: ProjectEnvironment) => string;
};

const ENVIRONMENT_FIELDS_BY_PROFILE: Partial<
	Record<EnvironmentProfile, EnvironmentField[]>
> = {
	'analytics-cloud': [
		{
			label: 'owner-email',
			value: (environment) => environment.ownerEmailAddress,
		},
		{
			label: 'workspace-name',
			value: (environment) => environment.workspaceName,
		},
		{
			label: 'data-center-location',
			value: (environment) => environment.region,
		},
		{
			label: 'workspace-friendly-url',
			value: (environment) => environment.friendlyURL,
		},
		{
			label: 'allowed-email-domains',
			value: (environment) => environment.allowedEmailDomains,
		},
		{label: 'time-zone', value: (environment) => environment.timeZone},
	],
	'paas': [
		{label: 'project-id', value: (environment) => environment.projectId},
		{
			label: 'primary-data-center-region',
			value: (environment) => environment.region,
		},
		{
			label: 'system-admin-email',
			value: (environment) => environment.adminEmailAddress,
		},
		{
			label: 'system-admin-first-name',
			value: (environment) => environment.adminFirstName,
		},
		{
			label: 'system-admin-last-name',
			value: (environment) => environment.adminLastName,
		},
		{
			label: 'github-username',
			value: (environment) => environment.githubUsername,
		},
	],
	'saas': [
		{label: 'project-id', value: (environment) => environment.projectId},
		{label: 'primary-region', value: (environment) => environment.region},
		{
			label: 'project-admin-name',
			value: (environment) =>
				`${environment.adminFirstName} ${environment.adminLastName}`.trim(),
		},
		{
			label: 'project-admin-email',
			value: (environment) => environment.adminEmailAddress,
		},
	],
	'workspace': [
		{
			label: 'workspace-name',
			value: (environment) => environment.workspaceName,
		},
		{
			label: 'workspace-owner-email',
			value: (environment) => environment.ownerEmailAddress,
		},
		{
			label: 'data-center-location',
			value: (environment) => environment.region,
		},
		{label: 'time-zone', value: (environment) => environment.timeZone},
		{
			label: 'workspace-friendly-url',
			value: (environment) => environment.friendlyURL,
		},
		{
			label: 'allowed-email-domains',
			value: (environment) => environment.allowedEmailDomains,
		},
	],
};

export function buildEnvironmentSections(
	environments: ProjectEnvironment[],
	profile: EnvironmentProfile
): DetailsSection[] {
	const fields = ENVIRONMENT_FIELDS_BY_PROFILE[profile] ?? [];

	return environments.map((environment) => ({
		rows: fields
			.map((field) => ({
				label: i18n.translate(field.label),
				value: field.value(environment),
			}))
			.filter((row) => row.value),
		title: environment.externalReferenceCode,
	}));
}

export default buildEnvironmentSections;
