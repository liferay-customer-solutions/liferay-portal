/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import useSWR from 'swr';
import TrialExtensionRequests from '~/services/objects/TrialExtensionRequests';
import SearchBuilder from '~/utils/SearchBuilder';

import type {Account} from '~/types/accounts';

const useSSATrialsExtend = (account: Account) =>
	useSWR(account?.id ? '/o/c/trialextensionrequests' : null, () =>
		TrialExtensionRequests.getTrialExtensionRequest(
			new URLSearchParams({
				filter: SearchBuilder.eq(
					'r_accountEntryToTrialExtensionRequest_accountEntryId',
					account.id
				),
				page: '1',
				pageSize: '-1',
				sort: 'dateCreated:desc',
			})
		)
	);

export {useSSATrialsExtend};
