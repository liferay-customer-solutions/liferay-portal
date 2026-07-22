/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import useSWR, {SWRConfiguration} from 'swr';
import HeadlessCommerceAdminCatalog from '~/services/headless/HeadlessCommerceAdminCatalog';

const useAdminSolution = (productId: string, swrOptions?: SWRConfiguration) => {
	return useSWR(
		`/admin-solution/${productId}`,
		() =>
			HeadlessCommerceAdminCatalog.getProduct(
				productId,
				new URLSearchParams({
					nestedFields: 'attachments,images,productSpecifications',
				})
			),
		swrOptions
	);
};

export default useAdminSolution;
