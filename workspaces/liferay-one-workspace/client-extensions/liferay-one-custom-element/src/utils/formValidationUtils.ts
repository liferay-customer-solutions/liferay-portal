/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {IYears} from '@clayui/date-picker/lib/types';
import {translate} from '~/i18n';

import type {ITimeInput} from '~/types/timeInput';

export function requiredTimeInput(value: ITimeInput) {
	if (!value || value.hours === '--' || value.minutes === '--') {
		return translate('this-field-is-required');
	}
}

export function isValidDate(value: string, years?: IYears) {
	if (!value) {
		return;
	}

	const date = new Date(value.replace(/-/g, '/'));

	if (date.toString() === 'Invalid Date') {
		return translate('please-insert-a-valid-date');
	}

	const year = date.getFullYear();

	if (years && (year > years?.end || year < years?.start)) {
		return translate('please-insert-a-valid-date');
	}
}
