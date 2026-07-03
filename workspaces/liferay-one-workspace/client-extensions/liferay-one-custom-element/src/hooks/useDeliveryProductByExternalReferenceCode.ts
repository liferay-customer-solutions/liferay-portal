/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import useSWR from 'swr';
import HeadlessCommerceDeliveryCatalog from '~/services/headless/HeadlessCommerceDeliveryCatalog';
import {Liferay} from '~/services/liferay/liferay';

const useDeliveryProductByExternalReferenceCode = (
	externalReferenceCode: string
) => {
	return useSWR(
		externalReferenceCode
			? `/delivery-product-by-external-reference-code/${externalReferenceCode}`
			: null,
		async () => {
			const {items} =
				await HeadlessCommerceDeliveryCatalog.getProductsPage(
					Liferay.CommerceContext.commerceChannelId,
					new URLSearchParams({
						'accountId': '-1',
						'attachments.accountId': '-1',
						'filter': `externalReferenceCode eq '${externalReferenceCode}'`,
						'images.accountId': '-1',
						'nestedFields':
							'attachments,categories,images,productSpecifications,skus',
						'pageSize': '1',
						'skus.accountId': '-1',
						'skus.currencyCode':
							Liferay.CommerceContext.currency.currencyCode,
					})
				);

			return items?.[0];
		}
	);
};

export {useDeliveryProductByExternalReferenceCode};
