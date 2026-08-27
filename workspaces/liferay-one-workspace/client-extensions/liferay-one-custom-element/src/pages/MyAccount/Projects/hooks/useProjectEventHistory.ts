/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import useSWR from 'swr';
import FetcherError from '~/services/fetcher/FetcherError';
import Projects from '~/services/spring-boot/Projects';

import {isUnassignedProject} from '../utils/isUnassignedProject';

import type {EventDataSource} from './useProjectEventUsage';

export type EventHistoryGranularity = 'day' | 'month';

export type EventHistoryPoint = {
	date: string;
	eventSummary?: EventDataSource[];
};

export type ProjectEventHistory = {
	eventHistory: EventHistoryPoint[];
	usageDataAvailable?: boolean;
};

type UseProjectEventHistoryOptions = {
	endDate?: string;
	granularity: EventHistoryGranularity;
	projectExternalReferenceCode: string;
	startDate?: string;
};

export function useProjectEventHistory({
	endDate,
	granularity,
	projectExternalReferenceCode,
	startDate,
}: UseProjectEventHistoryOptions): {
	error?: FetcherError;
	eventHistory?: ProjectEventHistory;
	isLoading: boolean;
} {
	const skip =
		!endDate ||
		!startDate ||
		!projectExternalReferenceCode ||
		isUnassignedProject(projectExternalReferenceCode);

	const {data, error, isLoading} = useSWR<ProjectEventHistory>(
		skip
			? null
			: `/projects/${projectExternalReferenceCode}/usage/event-history?startDate=${startDate}&endDate=${endDate}&granularity=${granularity}`,
		() =>
			Projects.getProjectEventHistory(
				endDate as string,
				granularity,
				projectExternalReferenceCode,
				startDate as string
			) as Promise<ProjectEventHistory>
	);

	return {error, eventHistory: data, isLoading};
}
