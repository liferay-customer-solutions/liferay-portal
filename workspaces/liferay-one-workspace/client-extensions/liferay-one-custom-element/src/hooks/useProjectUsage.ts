/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useProject} from '~/context/ProjectContext';
import {useFetch} from '~/hooks/useFetch';

import type {APIResponse} from '~/types/api';

const GRANT_TYPE_UNLIMITED = 'unlimited';

export type ProjectUsage = {
	consumed: number;
	included: number;
	period: string;
	unit: string;
	unlimited: boolean;
};

type EntitlementDefinitionNode = {
	r_usageDefinitionToEntitlementDefinition_c_usageDefinitionId?: number;
	unit?: string;
};

type EntitlementNode = {
	grantType?: string;
	id: number;
	quantity?: number;
	r_entitlementDefinitionToEntitlement_c_entitlementDefinition?: EntitlementDefinitionNode;
};

type UsageDefinitionNode = {
	id: number;
	period?: string;
	quantity?: number;
	unit?: string;
};

type UsageEventNode = {
	quantity?: number;
	r_entitlementToUsageEvent_c_entitlementId?: number;
};

type Allowance = {
	entitlementIds: number[];
	included: number;
	unlimited: boolean;
};

export function useProjectUsage() {
	const {projectId} = useProject();

	const {data: entitlementsData} = useFetch<APIResponse<EntitlementNode>>(
		projectId ? '/o/c/entitlements' : null,
		{
			params: {
				filter: `r_projectToEntitlement_c_projectERC eq '${projectId}'`,
				nestedFields: 'entitlementDefinition',
				pageSize: 200,
			},
		}
	);

	const allowancesByDefinitionId = new Map<number, Allowance>();

	for (const entitlement of entitlementsData?.items ?? []) {
		const definition =
			entitlement.r_entitlementDefinitionToEntitlement_c_entitlementDefinition;

		const usageDefinitionId =
			definition?.r_usageDefinitionToEntitlementDefinition_c_usageDefinitionId;

		if (!usageDefinitionId) {
			continue;
		}

		const allowance = allowancesByDefinitionId.get(usageDefinitionId) ?? {
			entitlementIds: [],
			included: 0,
			unlimited: false,
		};

		allowance.entitlementIds.push(entitlement.id);
		allowance.included += entitlement.quantity ?? 0;
		allowance.unlimited =
			allowance.unlimited ||
			entitlement.grantType === GRANT_TYPE_UNLIMITED;

		allowancesByDefinitionId.set(usageDefinitionId, allowance);
	}

	const entitlementIds = Array.from(
		allowancesByDefinitionId.values()
	).flatMap((allowance) => allowance.entitlementIds);

	const eventFilter = entitlementIds
		.map((id) => `r_entitlementToUsageEvent_c_entitlementId eq '${id}'`)
		.join(' or ');

	const {data: definitionsData} = useFetch<APIResponse<UsageDefinitionNode>>(
		allowancesByDefinitionId.size ? '/o/c/usagedefinitions' : null,
		{params: {pageSize: 200}}
	);

	const {
		data: eventsData,
		error,
		isLoading: loading,
	} = useFetch<APIResponse<UsageEventNode>>(
		entitlementIds.length ? '/o/c/usageevents' : null,
		{params: {filter: eventFilter, pageSize: 500}}
	);

	const consumedByEntitlementId = new Map<number, number>();

	for (const event of eventsData?.items ?? []) {
		const entitlementId = event.r_entitlementToUsageEvent_c_entitlementId;

		if (entitlementId === undefined) {
			continue;
		}

		consumedByEntitlementId.set(
			entitlementId,
			(consumedByEntitlementId.get(entitlementId) ?? 0) +
				(event.quantity ?? 0)
		);
	}

	const usage: ProjectUsage[] = (definitionsData?.items ?? [])
		.filter((definition) => allowancesByDefinitionId.has(definition.id))
		.map((definition) => {
			const allowance = allowancesByDefinitionId.get(definition.id)!;

			return {
				consumed: allowance.entitlementIds.reduce(
					(total, id) =>
						total + (consumedByEntitlementId.get(id) ?? 0),
					0
				),
				included: allowance.included,
				period: definition.period ?? '',
				unit: definition.unit ?? '',
				unlimited: allowance.unlimited,
			};
		});

	return {error, loading, usage};
}

export default useProjectUsage;
