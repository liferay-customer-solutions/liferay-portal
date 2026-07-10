/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import i18n, {Word} from '~/i18n';

import {DetailsSection} from '../components/SectionedDetailsCard/SectionedDetailsCard';

import type {ProjectEnvironment} from '~/hooks/useProjectEnvironments';

const ENVIRONMENT_FIELDS: {key: keyof ProjectEnvironment; label: Word}[] = [
	{key: 'type', label: 'type'},
	{key: 'region', label: 'region'},
	{key: 'activationMode', label: 'activation-mode'},
	{key: 'status', label: 'status'},
	{key: 'hostName', label: 'host-name'},
	{key: 'domains', label: 'domains'},
	{key: 'currentEntitlementHash', label: 'identity'},
];

export function buildEnvironmentSections(
	environments: ProjectEnvironment[]
): DetailsSection[] {
	return environments.map((environment) => ({
		rows: ENVIRONMENT_FIELDS.filter((field) => environment[field.key]).map(
			(field) => ({
				label: i18n.translate(field.label),
				value: environment[field.key],
			})
		),
		title: environment.externalReferenceCode,
	}));
}

export default buildEnvironmentSections;
