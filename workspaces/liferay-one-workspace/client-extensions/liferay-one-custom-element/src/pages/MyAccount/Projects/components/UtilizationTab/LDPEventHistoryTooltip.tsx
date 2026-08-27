/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import classNames from 'classnames';
import {formatCount} from '~/pages/MyAccount/Projects/utils/usageMetricDisplayUtils';

const SINGLE_COLUMN_LIMIT = 4;

type TooltipPayloadEntry = {
	color?: string;
	dataKey?: string;
	name?: string;
	value?: number;
};

type LDPEventHistoryTooltipProps = {
	active?: boolean;
	formatTitle?: (label: string) => string;
	label?: string;
	payload?: TooltipPayloadEntry[];
};

export default function LDPEventHistoryTooltip({
	active,
	formatTitle,
	label,
	payload = [],
}: LDPEventHistoryTooltipProps) {
	if (!active || !payload.length) {
		return null;
	}

	return (
		<div className="ldp-event-history-tooltip">
			<p className="ldp-event-history-tooltip-title">
				{formatTitle && label ? formatTitle(label) : label}
			</p>

			<ul
				className={classNames('ldp-event-history-tooltip-list', {
					'ldp-event-history-tooltip-list-split':
						payload.length > SINGLE_COLUMN_LIMIT,
				})}
			>
				{payload.map((entry) => (
					<li
						className="ldp-event-history-tooltip-row"
						key={entry.dataKey}
					>
						<span
							className="ldp-event-history-tooltip-dot"
							style={{backgroundColor: entry.color}}
						/>

						<span className="ldp-event-history-tooltip-name">
							{entry.name}
						</span>

						<span className="ldp-event-history-tooltip-count">
							{formatCount(entry.value ?? 0)}
						</span>
					</li>
				))}
			</ul>
		</div>
	);
}
