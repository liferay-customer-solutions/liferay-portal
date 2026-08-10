/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Word} from '~/i18n';

export function getKeyType(licenseType?: string): Word {
	if (licenseType === 'virtual-cluster') {
		return 'virtual-cluster';
	}

	if (licenseType === 'cluster' || licenseType === 'developer-cluster') {
		return 'cluster';
	}

	return 'on-premise';
}

export default getKeyType;
