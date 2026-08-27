/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {OneSpringBootOAuth2} from './OAuth2Client';

class ProjectsOAuth2 extends OneSpringBootOAuth2 {
	async getProjectEventHistory(
		endDate: string,
		granularity: string,
		projectExternalReferenceCode: string,
		startDate: string
	) {
		const searchParams = new URLSearchParams({
			endDate,
			granularity,
			startDate,
		});

		return this.get(
			`/${projectExternalReferenceCode}/usage/event-history` +
				`?${searchParams.toString()}`
		);
	}

	async getProjectEventUsage(
		endDate: string,
		projectExternalReferenceCode: string,
		startDate: string
	) {
		const searchParams = new URLSearchParams({endDate, startDate});

		return this.get(
			`/${projectExternalReferenceCode}/usage/event-summary` +
				`?${searchParams.toString()}`
		);
	}

	async getProjectUsage(
		productExternalReferenceCode: string,
		projectExternalReferenceCode: string
	) {
		const searchParams = new URLSearchParams({
			productExternalReferenceCode,
		});

		return this.get(
			`/${projectExternalReferenceCode}/usage?${searchParams.toString()}`
		);
	}

	async postProjectMembership(
		projectExternalReferenceCode: string,
		userId: number | string,
		roleExternalReferenceCode: string
	) {
		return this.post(
			`/${projectExternalReferenceCode}/user-accounts/${userId}` +
				`/account-roles/${roleExternalReferenceCode}`
		);
	}
}

const Projects = new ProjectsOAuth2('/projects');

export default Projects;
