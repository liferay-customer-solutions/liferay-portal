/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Cell, Pie, PieChart, ResponsiveContainer} from 'recharts';
import i18n, {Word} from '~/i18n';
import {
	formatUsageLimit,
	formatUsageUsed,
} from '~/pages/MyAccount/Projects/utils/usageMetricDisplayUtils';

import type {UsageMetric} from '~/pages/MyAccount/Projects/hooks/useProjectUsageDashboard';

const COLORS = ['var(--color-primary)', 'transparent'];

type UsageDonutProps = {
	label: Word;
	metric?: UsageMetric;
	totalLabel: Word;
};

export default function UsageDonut({
	label,
	metric,
	totalLabel,
}: UsageDonutProps) {
	const percentage = Math.min(Number(metric?.percentage ?? 0), 100);

	const data = [
		{name: 'used', value: percentage},
		{name: 'remainder', value: 100 - percentage},
	];

	return (
		<div className="align-items-center d-flex flex-row justify-content-between">
			<div className="usage-metric-donut">
				<ResponsiveContainer>
					<PieChart tabIndex={-1}>
						<Pie
							data={data}
							dataKey="value"
							endAngle={-270}
							innerRadius="70%"
							outerRadius="100%"
							startAngle={90}
						>
							{data.map((_, index) => (
								<Cell fill={COLORS[index]} key={index} />
							))}
						</Pie>

						<text
							className="usage-metric-donut-legend"
							dominantBaseline="middle"
							textAnchor="middle"
							x="50%"
							y="50%"
						>
							{formatUsageUsed(metric)}
						</text>
					</PieChart>
				</ResponsiveContainer>
			</div>

			<div className="d-flex flex-column ml-3">
				<span className="font-weight-bold">
					{i18n.translate(label)}
				</span>

				<span className="text-neutral-7 text-small">
					{i18n.translate(totalLabel)}
				</span>

				<span className="font-weight-bold">
					{formatUsageLimit(metric)}
				</span>
			</div>
		</div>
	);
}
