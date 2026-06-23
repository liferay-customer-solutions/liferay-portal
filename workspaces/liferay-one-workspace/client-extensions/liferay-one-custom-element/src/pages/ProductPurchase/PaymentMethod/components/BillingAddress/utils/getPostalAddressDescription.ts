/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import type {AccountPostalAddresses} from '~/types/accounts';

export default function getPostalAddressDescription(
	address: AccountPostalAddresses
) {
	const streetAddressLine2 = address.streetAddressLine2
		? `${address.streetAddressLine2}, `
		: '';

	return {
		description: `${address.streetAddressLine1}, ${streetAddressLine2}${address.addressLocality}, ${address.addressRegion}, ${address.addressCountry} ${address.postalCode}`,
		title: address.name,
	};
}
