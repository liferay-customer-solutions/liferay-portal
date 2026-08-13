/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import useSWR from 'swr';
import {useMarketplaceContext} from '~/context/MarketplaceContextProvider';
import HeadlessAdminTaxonomy from '~/services/headless/HeadlessAdminTaxonomy';
import {getTaxonomyCategoryLabel} from '~/utils/getTaxonomyCategoryLabel';

export type VocabularyCategoryOption = {
	label: string;
	name: string;
	value: string;
};

const useGetVocabulariesAndCategories = (vocabulariesName: string[]) => {
	const {properties} = useMarketplaceContext();

	return useSWR({key: 'vocabularies', vocabulariesName}, async () => {
		const fn = properties.useSiteTaxonomyVocabularyQuery
			? HeadlessAdminTaxonomy.getSiteTaxonomyVocabulariesGraphQL
			: HeadlessAdminTaxonomy.getTaxonomyVocabulariesGraphQL;

		const response = await fn();

		const vocabularies: Record<
			string,
			{
				categories: VocabularyCategoryOption[];
				id: unknown;
				name: string;
			}
		> = {};

		for (const vocabularyName of vocabulariesName) {
			const vocabulary = response.items.find(
				({name}) => name === vocabularyName
			);

			if (!vocabulary) {
				continue;
			}

			vocabularies[vocabularyName] = {
				...vocabulary,
				categories: vocabulary.taxonomyCategories.items.map(
					(taxonomyCategory) => ({
						label: getTaxonomyCategoryLabel(taxonomyCategory.name),
						name: taxonomyCategory.name,
						value: `${taxonomyCategory.id}`,
					})
				),
			};
		}

		return vocabularies;
	});
};

export {useGetVocabulariesAndCategories};
