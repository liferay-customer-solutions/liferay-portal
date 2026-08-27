/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayDropDown from '@clayui/drop-down';
import {ClayCheckbox} from '@clayui/form';
import ClayIcon from '@clayui/icon';
import classNames from 'classnames';
import {useMemo, useState} from 'react';
import {
	CartesianGrid,
	Line,
	LineChart,
	ResponsiveContainer,
	Tooltip as RechartsTooltip,
	XAxis,
	YAxis,
} from 'recharts';
import {Tooltip} from '~/components/Tooltip/Tooltip';
import i18n, {Word} from '~/i18n';
import {useProjectEventHistory} from '~/pages/MyAccount/Projects/hooks/useProjectEventHistory';
import {getDataSourceColor} from '~/pages/MyAccount/Projects/utils/getDataSourceColor';
import {
	formatUTCMonthShort,
	getTodaySlashDate,
	getUTCDayBounds,
	getUTCMonthBounds,
	parseSlashDateUTC,
	parseUTCDateString,
	shiftUTCDays,
	shiftUTCMonths,
	toSlashDateUTC,
	toUTCDateString,
} from '~/utils/dateUtils';

import LDPEventHistoryTooltip from './LDPEventHistoryTooltip';
import LDPEventsDayPicker from './LDPEventsDayPicker';
import LDPEventsMonthPicker from './LDPEventsMonthPicker';
import UsageUnavailableBanner from './UsageUnavailableBanner';

import './LDPEventHistoryChart.css';

import type {EventHistoryGranularity} from '~/pages/MyAccount/Projects/hooks/useProjectEventHistory';

type LDPEventHistoryChartProps = {
	projectExternalReferenceCode: string;
};

const DEFAULT_POINTS = 12;

const DOT_RADIUS = 3;

const MAX_MONTH_SPAN_YEARS = 10;

const MILLION = 1000000;

const SHORT_YEAR_DIGITS = 2;

const TENTHS = 10;

const THOUSAND = 1000;

type GetRangeErrorOptions = {
	endDate?: string;
	from: string;
	granularity: EventHistoryGranularity;
	startDate?: string;
	to: string;
};

function getRangeError({
	endDate,
	from,
	granularity,
	startDate,
	to,
}: GetRangeErrorOptions): Word | undefined {
	const fromDate = parseSlashDateUTC(from);
	const toDate = parseSlashDateUTC(to);

	if (!fromDate || !toDate) {
		return undefined;
	}

	if (fromDate.getTime() === toDate.getTime()) {
		return 'the-start-and-end-dates-must-be-different';
	}

	if (fromDate.getTime() > toDate.getTime()) {
		return 'the-start-date-must-be-earlier-than-the-end-date';
	}

	if (granularity === 'day') {
		const earliest = parseSlashDateUTC(getDailyMinValue());

		return earliest && fromDate.getTime() < earliest.getTime()
			? 'the-daily-view-is-limited-to-the-current-and-previous-month'
			: undefined;
	}

	const endDateValue = endDate ? parseUTCDateString(endDate) : undefined;
	const startDateValue = startDate
		? parseUTCDateString(startDate)
		: undefined;

	if (!endDateValue || !startDateValue) {
		return undefined;
	}

	const maxEndDate = new Date(
		Date.UTC(
			startDateValue.getUTCFullYear() + MAX_MONTH_SPAN_YEARS,
			startDateValue.getUTCMonth(),
			startDateValue.getUTCDate()
		)
	);

	return maxEndDate.getTime() < endDateValue.getTime()
		? 'the-monthly-view-supports-a-date-range-of-up-to-ten-years'
		: undefined;
}

function getDailyMinValue(): string {
	const now = new Date();

	return toSlashDateUTC(
		new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth() - 1, 1))
	);
}

function getDefaultRange(granularity: EventHistoryGranularity): {
	from: string;
	to: string;
} {
	const today = getTodaySlashDate();

	if (granularity === 'day') {
		return {
			from: shiftUTCDays(today, -(DEFAULT_POINTS - 1)) ?? today,
			to: today,
		};
	}

	const to = shiftUTCMonths(today, 0) ?? today;

	return {
		from: shiftUTCMonths(today, -(DEFAULT_POINTS - 1)) ?? to,
		to,
	};
}

function clampToToday(endDate?: string): string | undefined {
	if (!endDate) {
		return undefined;
	}

	const today = toUTCDateString(new Date());

	return endDate > today ? today : endDate;
}

function roundToTenths(value: number): number {
	return Math.round(value * TENTHS) / TENTHS;
}

function formatAxisCount(count: number): string {
	if (count >= MILLION) {
		return `${roundToTenths(count / MILLION)}M`;
	}

	if (count >= THOUSAND) {
		return `${roundToTenths(count / THOUSAND)}k`;
	}

	return String(count);
}

function formatAxisDate(
	date: string,
	granularity: EventHistoryGranularity,
	withYear: boolean
): string {
	const parsed = parseUTCDateString(date);

	if (!parsed) {
		return date;
	}

	const year = String(parsed.getUTCFullYear()).slice(-SHORT_YEAR_DIGITS);

	if (granularity === 'month') {
		const month = formatUTCMonthShort(parsed);

		return withYear ? `${month} ${year}` : month;
	}

	const day = `${parsed.getUTCMonth() + 1}/${String(
		parsed.getUTCDate()
	).padStart(2, '0')}`;

	return withYear ? `${day}/${year}` : day;
}

export default function LDPEventHistoryChart({
	projectExternalReferenceCode,
}: LDPEventHistoryChartProps) {
	const [granularity, setGranularity] =
		useState<EventHistoryGranularity>('day');

	const [range, setRange] = useState(() => getDefaultRange('day'));

	const [selectedIds, setSelectedIds] = useState<string[]>([]);

	const [filterActive, setFilterActive] = useState(false);

	const dailyMinValue = getDailyMinValue();

	const today = getTodaySlashDate();

	const crossesYears = useMemo(() => {
		const fromDate = parseSlashDateUTC(range.from);
		const toDate = parseSlashDateUTC(range.to);

		return Boolean(
			fromDate &&
				toDate &&
				fromDate.getUTCFullYear() !== toDate.getUTCFullYear()
		);
	}, [range]);

	const monthly = granularity === 'month';

	const fromBounds = monthly
		? getUTCMonthBounds(range.from)
		: getUTCDayBounds(range.from);

	const toBounds = monthly
		? getUTCMonthBounds(range.to)
		: getUTCDayBounds(range.to);

	const endDate = clampToToday(toBounds?.endDate);

	const rangeError = getRangeError({
		endDate,
		from: range.from,
		granularity,
		startDate: fromBounds?.startDate,
		to: range.to,
	});

	const {error, eventHistory, isLoading} = useProjectEventHistory({
		endDate: rangeError ? undefined : endDate,
		granularity,
		projectExternalReferenceCode,
		startDate: rangeError ? undefined : fromBounds?.startDate,
	});

	const dataSources = useMemo(() => {
		const seen = new Map<string, string>();

		for (const point of eventHistory?.eventHistory ?? []) {
			for (const dataSource of point.eventSummary ?? []) {
				if (!seen.has(dataSource.dataSourceId)) {
					seen.set(
						dataSource.dataSourceId,
						dataSource.dataSourceName
					);
				}
			}
		}

		return [...seen].map(([dataSourceId, dataSourceName]) => ({
			color: getDataSourceColor(dataSourceId),
			dataSourceId,
			dataSourceName,
		}));
	}, [eventHistory]);

	const rows = useMemo(
		() =>
			(eventHistory?.eventHistory ?? []).map((point) => {
				const row: {[key: string]: number | string} = {
					date: point.date,
				};

				for (const dataSource of point.eventSummary ?? []) {
					row[dataSource.dataSourceId] = dataSource.eventsCount;
				}

				return row;
			}),
		[eventHistory]
	);

	const visibleDataSources = selectedIds.length
		? dataSources.filter((dataSource) =>
				selectedIds.includes(dataSource.dataSourceId)
			)
		: dataSources;

	const handleGranularityChange = (value: EventHistoryGranularity) => {
		setGranularity(value);
		setRange(getDefaultRange(value));
	};

	const toggleDataSource = (dataSourceId: string) =>
		setSelectedIds((current) =>
			current.includes(dataSourceId)
				? current.filter((id) => id !== dataSourceId)
				: [...current, dataSourceId]
		);

	const PeriodPicker = monthly ? LDPEventsMonthPicker : LDPEventsDayPicker;

	return (
		<div className="ldp-events-card mt-3">
			<div className="ldp-events-header">
				<div className="ldp-events-title-row">
					<h3 className="ldp-events-title">
						{i18n.translate('event-history')}
					</h3>
				</div>

				<Tooltip
					symbol="info-circle-open"
					tooltip={i18n.translate(
						'events-ingested-by-ldp-over-the-selected-period-broken-down-by-data-source'
					)}
				/>
			</div>

			<div className="ldp-event-history-controls">
				<div className="ldp-event-history-range">
					<span className="ldp-event-history-range-label">
						{i18n.translate('from')}
					</span>

					<PeriodPicker
						alignment="left"
						maxValue={today}
						minValue={monthly ? undefined : dailyMinValue}
						onChange={(from) =>
							setRange((current) => ({...current, from}))
						}
						value={range.from}
					/>

					<span className="ldp-event-history-range-label">
						{i18n.translate('to')}
					</span>

					<PeriodPicker
						alignment="left"
						maxValue={today}
						minValue={monthly ? undefined : dailyMinValue}
						onChange={(to) =>
							setRange((current) => ({...current, to}))
						}
						value={range.to}
					/>
				</div>

				<div className="ldp-events-controls">
					<ClayDropDown
						active={filterActive}
						menuElementAttrs={{
							className: 'ldp-event-history-filter-menu',
						}}
						onActiveChange={setFilterActive}
						trigger={
							<button
								className="ldp-events-period-trigger"
								type="button"
							>
								<span className="ldp-events-period-label">
									{!selectedIds.length
										? i18n.translate('all-data-sources')
										: selectedIds.length === 1
											? i18n.translate('1-data-source')
											: i18n.sub(
													'x-data-sources',
													String(selectedIds.length)
												)}
								</span>

								<ClayIcon symbol="caret-bottom" />
							</button>
						}
					>
						<div className="ldp-event-history-filter-options">
							<ClayCheckbox
								checked={!selectedIds.length}
								label={i18n.translate('all-data-sources')}
								onChange={() => setSelectedIds([])}
							/>

							{dataSources.map((dataSource) => (
								<ClayCheckbox
									checked={selectedIds.includes(
										dataSource.dataSourceId
									)}
									key={dataSource.dataSourceId}
									label={dataSource.dataSourceName}
									onChange={() =>
										toggleDataSource(
											dataSource.dataSourceId
										)
									}
								/>
							))}
						</div>
					</ClayDropDown>

					<div className="ldp-events-granularity">
						{(['month', 'day'] as EventHistoryGranularity[]).map(
							(value) => (
								<button
									className={classNames(
										'ldp-events-granularity-option',
										{active: granularity === value}
									)}
									key={value}
									onClick={() =>
										handleGranularityChange(value)
									}
									type="button"
								>
									{i18n.translate(
										value === 'month' ? 'monthly' : 'daily'
									)}
								</button>
							)
						)}
					</div>
				</div>
			</div>

			{rangeError ? (
				<p className="ldp-event-history-message">
					{i18n.translate(rangeError)}
				</p>
			) : error ? (
				<p className="ldp-event-history-message">
					{i18n.translate(
						'usage-data-is-temporarily-unavailable-please-try-again-later'
					)}
				</p>
			) : isLoading ? (
				<p className="mt-3 text-neutral-7">
					{i18n.translate('loading')}
				</p>
			) : (
				<>
					{eventHistory?.usageDataAvailable === false && (
						<div className="mt-3">
							<UsageUnavailableBanner />
						</div>
					)}

					<div className="ldp-event-history-chart">
						<ResponsiveContainer>
							<LineChart data={rows} tabIndex={-1}>
								<CartesianGrid stroke="var(--color-neutral-2, #e2e2e4)" />

								<XAxis
									dataKey="date"
									tickFormatter={(date) =>
										formatAxisDate(
											date,
											granularity,
											crossesYears
										)
									}
									tickLine={false}
								/>

								<YAxis
									tickFormatter={formatAxisCount}
									tickLine={false}
								/>

								<RechartsTooltip
									content={
										<LDPEventHistoryTooltip
											formatTitle={(date) =>
												formatAxisDate(
													date,
													granularity,
													crossesYears
												)
											}
										/>
									}
								/>

								{visibleDataSources.map((dataSource) => (
									<Line
										dataKey={dataSource.dataSourceId}
										dot={{
											fill: dataSource.color,
											r: DOT_RADIUS,
											strokeWidth: 0,
										}}
										key={dataSource.dataSourceId}
										name={dataSource.dataSourceName}
										stroke={dataSource.color}
									/>
								))}
							</LineChart>
						</ResponsiveContainer>
					</div>
				</>
			)}
		</div>
	);
}
