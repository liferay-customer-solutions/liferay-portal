/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayLoadingIndicator from '@clayui/loading-indicator';
import {Navigate, useLocation} from 'react-router-dom';
import {useFetch} from '~/hooks/useFetch';
import {
	resolveDefaultProject,
	useUserProjects,
} from '~/pages/MyAccount/Projects/projects';
import {Liferay} from '~/services/liferay/liferay';

import type {Account} from '~/types/accounts';

export default function ProjectRedirect() {
	const currentAccountId = Liferay.CommerceContext.account?.accountId;

	const {pathname} = useLocation();

	const {data: account, isLoading: accountLoading} = useFetch<Account>(
		currentAccountId
			? `/o/headless-admin-user/v1.0/accounts/${currentAccountId}`
			: null
	);

	const {loading: projectsLoading, projects} = useUserProjects();

	if (account && !projectsLoading) {
		const accountERC = account.externalReferenceCode;

		const target = resolveDefaultProject(projects);

		if (!target) {
			return <Navigate replace to={`/${accountERC}/project`} />;
		}

		const tab = pathname.replace(/^\/project\/?/, '');

		return (
			<Navigate
				replace
				to={`/${accountERC}/project/${target.externalReferenceCode}/${
					tab || 'products'
				}`}
			/>
		);
	}

	if (!currentAccountId || (!accountLoading && !projectsLoading)) {
		return null;
	}

	return (
		<div className="mx-auto p-4">
			<ClayLoadingIndicator size="sm" />
		</div>
	);
}
