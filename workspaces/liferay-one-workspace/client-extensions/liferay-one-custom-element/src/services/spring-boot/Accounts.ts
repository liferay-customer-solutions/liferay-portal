/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {OneSpringBootOAuth2} from './OAuth2Client';

type InvitationBody = {
	emailAddress: string;
	familyName: string;
	givenName: string;
	roleNames: string[];
};

class AccountsOAuth2 extends OneSpringBootOAuth2 {
	async postInvitations(
		accountExternalReferenceCode: string,
		body: InvitationBody
	) {
		return this.post(`/${accountExternalReferenceCode}/invitations`, body);
	}

	async postSyncToJSM(accountExternalReferenceCode: string) {
		return this.post(`/${accountExternalReferenceCode}/sync-to-jsm`);
	}
}

const Accounts = new AccountsOAuth2('/accounts');

export default Accounts;
