/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

export type MetricPeriod = 'month' | 'q1' | 'q2' | 'q3' | 'q4' | 'week';

export const METRIC_PARAMETER: Record<MetricPeriod, number> = {
	month: 30,
	q1: 1,
	q2: 2,
	q3: 3,
	q4: 4,
	week: 7,
};

export function percentage(total: number, partial: number): number {
	if (!total) {
		return 0;
	}

	return Math.round((partial / total) * 100);
}
