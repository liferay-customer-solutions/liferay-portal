/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import useSWR from 'swr';
import FetcherError from '~/services/fetcher/FetcherError';
import Projects from '~/services/spring-boot/Projects';

import {isUnassignedProject} from '../utils/isUnassignedProject';

export type UsageMetric = {
	maxCount: number;
	maxCountUnits?: string;
	percentage: string;
	usedCount: number;
	usedCountUnits?: string;
};

export type UsageDashboard = {
	metrics: {[key: string]: UsageMetric};
};

export function useProjectUsageDashboard(
	productExternalReferenceCode: string,
	projectExternalReferenceCode: string
): {error?: FetcherError; isLoading: boolean; usageDashboard?: UsageDashboard} {
	const skip =
		!projectExternalReferenceCode ||
		isUnassignedProject(projectExternalReferenceCode);

	const {data, error, isLoading} = useSWR<UsageDashboard>(
		skip
			? null
			: `/projects/${projectExternalReferenceCode}/usage?productExternalReferenceCode=${productExternalReferenceCode}`,
		() =>
			Projects.getProjectUsage(
				productExternalReferenceCode,
				projectExternalReferenceCode
			) as Promise<UsageDashboard>
	);

	return {error, isLoading, usageDashboard: data};
}
