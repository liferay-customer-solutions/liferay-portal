/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ReactNode} from 'react';

import ActivationStatusCard from '../ActivationStatusCard/ActivationStatusCard';
import AppProvisioning from '../AppProvisioning/AppProvisioning';
import CloudNativeActivation from '../CloudNativeActivation/CloudNativeActivation';
import CommerceActivation from '../CommerceActivation/CommerceActivation';
import EnterpriseSearchActivation from '../EnterpriseSearchActivation/EnterpriseSearchActivation';

import type {DeliveryProduct} from '~/types/product';

import type {ActivationProfile} from '../../utils/resolveActivationProfile';

type ActivationTabProps = {
	orderId?: string;
	product: DeliveryProduct;
	profile?: ActivationProfile;
};

const ACTIVATION_CONTENT_BY_PROFILE: Record<
	ActivationProfile,
	(orderId: string | undefined, product: DeliveryProduct) => ReactNode
> = {
	'app-licenses': () => null,
	'app-provisioning': (orderId) =>
		orderId ? <AppProvisioning orderId={orderId} /> : null,
	'cloud-native': () => <CloudNativeActivation />,
	'commerce': () => <CommerceActivation />,
	'dxp-portal': () => null,
	'enterprise-search': () => <EnterpriseSearchActivation />,
	'keys-list': () => null,
	'licenses': () => null,
	'none': () => null,
	'status': (orderId, product) => (
		<ActivationStatusCard productName={product.name} />
	),
};

export default function ActivationTab({
	orderId,
	product,
	profile,
}: ActivationTabProps) {
	return ACTIVATION_CONTENT_BY_PROFILE[profile ?? 'none'](orderId, product);
}
