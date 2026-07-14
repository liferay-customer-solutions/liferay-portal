/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import fetcher from '~/services/fetcher/fetcher';

export type DXPFreeActivationKeyRequest = {
	businessEmailAddress: string;
	companyName?: string;
	country: string;
	domain: string;
	extension?: string;
	fullName: string;
	intlCode?: string;
	jobTitle?: string;
	notifyMe?: boolean;
	phoneNumber?: string;
	purpose: string;
	r_orderToDXPFreeActivationKeyRequest_commerceOrderId?: string;
};

export default class DXPFreeActivationKeyRequests {
	static async createDXPFreeActivationKeyRequest(
		body: DXPFreeActivationKeyRequest
	) {
		return fetcher.post('/o/c/dxpfreeactivationkeyrequests', body);
	}
}
