/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import accountPlaceholder from '~/assets/images/app_placeholder.png';

export function getAccountImage(url?: string) {
	return url?.includes('img_id=0') || !url ? accountPlaceholder : url;
}
