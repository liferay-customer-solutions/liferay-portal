/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import HelpSupportCard from '../HelpSupportCard/HelpSupportCard';
import LearnLinkCard from '../LearnLinkCard/LearnLinkCard';

import type {DeliveryProduct} from '~/types/product';

type HelpSupportTabProps = {
	learnUrl?: string;
	product: DeliveryProduct;
};

export default function HelpSupportTab({
	learnUrl,
	product,
}: HelpSupportTabProps) {
	if (learnUrl) {
		return <LearnLinkCard url={learnUrl} />;
	}

	return <HelpSupportCard specifications={product.productSpecifications} />;
}
