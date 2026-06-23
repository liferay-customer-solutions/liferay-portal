/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Liferay} from '~/services/liferay/liferay';

const FORMAT_DATE_TYPES = {
	day2DMonth2DYearN: {
		day: '2-digit',
		month: '2-digit',
		year: 'numeric',
	},
	day2DMonthSYearN: {
		day: '2-digit',
		month: 'short',
		year: 'numeric',
	},
} as const;

export function getFormattedDate(
	date: string | undefined,
	formatType: keyof typeof FORMAT_DATE_TYPES,
	timeZone?: string
): string {
	if (!date) {
		return '';
	}

	try {
		const parsedDate = new Date(date);

		if (isNaN(parsedDate.getTime())) {
			return 'Invalid Date';
		}

		const options: Intl.DateTimeFormatOptions = {
			...FORMAT_DATE_TYPES[formatType],
		};

		if (timeZone) {
			options.timeZone = timeZone;
		}

		return parsedDate.toLocaleDateString(
			Liferay.ThemeDisplay.getBCP47LanguageId(),
			options
		);
	}
	catch (error) {
		return 'Invalid Date';
	}
}

export default getFormattedDate;
