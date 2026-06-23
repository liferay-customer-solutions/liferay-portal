/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import fetcher from '~/services/fetcher/fetcher';

type GetAppInformationBody = {
	dashboardLink: string;
	orderId: string;
	priceModel?: string;
	productName: string;
	productType?: string;
};

export default class GetAppInformations {
	static async postGetAppInformation(
		getAppInformation: GetAppInformationBody
	) {
		return fetcher.post('/o/c/getappinformations', getAppInformation);
	}
}
