/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useFetch} from '~/hooks/useFetch';
import {Liferay} from '~/services/liferay/liferay';

import type {APIResponse} from '~/types/api';

export type ProjectEnvironment = {
	activationMode: string;
	currentEntitlementHash: string;
	domains: string;
	externalReferenceCode: string;
	hostName: string;
	id: string;
	region: string;
	status: string;
	type: string;
};

type EnvironmentNode = {
	activationMode?: string;
	currentEntitlementHash?: string;
	domains?: string;
	externalReferenceCode: string;
	hostName?: string;
	id: number;
	region?: string;
	status?: string;
	type?: string;
};

export function useProjectEnvironments() {
	const accountId = Liferay.CommerceContext.account?.accountId;

	const {
		data,
		error,
		isLoading: loading,
	} = useFetch<APIResponse<EnvironmentNode>>(
		accountId ? '/o/c/environments' : null,
		{
			params: {
				filter: `r_accountEntryToEnvironment_accountEntryId eq '${accountId}'`,
				pageSize: 200,
				sort: 'type:asc',
			},
		}
	);

	const environments: ProjectEnvironment[] = (data?.items ?? []).map(
		(node) => ({
			activationMode: node.activationMode ?? '',
			currentEntitlementHash: node.currentEntitlementHash ?? '',
			domains: node.domains ?? '',
			externalReferenceCode: node.externalReferenceCode,
			hostName: node.hostName ?? '',
			id: String(node.id),
			region: node.region ?? '',
			status: node.status ?? '',
			type: node.type ?? '',
		})
	);

	return {environments, error, loading};
}

export default useProjectEnvironments;
