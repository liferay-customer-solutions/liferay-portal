/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Liferay} from '~/services/liferay/liferay';

const dateOptions: Intl.DateTimeFormatOptions = {
	day: 'numeric',
	month: 'short',
	year: 'numeric',
};

const REFERENCE_YEAR = 2024;

let monthNames: string[] | undefined;

let weekdayNarrowNames: string[] | undefined;

function normalize(date: Date | string) {
	return typeof date === 'string' ? new Date(date) : date;
}

export function formatDate(date: Date | string | undefined, fallback = 'N/A') {
	if (!date) {
		return fallback;
	}

	try {
		return new Intl.DateTimeFormat(
			Liferay.ThemeDisplay.getBCP47LanguageId(),
			dateOptions
		).format(normalize(date));
	}
	catch {
		return fallback;
	}
}

export function formatDateTime(
	date: Date | string | undefined,
	fallback = 'N/A'
) {
	if (!date) {
		return fallback;
	}

	try {
		return new Intl.DateTimeFormat(
			Liferay.ThemeDisplay.getBCP47LanguageId(),
			{...dateOptions, hour: 'numeric', minute: 'numeric'}
		).format(normalize(date));
	}
	catch {
		return fallback;
	}
}

export function formatUTCMonthShort(date: Date): string {
	return new Intl.DateTimeFormat(Liferay.ThemeDisplay.getBCP47LanguageId(), {
		month: 'short',
		timeZone: 'UTC',
	}).format(date);
}

export function formatUTCMonthYear(date: Date): string {
	return new Intl.DateTimeFormat(Liferay.ThemeDisplay.getBCP47LanguageId(), {
		month: 'long',
		timeZone: 'UTC',
		year: 'numeric',
	}).format(date);
}

export function getLastDayOfMonth(month: number, year: number) {
	return new Date(year, month + 1, 0).getDate();
}

export function getTodaySlashDate(): string {
	return toSlashDateUTC(new Date());
}

export function getUTCDayBounds(
	value: string
): {endDate: string; startDate: string} | undefined {
	const date = parseSlashDateUTC(value);

	if (!date) {
		return undefined;
	}

	const dateString = toUTCDateString(date);

	return {endDate: dateString, startDate: dateString};
}

export function getUTCMonthBounds(
	value: string
): {endDate: string; startDate: string} | undefined {
	const date = parseSlashDateUTC(value);

	if (!date) {
		return undefined;
	}

	const month = date.getUTCMonth();
	const year = date.getUTCFullYear();

	return {
		endDate: toUTCDateString(new Date(Date.UTC(year, month + 1, 0))),
		startDate: toUTCDateString(new Date(Date.UTC(year, month, 1))),
	};
}

export function getUTCMonthNames(): string[] {
	if (!monthNames) {
		const dateTimeFormat = new Intl.DateTimeFormat(
			Liferay.ThemeDisplay.getBCP47LanguageId(),
			{month: 'long', timeZone: 'UTC'}
		);

		monthNames = Array.from({length: 12}, (unused, month) =>
			dateTimeFormat.format(new Date(Date.UTC(REFERENCE_YEAR, month, 1)))
		);
	}

	return monthNames;
}

export function getUTCWeekdayNarrowNames(firstDayOfWeek = 0): string[] {
	if (!weekdayNarrowNames) {
		const dateTimeFormat = new Intl.DateTimeFormat(
			Liferay.ThemeDisplay.getBCP47LanguageId(),
			{timeZone: 'UTC', weekday: 'narrow'}
		);

		const firstOfYear = new Date(Date.UTC(REFERENCE_YEAR, 0, 1));

		const firstSunday = 1 + ((7 - firstOfYear.getUTCDay()) % 7);

		weekdayNarrowNames = Array.from({length: 7}, (unused, offset) =>
			dateTimeFormat.format(
				new Date(Date.UTC(REFERENCE_YEAR, 0, firstSunday + offset))
			)
		);
	}

	return weekdayNarrowNames
		.slice(firstDayOfWeek)
		.concat(weekdayNarrowNames.slice(0, firstDayOfWeek));
}

export function parseSlashDateUTC(value: string): Date | undefined {
	const parts = value.split('/');

	if (parts.length !== 3) {
		return undefined;
	}

	const [month, day, year] = parts.map(Number);

	if (
		!Number.isFinite(day) ||
		!Number.isFinite(month) ||
		!Number.isFinite(year)
	) {
		return undefined;
	}

	const date = new Date(Date.UTC(year, month - 1, day));

	if (
		date.getUTCDate() !== day ||
		date.getUTCFullYear() !== year ||
		date.getUTCMonth() !== month - 1
	) {
		return undefined;
	}

	return date;
}

export function parseUTCDateString(value: string): Date | undefined {
	const date = new Date(`${value}T00:00:00Z`);

	return Number.isNaN(date.getTime()) ? undefined : date;
}

export function shiftUTCDays(value: string, days: number): string | undefined {
	const date = parseSlashDateUTC(value);

	if (!date) {
		return undefined;
	}

	date.setUTCDate(date.getUTCDate() + days);

	return toSlashDateUTC(date);
}

export function shiftUTCMonths(
	value: string,
	months: number
): string | undefined {
	const date = parseSlashDateUTC(value);

	if (!date) {
		return undefined;
	}

	return toSlashDateUTC(
		new Date(
			Date.UTC(date.getUTCFullYear(), date.getUTCMonth() + months, 1)
		)
	);
}

export function toSlashDateUTC(date: Date): string {
	const [year, month, day] = toUTCDateString(date).split('-');

	return `${month}/${day}/${year}`;
}

export function toUTCDateString(date: Date): string {
	return date.toISOString().slice(0, 'yyyy-MM-dd'.length);
}
