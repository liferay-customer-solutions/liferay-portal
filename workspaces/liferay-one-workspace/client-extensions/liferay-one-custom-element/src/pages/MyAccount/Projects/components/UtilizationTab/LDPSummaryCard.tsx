/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Tooltip} from '~/components/Tooltip/Tooltip';
import i18n, {Word} from '~/i18n';
import {
	formatUsageLimit,
	formatUsageUsed,
	isUnlimitedUsage,
} from '~/pages/MyAccount/Projects/utils/usageMetricDisplayUtils';

import './LDPSummaryCard.css';

import type {UsageMetric} from '~/pages/MyAccount/Projects/hooks/useProjectUsageDashboard';

type LDPSummaryCardProps = {
	label: Word;
	metric?: UsageMetric;
	tooltip: Word;
};

export default function LDPSummaryCard({
	label,
	metric,
	tooltip,
}: LDPSummaryCardProps) {
	const parsedPercentage = Number(metric?.percentage);

	const percentage = Number.isFinite(parsedPercentage)
		? Math.min(Math.max(parsedPercentage, 0), 100)
		: 0;

	return (
		<div className="ldp-summary-card">
			<div className="ldp-summary-card-header">
				<h3 className="ldp-summary-card-title">
					{i18n.translate(label)}
				</h3>

				<Tooltip
					symbol="info-circle-open"
					tooltip={i18n.translate(tooltip)}
				/>
			</div>

			<div className="ldp-summary-card-body">
				<span className="ldp-summary-card-used-count">
					{formatUsageUsed(metric)}
				</span>

				<span className="ldp-summary-card-max-count">
					{isUnlimitedUsage(metric)
						? i18n.translate('unlimited')
						: i18n.sub('of-x', formatUsageLimit(metric))}
				</span>

				<div className="ldp-summary-card-progress-bar">
					<div
						className="ldp-summary-card-progress-bar-fill"
						style={{
							width: isUnlimitedUsage(metric)
								? 0
								: `${percentage}%`,
						}}
					/>
				</div>
			</div>
		</div>
	);
}
