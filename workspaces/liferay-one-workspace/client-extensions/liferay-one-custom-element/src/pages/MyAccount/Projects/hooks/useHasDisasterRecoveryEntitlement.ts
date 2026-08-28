/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import useSWR from 'swr';
import Cloud from '~/services/spring-boot/Cloud';

export function useHasDisasterRecoveryEntitlement(
	projectExternalReferenceCode: string
): {
	hasDisasterRecoveryEntitlement: boolean;
	loading: boolean;
} {
	const {data, isLoading} = useSWR(
		projectExternalReferenceCode
			? `/cloud/projects/${projectExternalReferenceCode}/entitlements` +
					'/disaster-recovery'
			: null,
		() =>
			Cloud.getProjectsEntitlementsDisasterRecovery(
				projectExternalReferenceCode
			)
	);

	return {
		hasDisasterRecoveryEntitlement:
			data?.hasDisasterRecoveryEntitlement ?? false,
		loading: isLoading,
	};
}

export default useHasDisasterRecoveryEntitlement;
