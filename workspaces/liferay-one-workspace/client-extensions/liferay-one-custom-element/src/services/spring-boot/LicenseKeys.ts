/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {OneSpringBootOAuth2} from './OAuth2Client';

export type LicenseKey = {
	active: boolean;
	customExpirationDate: string;
	domains: string;
	id: number;
	name: string;
	orderId: string;
	owner: string;
	productName: string;
	startDate: string;
};

class LicenseKeysOAuth2 extends OneSpringBootOAuth2 {
	createLicenseKeyTypeFree({
		domains,
		orderId,
		owner,
	}: {
		domains: string;
		orderId: string;
		owner: string;
	}): Promise<LicenseKey> {
		return this.post('/type-free', {domains, orderId, owner});
	}

	async licenseKeyTypeFreeDomainsCheck({
		domains,
		owner,
	}: {
		domains: string;
		owner: string;
	}) {
		await this.post('/type-free-domains-check', {domains, owner});
	}
}

const LicenseKeys = new LicenseKeysOAuth2('/license-keys');

export default LicenseKeys;
