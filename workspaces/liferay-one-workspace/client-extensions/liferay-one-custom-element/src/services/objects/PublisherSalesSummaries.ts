/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import fetcher from '~/services/fetcher/fetcher';

import type {APIResponse} from '~/types/api';
import type {PublisherSalesSummaryEntry} from '~/types/publisher';

export default class PublisherSalesSummaries {
	static async getPublisherSalesSummaryById(
		entryId: string | number,
		params = new URLSearchParams()
	) {
		return fetcher<PublisherSalesSummaryEntry>(
			`/o/c/publishersalessummaries/${entryId}?${params}`
		);
	}

	static async patchPublisherSalesSummary(
		body: Partial<PublisherSalesSummaries>,
		entryId: number | string
	) {
		return fetcher.patch<APIResponse<PublisherSalesSummaryEntry>>(
			`/o/c/publishersalessummaries/${entryId}`,
			body
		);
	}
}
