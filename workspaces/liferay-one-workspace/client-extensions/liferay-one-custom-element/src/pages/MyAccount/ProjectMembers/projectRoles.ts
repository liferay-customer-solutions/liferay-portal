/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {
	CRITICAL_INCIDENT_CONTACT,
	PAAS_USER,
	PRIVACY_BREACH_CONTACT,
	SECURITY_BREACH_CONTACT,
} from '~/pages/MyAccount/AccountMembers/accountRoles';

export const PROJECT_ADMIN_ERC = 'C_PROJECT_ADMIN';
export const PROJECT_REQUESTER_ERC = 'C_PROJECT_REQUESTER';
export const PROJECT_USER_ERC = 'C_PROJECT_USER';

export const PROJECT_ROLE_ERCS = [
	PROJECT_ADMIN_ERC,
	PROJECT_REQUESTER_ERC,
	PROJECT_USER_ERC,
];

export const PROJECT_ROLE_LABELS: Record<string, string> = {
	[PROJECT_ADMIN_ERC]: 'Admin',
	[PROJECT_REQUESTER_ERC]: 'Requester',
	[PROJECT_USER_ERC]: 'User',
};

export function getProjectRoleLabel(roleExternalReferenceCode: string) {
	return PROJECT_ROLE_LABELS[roleExternalReferenceCode] ?? '';
}

const DESIGNATIONS_BY_PRODUCT_TYPE: Record<string, string[]> = {
	CLOUD_APP: [
		CRITICAL_INCIDENT_CONTACT,
		PRIVACY_BREACH_CONTACT,
		SECURITY_BREACH_CONTACT,
	],
	DXP_APP: [CRITICAL_INCIDENT_CONTACT],
	SSA_SAAS: [
		CRITICAL_INCIDENT_CONTACT,
		PRIVACY_BREACH_CONTACT,
		SECURITY_BREACH_CONTACT,
	],
};

const ALL_DESIGNATIONS = [
	CRITICAL_INCIDENT_CONTACT,
	PAAS_USER,
	PRIVACY_BREACH_CONTACT,
	SECURITY_BREACH_CONTACT,
];

export function getAvailableDesignations(
	productTypeExternalReferenceCodes: string[]
) {
	const designations = new Set<string>();

	productTypeExternalReferenceCodes.forEach((productType) => {
		(DESIGNATIONS_BY_PRODUCT_TYPE[productType] ?? []).forEach(
			(designation) => designations.add(designation)
		);
	});

	return ALL_DESIGNATIONS.filter((designation) =>
		designations.has(designation)
	);
}
