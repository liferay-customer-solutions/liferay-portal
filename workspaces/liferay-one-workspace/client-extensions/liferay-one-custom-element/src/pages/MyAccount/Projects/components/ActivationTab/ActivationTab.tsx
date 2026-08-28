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

import type {ProjectContract} from '~/hooks/useProjectCommerce';
import type {DeliveryProduct} from '~/types/product';

import type {ActivationProfile} from '../../utils/resolveActivationProfile';
import type {EnvironmentProfile} from '../../utils/resolveEnvironmentProfile';

type ActivationTabProps = {
	contract?: ProjectContract;
	environmentProfile?: EnvironmentProfile;
	orderId?: string;
	product: DeliveryProduct;
	profile?: ActivationProfile;
};

type ActivationContentProps = {
	contract?: ProjectContract;
	environmentProfile?: EnvironmentProfile;
	orderId?: string;
	product: DeliveryProduct;
};

const ACTIVATION_CONTENT_BY_PROFILE: Record<
	ActivationProfile,
	(props: ActivationContentProps) => ReactNode
> = {
	'app-licenses': () => null,
	'app-provisioning': ({orderId}) =>
		orderId ? <AppProvisioning orderId={orderId} /> : null,
	'cloud-native': () => <CloudNativeActivation />,
	'commerce': () => <CommerceActivation />,
	'dxp-portal': () => null,
	'enterprise-search': () => <EnterpriseSearchActivation />,
	'keys-list': () => null,
	'licenses': () => null,
	'none': () => null,
	'status': ({contract, environmentProfile, product}) => (
		<ActivationStatusCard
			contract={contract}
			environmentProfile={environmentProfile}
			productName={product.name}
		/>
	),
};

export default function ActivationTab({
	contract,
	environmentProfile,
	orderId,
	product,
	profile,
}: ActivationTabProps) {
	return ACTIVATION_CONTENT_BY_PROFILE[profile ?? 'none']({
		contract,
		environmentProfile,
		orderId,
		product,
	});
}
