/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {downloadFile} from '~/utils/downloadFile';

import {OneSpringBootOAuth2} from './OAuth2Client';

import type {APIResponse} from '~/types/api';

export type ProductGroup = 'COMMERCE' | 'ENTERPRISE_SEARCH';

export type CommonLicenseKey = {
	endDate: string;
	id: number;
	name: string;
	productEnvironment: string;
	startDate: string;
};

class CommonLicenseKeysOAuth2 extends OneSpringBootOAuth2 {
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

const CommonLicenseKeys = new CommonLicenseKeysOAuth2('/common-license-keys');

export default CommonLicenseKeys;
