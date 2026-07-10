/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useFetch} from '~/hooks/useFetch';
import {useProjectEnvironments} from '~/hooks/useProjectEnvironments';

import type {APIResponse} from '~/types/api';

export type ProjectUsage = {
	consumed: number;
	included: number;
	period: string;
	unit: string;
};

type UsageDefinitionNode = {
	id: number;
	period?: string;
	quantity?: number;
	unit?: string;
};

type UsageEventNode = {
	quantity?: number;
	r_usageDefinitionToUsageEvent_c_usageDefinitionId?: number;
};

export function useProjectUsage() {
	const {environments} = useProjectEnvironments();

	const environmentIds = environments.map((environment) => environment.id);

	const eventFilter = environmentIds
		.map((id) => `r_environmentToUsageEvent_c_environmentId eq '${id}'`)
		.join(' or ');

	const {data: definitionsData} = useFetch<APIResponse<UsageDefinitionNode>>(
		'/o/c/usagedefinitions',
		{params: {pageSize: 200}}
	);

	const {
		data: eventsData,
		error,
		isLoading: loading,
	} = useFetch<APIResponse<UsageEventNode>>(
		environmentIds.length ? '/o/c/usageevents' : null,
		{params: {filter: eventFilter, pageSize: 500}}
	);

	const consumedByDefinitionId = new Map<number, number>();

	for (const event of eventsData?.items ?? []) {
		const definitionId =
			event.r_usageDefinitionToUsageEvent_c_usageDefinitionId;

		if (definitionId === undefined) {
			continue;
		}

		consumedByDefinitionId.set(
			definitionId,
			(consumedByDefinitionId.get(definitionId) ?? 0) +
				(event.quantity ?? 0)
		);
	}

	const usage: ProjectUsage[] = (definitionsData?.items ?? [])
		.filter((definition) => consumedByDefinitionId.has(definition.id))
		.map((definition) => ({
			consumed: consumedByDefinitionId.get(definition.id) ?? 0,
			included: definition.quantity ?? 0,
			period: definition.period ?? '',
			unit: definition.unit ?? '',
		}));

	return {error, loading, usage};
}

export default useProjectUsage;
