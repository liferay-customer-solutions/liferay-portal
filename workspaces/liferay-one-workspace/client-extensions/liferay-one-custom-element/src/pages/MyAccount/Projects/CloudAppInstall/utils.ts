/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import i18n from '~/i18n';
import {convertSize} from '~/utils/fileUtils';

import type {ConsoleUserProject} from '~/services/spring-boot/Console';

export function getResourceSummary(project: ConsoleUserProject) {
	return i18n.sub('x-environments-x-cpus-x-gb-ram', [
		String(project.environments.length),
		String(project.rootProjectPlanUsage.cpu.free),
		String(
			convertSize('MB', 'GB', project.rootProjectPlanUsage.memory.free)
		),
	]);
}
