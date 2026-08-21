/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import classNames from 'classnames';
import {useState} from 'react';
import {
	getUTCMonthNames,
	getUTCWeekdayNarrowNames,
	parseSlashDateUTC,
	toSlashDateUTC,
	toUTCDateString,
} from '~/utils/dateUtils';

import LDPEventsPeriodPicker from './LDPEventsPeriodPicker';

type LDPEventsDayPickerProps = {
	onChange: (value: string) => void;
	value: string;
};

const CALENDAR_CELLS = 42;

const FIRST_DAY_OF_WEEK = 1;

export default function LDPEventsDayPicker({
	onChange,
	value,
}: LDPEventsDayPickerProps) {
	const selectedDate = parseSlashDateUTC(value) ?? new Date();

	const [viewMonth, setViewMonth] = useState(() =>
		selectedDate.getUTCMonth()
	);

	const [viewYear, setViewYear] = useState(() =>
		selectedDate.getUTCFullYear()
	);

	const shiftMonth = (offset: number) => {
		const shifted = new Date(Date.UTC(viewYear, viewMonth + offset, 1));

		setViewMonth(shifted.getUTCMonth());
		setViewYear(shifted.getUTCFullYear());
	};

	const leadingDays =
		(new Date(Date.UTC(viewYear, viewMonth, 1)).getUTCDay() -
			FIRST_DAY_OF_WEEK +
			7) %
		7;

	const selectedDateString = toUTCDateString(selectedDate);

	const cells = Array.from({length: CALENDAR_CELLS}, (unused, index) => {
		const date = new Date(
			Date.UTC(viewYear, viewMonth, index + 1 - leadingDays)
		);

		return {
			date,
			outside: date.getUTCMonth() !== viewMonth,
			selected: toUTCDateString(date) === selectedDateString,
		};
	});

	return (
		<LDPEventsPeriodPicker
			label={value}
			onStepBack={() => shiftMonth(-1)}
			onStepForward={() => shiftMonth(1)}
			title={`${getUTCMonthNames()[viewMonth]} ${viewYear}`}
		>
			{(close) => (
				<div className="ldp-events-day-grid">
					{getUTCWeekdayNarrowNames(FIRST_DAY_OF_WEEK).map(
						(weekday, index) => (
							<span
								className="ldp-events-day-weekday"
								key={index}
							>
								{weekday}
							</span>
						)
					)}

					{cells.map((cell) => (
						<button
							className={classNames('ldp-events-day-cell', {
								outside: cell.outside,
								selected: cell.selected,
							})}
							key={toUTCDateString(cell.date)}
							onClick={() => {
								onChange(toSlashDateUTC(cell.date));

								close();
							}}
							type="button"
						>
							{cell.date.getUTCDate()}
						</button>
					))}
				</div>
			)}
		</LDPEventsPeriodPicker>
	);
}
