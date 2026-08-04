/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

const PERMANENT_KEY_YEARS = 80;

export function isPermanentKey(
	expirationDate?: string,
	startDate?: string
): boolean {
	if (!expirationDate) {
		return false;
	}

	const expiration = new Date(expirationDate);
	const start = startDate ? new Date(startDate) : new Date();

	if (Number.isNaN(expiration.getTime()) || Number.isNaN(start.getTime())) {
		return false;
	}

	const threshold = new Date(start);

	threshold.setFullYear(threshold.getFullYear() + PERMANENT_KEY_YEARS);

	return expiration >= threshold;
}

export default isPermanentKey;
