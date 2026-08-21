/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import useSWR from 'swr';
import FetcherError from '~/services/fetcher/FetcherError';
import Projects from '~/services/spring-boot/Projects';

import {isUnassignedProject} from '../utils/isUnassignedProject';

export type EventDataSource = {
	dataSourceId: string;
	dataSourceName: string;
	eventsCount: number;
};

export type ProjectEventUsage = {
	addOnBucketCount?: number;
	baseAllotment?: number;
	eventSummary: EventDataSource[];
	maxCount?: number;
	usedCount?: number;
};

export function useProjectEventUsage(
	projectExternalReferenceCode: string,
	startDate?: string,
	endDate?: string
): {
	error?: FetcherError;
	eventUsage?: ProjectEventUsage;
	isLoading: boolean;
} {
	const skip =
		!endDate ||
		!startDate ||
		!projectExternalReferenceCode ||
		isUnassignedProject(projectExternalReferenceCode);

	const {data, error, isLoading} = useSWR<ProjectEventUsage>(
		skip
			? null
			: `/projects/${projectExternalReferenceCode}/usage/event-summary?startDate=${startDate}&endDate=${endDate}`,
		() =>
			Projects.getProjectEventUsage(
				endDate as string,
				projectExternalReferenceCode,
				startDate as string
			) as Promise<ProjectEventUsage>
	);

	return {error, eventUsage: data, isLoading};
}
