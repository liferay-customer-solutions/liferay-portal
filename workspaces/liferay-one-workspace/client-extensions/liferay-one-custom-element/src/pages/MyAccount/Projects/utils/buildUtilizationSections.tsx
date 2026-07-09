/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import i18n from '~/i18n';

import {DetailsSection} from '../components/SectionedDetailsCard/SectionedDetailsCard';

import type {UtilizationProfile} from './resolveUtilizationProfile';

const TBD = 'TBD';

const UTILIZATION_SECTIONS_BY_PROFILE: Record<
	UtilizationProfile,
	() => DetailsSection[]
> = {
	'none': () => [],
	'paas-dashboard': () => [],
	'saas-dashboard': () => [],
	'usage-metrics': () => [
		{
			rows: [
				{label: i18n.translate('events-per-month'), value: TBD},
				{label: i18n.translate('api-requests-per-month'), value: TBD},
			],
		},
	],
};

export function buildUtilizationSections(
	profile: UtilizationProfile
): DetailsSection[] {
	return UTILIZATION_SECTIONS_BY_PROFILE[profile]();
}

export default buildUtilizationSections;
