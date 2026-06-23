/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

export function normalizeURLProtocol(url = '') {
	if (window.location.href.startsWith('https')) {
		return url;
	}

	return url.replace('https', 'http');
}

export function removeHTMLTags(text: string) {
	return text.replace(/<\/?[^>]+(>|$)/g, '');
}
