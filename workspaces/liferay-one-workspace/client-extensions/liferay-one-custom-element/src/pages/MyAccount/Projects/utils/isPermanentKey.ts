/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

const PERMANENT_KEY_YEAR = 2100;

export function isPermanentKey(expirationDate?: string): boolean {
	if (!expirationDate) {
		return false;
	}

	const date = new Date(expirationDate);

	return (
		!Number.isNaN(date.getTime()) && date.getFullYear() > PERMANENT_KEY_YEAR
	);
}

export default isPermanentKey;
