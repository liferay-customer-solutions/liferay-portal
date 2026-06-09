/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {downloadFile} from '../../utils/file';
import {OneSpringBootOAuth2} from './OAuth2Client';

/**
 * Admin CommonLicenseKey REST contract, served by the Spring Boot client
 * extension under `/o/one/v1/common-license-keys` and owned by LPD-89429.
 *
 * The endpoint shapes below are provisional: at the time of writing the
 * backend is not yet merged, the workspace `api.md` spec documents only the
 * customer-facing `/license-key/*` resource, and LPD-89429 documents the
 * customer download as the account-scoped
 * `GET /accounts/{accountKey}/product-groups/{group}/product-environments/{env}/common-license-key`
 * (a different flow from this admin list/download/delete by id). Keep every
 * path, query param, and field name in this file so the contract can be
 * reconciled with the backend owner in one place.
 */

export type ProductGroup = 'COMMERCE' | 'ENTERPRISE_SEARCH';

export type CommonLicenseKey = {
	endDate: string;
	id: number;
	name: string;
	productEnvironment: string;
	startDate: string;
};

class CommonLicenseKeyOAuth2 extends OneSpringBootOAuth2 {
	async deleteCommonLicenseKey(id: number) {
		await this.delete(`/${id}`);
	}

	async downloadCommonLicenseKey(id: number, name: string) {
		const response = await this.get<Response>(`/${id}/download`, {
			earlyReturn: true,
		});

		await downloadFile(name, response);
	}

	getCommonLicenseKeys({
		page,
		pageSize,
		productGroup,
	}: {
		page: number;
		pageSize: number;
		productGroup: ProductGroup;
	}): Promise<APIResponse<CommonLicenseKey>> {
		const searchParams = new URLSearchParams({
			page: String(page),
			pageSize: String(pageSize),
			productGroup,
		});

		return this.get(`?${searchParams}`);
	}

	uploadCommonLicenseKeys(productGroup: ProductGroup, files: File[]) {
		const formData = new FormData();

		formData.append('productGroup', productGroup);

		for (const file of files) {
			formData.append('files', file);
		}

		return this.post('', formData);
	}
}

const commonLicenseKeyOAuth2 = new CommonLicenseKeyOAuth2(
	'/common-license-keys'
);

export default commonLicenseKeyOAuth2;
