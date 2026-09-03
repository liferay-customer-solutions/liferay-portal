/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {getSpecificationValue} from '~/hooks/useProjectCommerce';

import {getAppType} from './getAppType';

import type {DeliveryProduct} from '~/types/product';

import type {ProjectItemKind} from '../types';
import type {AppType} from './getAppType';

export type ActivationProfile =
	| 'app-licenses'
	| 'app-provisioning'
	| 'cloud-native'
	| 'commerce'
	| 'dxp-portal'
	| 'enterprise-search'
	| 'keys-list'
	| 'licenses'
	| 'none'
	| 'status';

const ACTIVATION_PROFILES: ActivationProfile[] = [
	'app-licenses',
	'app-provisioning',
	'cloud-native',
	'commerce',
	'dxp-portal',
	'enterprise-search',
	'keys-list',
	'licenses',
	'none',
	'status',
];

const ACTIVATION_PROFILE_BY_APP_TYPE: Record<AppType, ActivationProfile> = {
	'client-extension': 'app-licenses',
	'cloud': 'app-provisioning',
	'composite-app': 'app-licenses',
	'dxp': 'app-licenses',
	'low-code-configuration': 'none',
	'other': 'none',
};

function isActivationProfile(value: string): value is ActivationProfile {
	return (ACTIVATION_PROFILES as string[]).includes(value);
}

export function resolveActivationProfile({
	kind,
	product,
}: {
	kind: ProjectItemKind;
	product: DeliveryProduct;
}): ActivationProfile {
	const profile = getSpecificationValue(
		product,
		'project-activation-profile'
	);

	if (isActivationProfile(profile)) {
		return profile;
	}

	if (kind === 'application') {
		const appType = getAppType(product);

		return appType
			? ACTIVATION_PROFILE_BY_APP_TYPE[appType]
			: 'app-licenses';
	}

	return 'none';
}

export default resolveActivationProfile;
