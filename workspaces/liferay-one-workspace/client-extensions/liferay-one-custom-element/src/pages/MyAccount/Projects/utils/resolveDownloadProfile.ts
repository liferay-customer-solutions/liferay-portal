/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {getSpecificationValue} from '~/hooks/useProjectCommerce';

import type {DeliveryProduct} from '~/types/product';

export type DownloadProfile = 'app' | 'bundle' | 'none';

const DOWNLOAD_PROFILES: DownloadProfile[] = ['app', 'bundle', 'none'];

function isDownloadProfile(value: string): value is DownloadProfile {
	return (DOWNLOAD_PROFILES as string[]).includes(value);
}

export function resolveDownloadProfile(
	product: DeliveryProduct
): DownloadProfile {
	const profile = getSpecificationValue(product, 'project-download-profile');

	return isDownloadProfile(profile) ? profile : 'none';
}

export default resolveDownloadProfile;
