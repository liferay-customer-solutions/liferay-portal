/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import i18n, {Word} from '~/i18n';
import {
	formatUsageLimit,
	formatUsageUsed,
	isUnlimitedUsage,
} from '~/pages/MyAccount/Projects/utils/usageMetricDisplayUtils';

import type {UsageMetric} from '~/pages/MyAccount/Projects/hooks/useProjectUsageDashboard';

type UsageProgressBarProps = {
	label: Word;
	metric?: UsageMetric;
};

export default function UsageProgressBar({
	label,
	metric,
}: UsageProgressBarProps) {
	const percentage = Math.min(Number(metric?.percentage ?? 0), 100);

	return (
		<div className="d-flex flex-column">
			<span className="font-weight-bold">{i18n.translate(label)}</span>

			<div className="align-items-baseline d-flex flex-row my-2">
				<span className="font-weight-bold text-title">
					{formatUsageUsed(metric)}
				</span>

				<span className="mx-1 text-neutral-7">/</span>

				<span className="text-neutral-7">
					{formatUsageLimit(metric)}
				</span>
			</div>

			<div className="usage-metric-progress-bar">
				<div
					className="usage-metric-progress-bar-fill"
					style={{
						width: isUnlimitedUsage(metric) ? 0 : `${percentage}%`,
					}}
				/>
			</div>
		</div>
	);
}
