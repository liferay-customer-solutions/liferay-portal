/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import useSWR, {SWRConfiguration} from 'swr';
import PublisherDetails from '~/services/objects/PublisherDetails';
import SearchBuilder from '~/utils/SearchBuilder';

const usePublisherDetails = (
	catalogId?: number | null,
	swrOptions?: SWRConfiguration
) => {
	const {data, error, isLoading, mutate} = useSWR(
		catalogId ? `/publisher-details/${catalogId}` : null,
		async () => {
			const {items} = await PublisherDetails.getPublisherDetails(
				new URLSearchParams({
					filter: SearchBuilder.unquote(
						SearchBuilder.eq('catalogId', catalogId!)
					),
				})
			);

			return items?.[0] ?? null;
		},
		swrOptions
	);

	return {error, isLoading, mutate, publisherDetails: data};
};

export default usePublisherDetails;
