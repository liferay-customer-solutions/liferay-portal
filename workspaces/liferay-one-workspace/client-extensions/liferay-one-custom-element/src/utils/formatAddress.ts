/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Liferay} from '~/services/liferay/liferay';

import type {BillingAddress} from '~/types/orders';

export function formatAddress(address: BillingAddress) {
	if (!address || !Object.keys(address).length) {
		return '-';
	}

	const displayNames = new Intl.DisplayNames(
		[Liferay.ThemeDisplay.getBCP47LanguageId()],
		{type: 'region'}
	);

	return [
		address.street1,
		address.city,
		address.regionISOCode,
		address.zip,
		displayNames.of(address.countryISOCode as string),
	]
		.filter(Boolean)
		.join(', ');
}

export default formatAddress;
