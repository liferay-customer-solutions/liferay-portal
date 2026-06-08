/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

export type Project = {
	id: string;
	name: string;
	status: string;
	termRange: string;
	termType: string;
};

export const PROJECTS: Project[] = [
	{
		id: 'default',
		name: 'North America Enterprise',
		status: 'active',
		termRange: '08.09.2026 - 01.09.2027',
		termType: 'annual',
	},
	{
		id: 'sandbox',
		name: 'Sandbox',
		status: 'active',
		termRange: '01.01.2026 - 31.12.2026',
		termType: 'annual',
	},
	{
		id: 'staging',
		name: 'Staging',
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

	return (
		localStorage.getItem(LAST_PROJECT_STORAGE_KEY) ?? DEFAULT_PROJECT_ID
	);
}

export function getProject(id: string): Project | undefined {
	return PROJECTS.find((project) => project.id === id);
}

export function getProjectName(id: string): string {
	return getProject(id)?.name ?? id;
}
