/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import fetcher from '~/services/fetcher/fetcher';

export default class HeadlessCommerceAdminAccount {
	static async deleteAccountAddress(id: number) {
		return fetcher.delete(
			`/o/headless-commerce-admin-account/v1.0/accountAddresses/${id}`
		);
	}
}
