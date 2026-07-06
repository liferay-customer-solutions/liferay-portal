/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

export type ProjectMember = {
	designations: string[];
	email: string;
	membershipId: number;
	name: string;
	roleExternalReferenceCode: string;
	userId: number;
};

export type ProjectMembersRow = {
	externalReferenceCode: string;
	hasProjectAdmin: boolean;
	id: number;
	members: ProjectMember[];
	name: string;
};

export type AccountMemberOption = {
	email: string;
	name: string;
	userId: number;
};
