/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Liferay} from '~/services/liferay/liferay';

const COOKIE_EXPIRY_DAYS = 30;

function getCookieName(accountId: string, userId: string): string {
	return `LO_LAST_VIEWED_PROJECT_${userId}_${accountId}`;
}

export function getLastViewedProjectCookie(
	accountId: string,
	userId: string
): string | undefined {
	const name = getCookieName(accountId, userId);

	const value = document.cookie
		.split('; ')
		.find((v) => v.startsWith(`${name}=`))
		?.split('=')
		.slice(1)
		.join('=');

	return value ? decodeURIComponent(value) : undefined;
}

export function setLastViewedProjectCookie(
	accountId: string,
	projectERC: string,
	userId: string
): void {
	const expires = new Date();

	expires.setDate(expires.getDate() + COOKIE_EXPIRY_DAYS);

	if (Liferay.Util.Cookie) {
		Liferay.Util.Cookie.set?.(
			getCookieName(accountId, userId),
			encodeURIComponent(projectERC),
			Liferay.Util.Cookie.TYPES.FUNCTIONAL,
			{expires, secure: true}
		);
	}
}
