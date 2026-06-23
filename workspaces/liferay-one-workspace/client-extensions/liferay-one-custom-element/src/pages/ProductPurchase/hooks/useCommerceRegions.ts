/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import useSWR from 'swr';
import HeadlessAdminAddress from '~/services/headless/HeadlessAdminAddress';

const useCommerceRegions = () =>
	useSWR('/admin-address-countries', () =>
		HeadlessAdminAddress.getCountries()
	);

export default useCommerceRegions;
