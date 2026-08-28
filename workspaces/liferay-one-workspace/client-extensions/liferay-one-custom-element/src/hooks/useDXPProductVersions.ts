/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useFetch} from '~/hooks/useFetch';
import SearchBuilder from '~/utils/SearchBuilder';

import type {APIResponse} from '~/types/api';

const MINIMUM_PRODUCT_GROUP_VERSION = '2024.Q1';

const QUARTERLY_VERSION_REGEX = /^DXP (\d{4})\.Q([1-4])\.(\d+)/;

const FILTER = [
	SearchBuilder.eq('productGroup', 'dxp'),
	SearchBuilder.eq('type', 'quarterly'),
	SearchBuilder.eq('versionLevel', 'patch'),
	SearchBuilder.ge(
		'productGroupVersion',
		`'${MINIMUM_PRODUCT_GROUP_VERSION}'`
	),
].join(' and ');

type ProductVersionNode = {
	externalReferenceCode: string;
	id: number;
	productVersion?: string;
};

function compareQuarterlyVersions(
	quarterlyVersion1: number[],
	quarterlyVersion2: number[]
) {
	for (let i = 0; i < quarterlyVersion1.length; i++) {
		if (quarterlyVersion1[i] !== quarterlyVersion2[i]) {
			return quarterlyVersion1[i] - quarterlyVersion2[i];
		}
	}

	return 0;
}

function parseQuarterlyVersion(productVersion: string) {
	const matches = productVersion.match(QUARTERLY_VERSION_REGEX);

	return matches
		? [Number(matches[1]), Number(matches[2]), Number(matches[3])]
		: null;
}

export function useDXPProductVersions(enabled = true) {
	const {data, isLoading: loading} = useFetch<
		APIResponse<ProductVersionNode>
	>(enabled ? '/o/c/productversions' : null, {
		params: {
			filter: FILTER,
			pageSize: -1,
		},
	});

	const productVersions = (data?.items ?? [])
		.map((node) => ({
			productVersion: node.productVersion ?? '',
			quarterlyVersion: parseQuarterlyVersion(node.productVersion ?? ''),
		}))
		.filter(({quarterlyVersion}) => quarterlyVersion)
		.sort((productVersion1, productVersion2) =>
			compareQuarterlyVersions(
				productVersion2.quarterlyVersion as number[],
				productVersion1.quarterlyVersion as number[]
			)
		)
		.map(({productVersion}) => productVersion);

	return {loading, productVersions};
}

export default useDXPProductVersions;
