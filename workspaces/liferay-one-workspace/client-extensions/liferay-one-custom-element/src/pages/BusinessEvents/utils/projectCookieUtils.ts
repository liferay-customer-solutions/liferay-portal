/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Liferay} from '~/services/liferay/liferay';

const COOKIE_EXPIRY_DAYS = 30;

function getCookieName(userId: string): string {
	return `LO_LAST_VIEWED_PROJECT_${userId}`;
}

export function getLastViewedProjectCookie(userId: string): string | undefined {
	const name = getCookieName(userId);

	return document.cookie
		.split('; ')
		.find((v) => v.startsWith(`${name}=`))
		?.split('=')
		.slice(1)
		.join('=');
}

export function setLastViewedProjectCookie(
	userId: string,
	accountKey: string
): void {
	const expires = new Date();

	expires.setDate(expires.getDate() + COOKIE_EXPIRY_DAYS);

	if (Liferay.Util.Cookie) {
		Liferay.Util.Cookie.set?.(
			getCookieName(userId),
			encodeURIComponent(accountKey),
			Liferay.Util.Cookie.TYPES.FUNCTIONAL,
			{expires, secure: true}
		);
	}
}
