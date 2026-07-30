/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import type {ProjectEnvironment} from '~/hooks/useProjectEnvironments';

export function filterEnvironmentsByContract(
	contractId: number | undefined,
	environments: ProjectEnvironment[]
): ProjectEnvironment[] {
	if (!contractId) {
		return environments;
	}

	return environments.filter(
		(environment) => environment.contractId === contractId
	);
}

export default filterEnvironmentsByContract;
