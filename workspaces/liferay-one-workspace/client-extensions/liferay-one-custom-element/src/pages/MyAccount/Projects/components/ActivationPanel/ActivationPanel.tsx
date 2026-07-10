/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ReactNode} from 'react';

import ActivationKeysCard from '../ActivationKeysCard/ActivationKeysCard';
import ActivationKeysTable from '../ActivationKeysTable/ActivationKeysTable';
import ActivationStatusCard from '../ActivationStatusCard/ActivationStatusCard';
import CloudNativeActivation from '../CloudNativeActivation/CloudNativeActivation';
import CommerceActivation from '../CommerceActivation/CommerceActivation';
import EnterpriseSearchActivation from '../EnterpriseSearchActivation/EnterpriseSearchActivation';
import LicensesTable from '../LicensesTable/LicensesTable';

import type {DeliveryProduct} from '~/types/product';

import type {ActivationProfile} from '../../utils/resolveActivationProfile';

type ActivationPanelProps = {
	product: DeliveryProduct;
	profile: ActivationProfile;
};

const ACTIVATION_CONTENT_BY_PROFILE: Record<
	ActivationProfile,
	(product: DeliveryProduct) => ReactNode
> = {
	'app-licenses': (product) => (
		<LicensesTable productName={product.name} variant="app-licenses" />
	),
	'cloud-native': () => <CloudNativeActivation />,
	'commerce': () => <CommerceActivation />,
	'dxp-portal': (product) => (
		<ActivationKeysTable productName={product.name} />
	),
	'enterprise-search': () => <EnterpriseSearchActivation />,
	'keys-list': (product) => <ActivationKeysCard productName={product.name} />,
	'licenses': (product) => (
		<LicensesTable productName={product.name} variant="licenses" />
	),
	'none': () => null,
	'status': (product) => <ActivationStatusCard productName={product.name} />,
};

export default function ActivationPanel({
	product,
	profile,
}: ActivationPanelProps) {
	return ACTIVATION_CONTENT_BY_PROFILE[profile](product);
}
