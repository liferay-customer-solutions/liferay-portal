/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import fetcher from '~/services/fetcher/fetcher';

export type LicenseKeyEntry = {
	active?: boolean;
	additionalInfo?: string;
	customExpirationDate?: string;
	description?: string;
	domains?: string;
	hostName?: string;
	licenseName?: string;
	licenseType?: string;
	maxClusterNodes?: number;
	name: string;
	productName?: string;
	r_accountEntryToLicenseKey_accountEntryId?: number;
	r_projectToLicenseKey_c_projectERC?: string;
	startDate?: string;
};

export default class LicenseKeys {
	static createLicenseKey(body: LicenseKeyEntry) {
		return fetcher.post('/o/c/licensekeys', body);
	}

	static deactivateLicenseKey(externalReferenceCode: string) {
		return fetcher.patch(
			`/o/c/licensekeys/by-external-reference-code/${externalReferenceCode}`,
			{active: false}
		);
	}

	static reactivateLicenseKey(externalReferenceCode: string) {
		return fetcher.patch(
			`/o/c/licensekeys/by-external-reference-code/${externalReferenceCode}`,
			{active: true}
		);
	}
}
