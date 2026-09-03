/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {getSpecificationValue} from '~/hooks/useProjectCommerce';

import {getAppType} from './getAppType';
import {resolveProfile} from './resolveProfile';

import type {DeliveryProduct} from '~/types/product';

import type {ProjectItemKind} from '../types';
import type {AppType} from './getAppType';

export type DownloadProfile = 'app' | 'bundle' | 'none';

const DOWNLOAD_PROFILES: DownloadProfile[] = ['app', 'bundle', 'none'];

const DOWNLOAD_PROFILE_BY_APP_TYPE: Record<AppType, DownloadProfile> = {
	'client-extension': 'app',
	'cloud': 'none',
	'composite-app': 'app',
	'dxp': 'app',
	'low-code-configuration': 'app',
	'other': 'none',
};

export function resolveDownloadProfile({
	kind,
	product,
}: {
	kind: ProjectItemKind;
	product: DeliveryProduct;
}): DownloadProfile {
	const profile = resolveProfile(
		getSpecificationValue(product, 'project-download-profile'),
		DOWNLOAD_PROFILES,
		'none'
	);

	if (profile !== 'none') {
		return profile;
	}

	if (kind === 'application') {
		const appType = getAppType(product);

		return appType ? DOWNLOAD_PROFILE_BY_APP_TYPE[appType] : 'none';
	}

	return 'none';
}

export default resolveDownloadProfile;
