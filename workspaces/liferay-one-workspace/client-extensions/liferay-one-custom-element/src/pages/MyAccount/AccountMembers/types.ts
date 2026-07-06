/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import type {RoleBrief} from '~/types/accounts';

export type AccountMemberRow = {
	email: string;
	id: number;
	image?: string;
	isAdministrator: boolean;
	isCurrentUser: boolean;
	name: string;
	roleBriefs: RoleBrief[];
	roleNames: string[];
	status: number;
};
