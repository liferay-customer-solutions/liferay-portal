/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import useSWR from 'swr';
import fetcher from '~/services/fetcher/fetcher';
import type {BillingAddress} from '~/types/orders';

const useAccountAddresses = (accountId?: number) =>
	useSWR(accountId ? `/account-addresses/${accountId}` : null, () =>
		fetcher<{items: BillingAddress[]}>(
			`/o/headless-commerce-admin-account/v1.0/accounts/${accountId}/accountAddresses`
		)
	);

export default useAccountAddresses;
