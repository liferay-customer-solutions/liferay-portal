/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import i18n from '~/i18n';
import {Liferay} from '~/services/liferay/liferay';

import type {UsageMetric} from '~/pages/MyAccount/Projects/hooks/useProjectUsageDashboard';

export const EM_DASH = '—';

export function formatCount(count: number, units?: string): string {
	const value = count.toLocaleString(
		Liferay.ThemeDisplay.getBCP47LanguageId()
	);

	return units ? `${value} ${units}` : value;
}

export function formatUsageLimit(metric?: UsageMetric): string {
	if (!metric) {
		return EM_DASH;
	}

	if (isUnlimitedUsage(metric)) {
		return i18n.translate('unlimited');
	}

	if (metric.maxCount > 0) {
		return formatCount(metric.maxCount, metric.maxCountUnits);
	}

	return EM_DASH;
}

export function formatUsageUsed(metric?: UsageMetric): string {
	if (metric?.usedCount === undefined) {
		return EM_DASH;
	}

	return formatCount(metric.usedCount, metric.usedCountUnits);
}

export function hasOverageUsage(metrics: {
	[key: string]: UsageMetric;
}): boolean {
	return Object.values(metrics).some(
		(metric) => Number(metric.percentage) > 100
	);
}

export function isUnlimitedUsage(metric?: UsageMetric): boolean {
	return (metric?.maxCount ?? 0) < 0;
}
