/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import classNames from 'classnames';
import {useMemo, useState} from 'react';
import {Tooltip} from '~/components/Tooltip/Tooltip';
import {useProject} from '~/context/ProjectContext';
import {useProjectCommerce} from '~/hooks/useProjectCommerce';
import i18n from '~/i18n';
import {useProjectEventUsage} from '~/pages/MyAccount/Projects/hooks/useProjectEventUsage';
import {DATA_SOURCE_COLORS} from '~/pages/MyAccount/Projects/utils/constants';
import {formatCount} from '~/pages/MyAccount/Projects/utils/usageMetricDisplayUtils';
import {Liferay} from '~/services/liferay/liferay';
import {
	getTodaySlashDate,
	getUTCDayBounds,
	getUTCMonthBounds,
} from '~/utils/dateUtils';

import LDPEventsDayPicker from './LDPEventsDayPicker';
import LDPEventsMonthPicker from './LDPEventsMonthPicker';
import UsageUnavailableCard from './UsageUnavailableCard';

import './LDPEventsWidget.css';

import type {
	EventDataSource,
	ProjectEventUsage,
} from '~/pages/MyAccount/Projects/hooks/useProjectEventUsage';

type Granularity = 'day' | 'month';

type LDPEventsWidgetProps = {
	projectExternalReferenceCode: string;
};

const BREAKDOWN_SINGLE_COLUMN_LIMIT = 4;

const MILLISECONDS_PER_DAY = 24 * 60 * 60 * 1000;

function formatPercentage(percentage: number): string {
	return `${percentage.toLocaleString(
		Liferay.ThemeDisplay.getBCP47LanguageId(),
		{maximumFractionDigits: 1, minimumFractionDigits: 1}
	)} %`;
}

function getAddOnMessage(eventUsage?: ProjectEventUsage): string | undefined {
	const {addOnBucketCount, baseAllotment, maxCount} = eventUsage ?? {};

	if (
		!addOnBucketCount ||
		baseAllotment === undefined ||
		maxCount === undefined ||
		maxCount <= baseAllotment
	) {
		return undefined;
	}

	const addedCount = formatCount(maxCount - baseAllotment);

	if (addOnBucketCount === 1) {
		return i18n.sub(
			'includes-1-add-on-bucket-x-on-top-of-the-y-base-allotment-per-month',
			[addedCount, formatCount(baseAllotment)]
		);
	}

	return i18n.sub(
		'includes-x-add-on-buckets-y-on-top-of-the-z-base-allotment-per-month',
		[String(addOnBucketCount), addedCount, formatCount(baseAllotment)]
	);
}

function getRenewalDays(endDate?: string): number | undefined {
	if (!endDate) {
		return undefined;
	}

	const end = new Date(endDate).getTime();

	if (Number.isNaN(end)) {
		return undefined;
	}

	const days = Math.ceil((end - Date.now()) / MILLISECONDS_PER_DAY);

	return days > 0 ? days : undefined;
}

export default function LDPEventsWidget({
	projectExternalReferenceCode,
}: LDPEventsWidgetProps) {
	const [granularity, setGranularity] = useState<Granularity>('day');

	const [pickedDate, setPickedDate] = useState(getTodaySlashDate);

	const {selectedContractERC} = useProject();

	const {contract} = useProjectCommerce(
		projectExternalReferenceCode,
		selectedContractERC
	);

	const monthly = granularity === 'month';

	const bounds = monthly
		? getUTCMonthBounds(pickedDate)
		: getUTCDayBounds(pickedDate);

	const {error, eventUsage, isLoading} = useProjectEventUsage(
		projectExternalReferenceCode,
		bounds?.startDate,
		bounds?.endDate
	);

	const summedCount = useMemo(
		() =>
			(eventUsage?.eventSummary ?? []).reduce(
				(sum, dataSource) => sum + dataSource.eventsCount,
				0
			),
		[eventUsage]
	);

	const total = eventUsage?.usedCount ?? summedCount;

	const maxCount = eventUsage?.maxCount ?? 0;

	const capPercentage =
		monthly && maxCount > 0 ? (total / maxCount) * 100 : undefined;

	const barDenominator =
		monthly && maxCount > 0 ? Math.max(maxCount, summedCount) : summedCount;

	const dataSources = useMemo(
		() =>
			(eventUsage?.eventSummary ?? []).map(
				(dataSource: EventDataSource, index) => ({
					...dataSource,
					color: DATA_SOURCE_COLORS[
						index % DATA_SOURCE_COLORS.length
					],
					segment: barDenominator
						? (dataSource.eventsCount / barDenominator) * 100
						: 0,
					share: summedCount
						? (dataSource.eventsCount / summedCount) * 100
						: 0,
				})
			),
		[barDenominator, eventUsage, summedCount]
	);

	const addOnMessage = getAddOnMessage(eventUsage);

	const renewalDays = getRenewalDays(contract?.endDate);

	if (error) {
		return <UsageUnavailableCard />;
	}

	return (
		<div className="ldp-events-card mt-3">
			<div className="ldp-events-header">
				<div>
					<div className="ldp-events-title-row">
						<h3 className="ldp-events-title">
							{i18n.translate('events')}
						</h3>

						<Tooltip
							symbol="info-circle-open"
							tooltip={i18n.translate(
								'events-successfully-ingested-by-ldp-this-month-resets-monthly-add-on-buckets-raise-the-included-volume'
							)}
						/>
					</div>

					{renewalDays !== undefined && (
						<p className="ldp-events-subtitle">
							{renewalDays === 1
								? i18n.translate('renews-in-1-day')
								: i18n.sub(
										'renews-in-x-days',
										String(renewalDays)
									)}
						</p>
					)}
				</div>

				<div className="ldp-events-controls">
					{monthly ? (
						<LDPEventsMonthPicker
							onChange={setPickedDate}
							value={pickedDate}
						/>
					) : (
						<LDPEventsDayPicker
							onChange={setPickedDate}
							value={pickedDate}
						/>
					)}

					<div className="ldp-events-granularity">
						{(['month', 'day'] as Granularity[]).map((value) => (
							<button
								className={classNames(
									'ldp-events-granularity-option',
									{active: granularity === value}
								)}
								key={value}
								onClick={() => setGranularity(value)}
								type="button"
							>
								{i18n.translate(value)}
							</button>
						))}
					</div>
				</div>
			</div>

			{isLoading ? (
				<p className="mt-3 text-neutral-7">
					{i18n.translate('loading')}
				</p>
			) : (
				<>
					<div className="ldp-events-summary">
						<p className="ldp-events-total">
							<span className="ldp-events-total-used">
								{formatCount(total)}
							</span>

							{monthly && maxCount > 0 && (
								<span className="ldp-events-total-cap">
									{i18n.sub(
										'x-events',
										formatCount(maxCount)
									)}
								</span>
							)}
						</p>

						{capPercentage !== undefined && (
							<span className="ldp-events-cap-percentage">
								{formatPercentage(capPercentage)}
							</span>
						)}
					</div>

					<div className="ldp-events-bar">
						{dataSources
							.filter((dataSource) => dataSource.segment > 0)
							.map((dataSource) => (
								<div
									className="ldp-events-bar-segment"
									key={dataSource.dataSourceId}
									style={{
										backgroundColor: dataSource.color,
										width: `${dataSource.segment}%`,
									}}
								/>
							))}
					</div>

					{monthly && addOnMessage && (
						<p className="ldp-events-addon">{addOnMessage}</p>
					)}

					<ul
						className={classNames('ldp-events-breakdown', {
							'ldp-events-breakdown-split':
								dataSources.length >
								BREAKDOWN_SINGLE_COLUMN_LIMIT,
						})}
					>
						{dataSources.map((dataSource) => (
							<li
								className="ldp-events-breakdown-row"
								key={dataSource.dataSourceId}
							>
								<span
									className="ldp-events-breakdown-dot"
									style={{backgroundColor: dataSource.color}}
								/>

								<span className="ldp-events-breakdown-name">
									{dataSource.dataSourceName}
								</span>

								<span className="ldp-events-breakdown-count">
									{formatCount(dataSource.eventsCount)}
								</span>

								<span className="ldp-events-breakdown-share">
									{`${Math.round(dataSource.share)}%`}
								</span>
							</li>
						))}
					</ul>
				</>
			)}
		</div>
	);
}
