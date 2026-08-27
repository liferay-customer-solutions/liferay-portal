/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import classNames from 'classnames';
import {useState} from 'react';
import {
	formatUTCMonthYear,
	getUTCMonthNames,
	parseSlashDateUTC,
	toSlashDateUTC,
} from '~/utils/dateUtils';

import LDPEventsPeriodPicker from './LDPEventsPeriodPicker';

type LDPEventsMonthPickerProps = {
	alignment?: 'left' | 'right';
	maxValue?: string;
	minValue?: string;
	onChange: (value: string) => void;
	value: string;
};

const MONTHS_PER_YEAR = 12;

export default function LDPEventsMonthPicker({
	alignment,
	maxValue,
	minValue,
	onChange,
	value,
}: LDPEventsMonthPickerProps) {
	const maxDate = maxValue ? parseSlashDateUTC(maxValue) : undefined;

	const maxMonthIndex = maxDate
		? maxDate.getUTCFullYear() * MONTHS_PER_YEAR + maxDate.getUTCMonth()
		: undefined;

	const minDate = minValue ? parseSlashDateUTC(minValue) : undefined;

	const minMonthIndex = minDate
		? minDate.getUTCFullYear() * MONTHS_PER_YEAR + minDate.getUTCMonth()
		: undefined;

	const selectedDate = parseSlashDateUTC(value);
	const selectedMonth = selectedDate?.getUTCMonth();
	const selectedYear = selectedDate?.getUTCFullYear();

	const [year, setYear] = useState(
		() => selectedYear ?? new Date().getFullYear()
	);

	return (
		<LDPEventsPeriodPicker
			alignment={alignment}
			label={selectedDate ? formatUTCMonthYear(selectedDate) : value}
			onStepBack={() => setYear(year - 1)}
			onStepForward={() => setYear(year + 1)}
			title={String(year)}
		>
			{(close) => (
				<div className="ldp-events-month-grid">
					{getUTCMonthNames().map((monthName, month) => (
						<button
							className={classNames('ldp-events-month-cell', {
								selected:
									month === selectedMonth &&
									year === selectedYear,
							})}
							disabled={
								(maxMonthIndex !== undefined &&
									year * MONTHS_PER_YEAR + month >
										maxMonthIndex) ||
								(minMonthIndex !== undefined &&
									year * MONTHS_PER_YEAR + month <
										minMonthIndex)
							}
							key={monthName}
							onClick={() => {
								onChange(
									toSlashDateUTC(
										new Date(Date.UTC(year, month, 1))
									)
								);

								close();
							}}
							type="button"
						>
							{monthName}
						</button>
					))}
				</div>
			)}
		</LDPEventsPeriodPicker>
	);
}
