/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {buildDetailsSections} from '~/pages/MyAccount/Projects/utils/buildDetailsSections';
import {Liferay} from '~/services/liferay/liferay';

import SectionedDetailsCard from '../SectionedDetailsCard/SectionedDetailsCard';

import type {ProjectContract} from '~/hooks/useProjectCommerce';
import type {ProductOrderInfo} from '~/hooks/useProjectOrders';
import type {DetailsProfile} from '~/pages/MyAccount/Projects/utils/resolveDetailsProfile';

type DetailsTabProps = {
	contract?: ProjectContract;
	orderInfo: ProductOrderInfo;
	profile: DetailsProfile;
};

export default function DetailsTab({
	contract,
	orderInfo,
	profile,
}: DetailsTabProps) {
	const sections = buildDetailsSections(profile, {
		accountName: Liferay.CommerceContext.account?.accountName ?? '',
		contract,
		orderInfo,
	});

	return <SectionedDetailsCard sections={sections} />;
}
