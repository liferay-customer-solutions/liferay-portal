/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useCallback, useEffect, useState} from 'react';
import {Liferay} from '~/services/liferay';

interface IHasJiraAccount {
	hasJiraAccount: boolean;
}

const useHasJiraAccount = (externalReferenceCode?: string, skip?: boolean) => {
	const [hasAccount, setHasAccount] = useState<boolean | undefined>(
		undefined
	);
	const [loading, setLoading] = useState<boolean>(false);

	const fetchHasJiraAccount = useCallback(async () => {
		if (skip || !externalReferenceCode) {
			return;
		}

		setLoading(true);

		try {
			const data: IHasJiraAccount =
				await Liferay.OAuth2Client.FromUserAgentApplication(
					'liferay-customer-etc-spring-boot-oaua'
				)
					.fetch(
						`/accounts/${externalReferenceCode}/has-jira-account`
					)
					.then((response: Response) => response.json());

			setHasAccount(data.hasJiraAccount);
		}
		catch (error) {
			console.error('Error fetching Jira account:', error);
			setHasAccount(undefined);
		}
		finally {
			setLoading(false);
		}
	}, [externalReferenceCode, skip]);

	useEffect(() => {
		fetchHasJiraAccount();
	}, [fetchHasJiraAccount]);

	return {hasAccount, loading};
};

export default useHasJiraAccount;
