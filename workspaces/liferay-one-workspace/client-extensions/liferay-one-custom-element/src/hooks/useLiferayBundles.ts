/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useFetch} from '~/hooks/useFetch';

import type {APIResponse} from '~/types/api';

export type LiferayBundle = {
	id: string;
	link: string;
	name: string;
};

type LiferayBundleNode = {
	bundleLink?: string;
	bundleName: string;
	externalReferenceCode: string;
};

export function useLiferayBundles() {
	const {
		data,
		error,
		isLoading: loading,
	} = useFetch<APIResponse<LiferayBundleNode>>('/o/c/liferaybundles', {
		params: {pageSize: 200, sort: 'bundleName:asc'},
	});

	const bundles: LiferayBundle[] = (data?.items ?? []).map((node) => ({
		id: node.externalReferenceCode,
		link: node.bundleLink ?? '',
		name: node.bundleName,
	}));

	return {bundles, error, loading};
}
