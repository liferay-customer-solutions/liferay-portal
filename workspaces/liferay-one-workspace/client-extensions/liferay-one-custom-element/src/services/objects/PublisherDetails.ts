/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import fetcher from '~/services/fetcher/fetcher';

import type {APIResponse} from '~/types/api';
import type {PublisherDetailsEntry} from '~/types/publisher';

export default class PublisherDetails {
	static async getPublisherDetails(params = new URLSearchParams()) {
		return fetcher<APIResponse<PublisherDetailsEntry>>(
			`/o/c/publisherdetails?${params}`
		);
	}

	static async getPublisherDetailsById(
		entryId: number | string,
		params = new URLSearchParams()
	) {
		return fetcher<PublisherDetailsEntry>(
			`/o/c/publisherdetails/${entryId}?${params}`
		);
	}

	static async patchPublisherDetails(
		entryId: number | string,
		body: Partial<PublisherDetailsEntry>
	) {
		return fetcher.patch<PublisherDetailsEntry>(
			`/o/c/publisherdetails/${entryId}`,
			body
		);
	}
}
