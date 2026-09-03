/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useProject} from '~/context/ProjectContext';
import {useFetch} from '~/hooks/useFetch';
import {isUnassignedProject} from '~/pages/MyAccount/Projects/utils/isUnassignedProject';

import type {APIResponse} from '~/types/api';

const GRANT_TYPE_UNLIMITED = 'unlimited';

const PERIOD_MONTHLY = 'per month';

export type ProjectUsage = {
	consumed: number;
	included: number;
	period: string;
	unit: string;
	unlimited: boolean;
};

type EntitlementDefinitionNode = {
	r_usageDefinitionToEntitlementDefinition_c_usageDefinitionId?: number;
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
	eventTimestamp?: string;
	quantity?: number;
	r_entitlementToUsageEvent_c_entitlementId?: number;
};

type Allowance = {
	entitlementIds: number[];
	included: number;
	unlimited: boolean;
};

function totalOf(events: UsageEventNode[]): number {
	return events.reduce((total, event) => total + (event.quantity ?? 0), 0);
}

function totalOfLatestMonth(events: UsageEventNode[]): number {
	const totalsByMonth = new Map<string, number>();

	for (const event of events) {
		const month = event.eventTimestamp?.slice(0, 7);

		if (!month) {
			continue;
		}

		totalsByMonth.set(
			month,
			(totalsByMonth.get(month) ?? 0) + (event.quantity ?? 0)
		);
	}

	const latest = Array.from(totalsByMonth.keys()).sort().pop();

	return latest === undefined ? 0 : totalsByMonth.get(latest) ?? 0;
}

export function useProjectUsage() {
	const {projectId} = useProject();

	const projectExternalReferenceCode =
		projectId && !isUnassignedProject(projectId) ? projectId : undefined;

	const {data: entitlementsData} = useFetch<APIResponse<EntitlementNode>>(
		projectExternalReferenceCode ? '/o/c/entitlements' : null,
		{
			params: {
				filter: `r_projectToEntitlement_c_projectERC eq '${projectExternalReferenceCode}'`,
				nestedFields: 'entitlementDefinition',
				pageSize: 200,
			},
		}
	);

	const allowancesByDefinitionId = new Map<number, Allowance>();

	for (const entitlement of entitlementsData?.items ?? []) {
		const usageDefinitionId =
			entitlement
				.r_entitlementDefinitionToEntitlement_c_entitlementDefinition
				?.r_usageDefinitionToEntitlementDefinition_c_usageDefinitionId;

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

	const eventsByEntitlementId = new Map<number, UsageEventNode[]>();

	for (const event of eventsData?.items ?? []) {
		const entitlementId = event.r_entitlementToUsageEvent_c_entitlementId;

		if (entitlementId === undefined) {
			continue;
		}

		const events = eventsByEntitlementId.get(entitlementId) ?? [];

		events.push(event);

		eventsByEntitlementId.set(entitlementId, events);
	}

	const usage: ProjectUsage[] = (definitionsData?.items ?? [])
		.filter((definition) => allowancesByDefinitionId.has(definition.id))
		.map((definition) => {
			const allowance = allowancesByDefinitionId.get(definition.id)!;

			const events = allowance.entitlementIds.flatMap(
				(id) => eventsByEntitlementId.get(id) ?? []
			);

			return {
				consumed:
					definition.period === PERIOD_MONTHLY
						? totalOfLatestMonth(events)
						: totalOf(events),
				included: allowance.included,
				period: definition.period ?? '',
				unit: definition.unit ?? '',
				unlimited: allowance.unlimited,
			};
		});

	return {error, loading, usage};
}

export default useProjectUsage;
