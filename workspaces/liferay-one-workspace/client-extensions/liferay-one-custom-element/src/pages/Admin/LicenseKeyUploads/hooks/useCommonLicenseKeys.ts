/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import useSWR from 'swr';
import CommonLicenseKeys, {
	CommonLicenseKey,
	ProductGroup,
} from '~/services/spring-boot/CommonLicenseKeys';

import type {APIResponse} from '~/types/api';

export const PAGE_SIZE = 20;

export default function useCommonLicenseKeys(
	productGroup: ProductGroup,
	page: number
) {
	return useSWR<APIResponse<CommonLicenseKey>>(
		['common-license-keys', productGroup, page],
		() =>
			CommonLicenseKeys.getCommonLicenseKeys({
				page,
				pageSize: PAGE_SIZE,
				productGroup,
			})
	);
}
