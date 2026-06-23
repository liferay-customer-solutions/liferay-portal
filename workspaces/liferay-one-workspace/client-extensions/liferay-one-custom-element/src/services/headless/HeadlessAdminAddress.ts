/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import fetcher from '~/services/fetcher/fetcher';

import type {APIResponse} from '~/types/api';

export type AddressCountryRegion = {
	name: string;
	regionCode: string;
};

export type AddressCountry = {
	a2: string;
	active: boolean;
	name: string;
	regions: AddressCountryRegion[];
	title_i18n: {[key: string]: string};
};

export default class HeadlessAdminAddress {
	static async getCountries(
		searchParams = new URLSearchParams({
			nestedFields: 'regions',
			pageSize: '-1',
		})
	) {
		return fetcher<APIResponse<AddressCountry>>(
			`/o/headless-admin-address/v1.0/countries?${searchParams.toString()}`
		);
	}
}
