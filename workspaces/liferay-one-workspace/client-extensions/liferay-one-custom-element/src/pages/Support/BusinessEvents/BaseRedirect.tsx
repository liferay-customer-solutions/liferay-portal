/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayLoadingIndicator from '@clayui/loading-indicator';
import {useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';

import {translate} from '../../../i18n';
import {Liferay} from '../../../liferay/liferay';
import HeadlessAdminUser from '../../../services/rest/HeadlessAdminUser';
import {
	getLastViewedProjectCookie,
	setLastViewedProjectCookie,
} from './utils/projectCookie';

const BaseRedirect = () => {
	const navigate = useNavigate();

	const [loading, setLoading] = useState(true);
	const [noAccounts, setNoAccounts] = useState(false);

	useEffect(() => {
		const userId = Liferay.ThemeDisplay.getUserId();
		const lastViewedERC = getLastViewedProjectCookie(userId);

		if (lastViewedERC) {
			navigate(
				`/${decodeURIComponent(lastViewedERC)}/business-events`,
				{replace: true}
			);

			return;
		}

		HeadlessAdminUser.getMyUserAccount()
			.then((userAccount) => {
				const firstAccount = userAccount?.accountBriefs?.[0];

				if (firstAccount) {
					setLastViewedProjectCookie(
						userId,
						firstAccount.externalReferenceCode
					);

					navigate(
						`/${firstAccount.externalReferenceCode}/business-events`,
						{replace: true}
					);
				}
				else {
					setNoAccounts(true);
					setLoading(false);
				}
			})
			.catch(() => {
				setNoAccounts(true);
				setLoading(false);
			});
	}, [navigate]);

	if (loading && !noAccounts) {
		return (
			<div className="mx-auto">
				<ClayLoadingIndicator size="sm" />
			</div>
		);
	}

	return (
		<div className="p-4">
			<p>
				{translate(
					'login-as-a-user-that-has-access-to-a-project-or-contact-your-project-administrator-to-add-you-to-a-project.'
				)}
			</p>
		</div>
	);
};

export default BaseRedirect;
