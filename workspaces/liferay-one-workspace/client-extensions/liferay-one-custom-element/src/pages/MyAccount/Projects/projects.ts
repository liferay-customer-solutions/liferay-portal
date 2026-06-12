/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

export type Project = {
	creditLimit: string;
	creditLimitPercent: number;
	id: string;
	name: string;
	spendingLimit: string;
	spendingLimitPercent: number;
	status: string;
	termRange: string;
	termType: string;
};

export const PROJECTS: Project[] = [
	{
		creditLimit: '$1K / $3.5K',
		creditLimitPercent: 34,
		id: 'default',
		name: 'North America Enterprise',
		spendingLimit: '$1.7K / $2.5K',
		spendingLimitPercent: 70,
		status: 'active',
		termRange: '08.09.2026 - 01.09.2027',
		termType: 'annual',
	},
	{
		creditLimit: '$0.5K / $2K',
		creditLimitPercent: 25,
		id: 'sandbox',
		name: 'Sandbox',
		spendingLimit: '$0.3K / $1K',
		spendingLimitPercent: 30,
		status: 'active',
		termRange: '01.01.2026 - 31.12.2026',
		termType: 'annual',
	},
	{
		creditLimit: '$0.8K / $2K',
		creditLimitPercent: 40,
		id: 'staging',
		name: 'Staging',
		spendingLimit: '$1K / $2K',
		spendingLimitPercent: 50,
		status: 'active',
		termRange: '01.03.2026 - 28.02.2027',
		termType: 'annual',
	},
];

export const DEFAULT_PROJECT_ID = PROJECTS[0].id;

export const LAST_PROJECT_STORAGE_KEY = 'liferay-one:last-project';

export function resolveProjectId(id?: string): string {
	if (id) {
		return id;
	}

	return localStorage.getItem(LAST_PROJECT_STORAGE_KEY) ?? DEFAULT_PROJECT_ID;
}

export function getProject(id: string): Project | undefined {
	return PROJECTS.find((project) => project.id === id);
}

export function getProjectName(id: string): string {
	return getProject(id)?.name ?? id;
}
