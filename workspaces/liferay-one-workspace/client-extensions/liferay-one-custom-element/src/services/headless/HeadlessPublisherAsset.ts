/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import fetcher from '~/services/fetcher/fetcher';

import type {APIResponse} from '~/types/api';
import type {PublisherAsset} from '~/types/publisherAsset';

export default class HeadlessPublisherAsset {
	static async createPublisherAsset(body: unknown) {
		return fetcher.post<PublisherAsset>('o/c/publisherassets', body);
	}

	static async deletePublisherAsset(id: number | string) {
		return fetcher.delete(`o/c/publisherassets/${id}`);
	}

	static getPublisherAssets(searchParams: URLSearchParams) {
		return fetcher<APIResponse<PublisherAsset>>(
			`o/c/publisherassets?${searchParams.toString()}`
		);
	}
}
