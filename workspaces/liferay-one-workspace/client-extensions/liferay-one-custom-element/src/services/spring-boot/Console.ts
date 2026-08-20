/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {OneSpringBootOAuth2} from './OAuth2Client';

export type ConsoleProjectsUsage = {
	userEmail: string;
	userProjects: ConsoleUserProject[];
};

export type ConsoleResourceUsage = {
	free: number;
	limit: number;
	used: number;
};

export type ConsoleUserProject = {
	environments: {isExtensionEnvironment: boolean; projectId: string}[];
	rootProjectId: string;
	rootProjectPlanUsage: {
		cpu: ConsoleResourceUsage;
		instance: ConsoleResourceUsage;
		memory: ConsoleResourceUsage;
	};
};

class ConsoleOAuth2 extends OneSpringBootOAuth2 {
	async getProjectsUsage() {
		return this.get<ConsoleProjectsUsage>('/projects-usage');
	}

	async provisioning(
		orderId: number,
		data: {orderItemId: number; projectId: string}
	): Promise<void> {
		await this.post(`/provisioning/${orderId}`, data);
	}

	async uninstallApp(
		orderId: number,
		data: {id: number | string; orderItemId: number}
	): Promise<void> {
		await this.post(`/uninstall-app/${orderId}`, data);
	}
}

const Console = new ConsoleOAuth2('/console');

export default Console;
