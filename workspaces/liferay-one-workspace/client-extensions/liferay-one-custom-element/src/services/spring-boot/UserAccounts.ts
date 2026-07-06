/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {OneSpringBootOAuth2} from './OAuth2Client';

type AssignmentProject = {
	projectExternalReferenceCode: string;
	roleExternalReferenceCode: string;
};

type AssignmentPayload = {
	accountId: number | string;
	accountRoleId?: number | null;
	projects?: AssignmentProject[];
	userId: number | string;
};

class UserAccountsOAuth2 extends OneSpringBootOAuth2 {
	async postAssignments(payload: AssignmentPayload) {
		return this.post('/assignments', payload);
	}
}

const UserAccounts = new UserAccountsOAuth2('/user-accounts');

export default UserAccounts;
