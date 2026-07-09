/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {resolveActivationProfile} from '~/pages/MyAccount/Projects/utils/getActivationProfile';

import type {DeliveryProduct} from '~/types/product';

import type {ProjectItemKind} from '../../types';

import ActivationKeysCard from '../ActivationKeysCard/ActivationKeysCard';
import ActivationKeysTable from '../ActivationKeysTable/ActivationKeysTable';
import ActivationStatusCard from '../ActivationStatusCard/ActivationStatusCard';
import CloudNativeActivation from '../CloudNativeActivation/CloudNativeActivation';
import CommerceActivation from '../CommerceActivation/CommerceActivation';
import EnterpriseSearchActivation from '../EnterpriseSearchActivation/EnterpriseSearchActivation';
import LicensesTable from '../LicensesTable/LicensesTable';

type ActivationPanelProps = {
	kind: ProjectItemKind;
	orderType?: string;
	product: DeliveryProduct;
};

export default function ActivationPanel({
	kind,
	orderType,
	product,
}: ActivationPanelProps) {
	const profile = resolveActivationProfile({kind, orderType, product});

	switch (profile) {
		case 'app-licenses':
			return (
				<LicensesTable
					productName={product.name}
					variant="app-licenses"
				/>
			);

		case 'cloud-native':
			return <CloudNativeActivation />;

		case 'commerce':
			return <CommerceActivation />;

		case 'dxp-portal':
			return <ActivationKeysTable />;

		case 'enterprise-search':
			return <EnterpriseSearchActivation />;

		case 'keys-list':
			return <ActivationKeysCard productName={product.name} />;

		case 'licenses':
			return (
				<LicensesTable productName={product.name} variant="licenses" />
			);

		case 'status':
			return <ActivationStatusCard productName={product.name} />;

		default:
			return null;
	}
}
