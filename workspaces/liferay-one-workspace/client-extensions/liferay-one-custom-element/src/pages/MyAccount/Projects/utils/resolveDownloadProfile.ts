/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {getSpecificationValue} from '~/hooks/useProjectCommerce';

import {resolveProfile} from './resolveProfile';

import type {OrderTypes} from '~/types/orders';
import type {DeliveryProduct} from '~/types/product';

import type {ProjectItemKind} from '../types';

export type DownloadProfile = 'app' | 'bundle' | 'none';

const DOWNLOAD_PROFILES: DownloadProfile[] = ['app', 'bundle', 'none'];

const DOWNLOAD_PROFILE_BY_ORDER_TYPE: Partial<
	Record<OrderTypes, DownloadProfile>
> = {
	CLIENT_EXTENSION: 'app',
	COMPOSITE_APP: 'app',
	DXP_APP: 'app',
	LOW_CODE_CONFIGURATION: 'app',
};

export function resolveDownloadProfile({
	kind,
	orderType,
	product,
}: {
	kind: ProjectItemKind;
	orderType?: string;
	product: DeliveryProduct;
}): DownloadProfile {
	if (kind === 'application') {
		return (
			(orderType &&
				DOWNLOAD_PROFILE_BY_ORDER_TYPE[orderType as OrderTypes]) ||
			'none'
		);
	}

	return resolveProfile(
		getSpecificationValue(product, 'project-download-profile'),
		DOWNLOAD_PROFILES,
		'none'
	);
}

export default resolveDownloadProfile;
