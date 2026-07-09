/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import i18n from '~/i18n';

import {DetailsSection} from '../components/SectionedDetailsCard/SectionedDetailsCard';

import type {ProductEnvironmentInfo} from '~/hooks/useProjectOrders';

import type {EnvironmentProfile} from './resolveEnvironmentProfile';

const TBD = 'TBD';

const ENVIRONMENT_SECTIONS_BY_PROFILE: Record<
	EnvironmentProfile,
	(environment: ProductEnvironmentInfo) => DetailsSection[]
> = {
	'ac-token': () => [
		{rows: [{label: i18n.translate('access-token'), value: TBD}]},
	],
	'none': () => [],
	'paas': () => [
		{
			rows: [
				{label: i18n.translate('project-id'), value: TBD},
				{
					label: i18n.translate('primary-data-center-region'),
					value: TBD,
				},
				{label: i18n.translate('system-admin-email'), value: TBD},
				{label: i18n.translate('system-admin-first-name'), value: TBD},
				{label: i18n.translate('system-admin-last-name'), value: TBD},
				{label: i18n.translate('github-username'), value: TBD},
			],
		},
	],
	'saas': () => [
		{
			rows: [
				{label: i18n.translate('project-id'), value: TBD},
				{label: i18n.translate('primary-region'), value: TBD},
				{label: i18n.translate('project-admin-name'), value: TBD},
				{label: i18n.translate('project-admin-email'), value: TBD},
			],
		},
	],
	'workspace': (environment) => [
		{
			rows: [
				{
					label: i18n.translate('workspace-name'),
					value:
						environment.cloudProjectName ||
						environment.projectName ||
						TBD,
				},
				{label: i18n.translate('workspace-owner-email'), value: TBD},
				{label: i18n.translate('data-center-location'), value: TBD},
				{label: i18n.translate('time-zone'), value: TBD},
				{label: i18n.translate('workspace-friendly-url'), value: TBD},
				{label: i18n.translate('allowed-email-domains'), value: TBD},
			],
		},
	],
};

export function buildEnvironmentSections(
	profile: EnvironmentProfile,
	environment: ProductEnvironmentInfo
): DetailsSection[] {
	return ENVIRONMENT_SECTIONS_BY_PROFILE[profile](environment);
}

export default buildEnvironmentSections;
