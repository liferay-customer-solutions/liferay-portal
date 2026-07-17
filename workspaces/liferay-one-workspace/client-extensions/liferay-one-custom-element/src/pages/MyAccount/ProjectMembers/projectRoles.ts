/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import type {DeliveryProductSpecification} from '~/types/product';

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

const CONTACTS_ROLE_ERCS_SPECIFICATION_KEY = 'project-contacts-role-ercs';

export function getProductContactRoleExternalReferenceCodes(
	specifications: DeliveryProductSpecification[]
): string[] {
	const value =
		specifications.find(
			(specification) =>
				specification.specificationKey ===
				CONTACTS_ROLE_ERCS_SPECIFICATION_KEY
		)?.value ?? '';

	return value
		.split(',')
		.map((externalReferenceCode) => externalReferenceCode.trim())
		.filter(Boolean);
}
