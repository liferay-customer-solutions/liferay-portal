/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useFetch} from '~/hooks/useFetch';
import {Liferay} from '~/services/liferay/liferay';

import type {APIResponse} from '~/types/api';

type ProjectAPIItem = {
	id: number;
};

export function useHasProject(): {hasProject: boolean; loading: boolean} {
	const accountId = Liferay.CommerceContext?.account?.accountId;

	const {data, isLoading} = useFetch<APIResponse<ProjectAPIItem>>(
		accountId ? '/o/c/projects' : null,
		{
			params: {
				fields: 'id',
				filter: `r_accountEntryToProject_accountEntryId eq '${accountId}'`,
				pageSize: 1,
			},
		}
	);

	return {hasProject: (data?.totalCount ?? 0) > 0, loading: isLoading};
}
