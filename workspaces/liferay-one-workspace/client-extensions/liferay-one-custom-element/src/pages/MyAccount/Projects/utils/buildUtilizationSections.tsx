/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {DetailsSection} from '../components/SectionedDetailsCard/SectionedDetailsCard';

import type {ProjectUsage} from '~/hooks/useProjectUsage';

function formatQuantity(value: number): string {
	return value.toLocaleString('en-US');
}

export function buildUtilizationSections(
	usage: ProjectUsage[]
): DetailsSection[] {
	if (!usage.length) {
		return [];
	}

	return [
		{
			rows: usage.map((row) => ({
				label: row.period ? `${row.unit} (${row.period})` : row.unit,
				value: `${formatQuantity(row.consumed)} / ${formatQuantity(
					row.included
				)}`,
			})),
		},
	];
}

export default buildUtilizationSections;
