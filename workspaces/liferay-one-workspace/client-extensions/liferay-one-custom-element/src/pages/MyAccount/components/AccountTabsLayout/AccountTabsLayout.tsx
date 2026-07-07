/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayTabs from '@clayui/tabs';
import {Suspense} from 'react';
import {Outlet, useLocation, useNavigate, useParams} from 'react-router-dom';
import {Header} from '~/components/Header/Header';
import {useOneContext} from '~/context/OneContextProvider';
import i18n, {Word} from '~/i18n';
import {canAccessAccountMembers} from '~/pages/MyAccount/AccountMembers/accountRoles';

type AccountTab = {
	label: Word;
	path: string;
};

const TABS: AccountTab[] = [
	{label: 'details', path: 'account-details'},
	{label: 'members', path: 'account-members'},
	{label: 'project-permissions', path: 'project-members'},
];

export default function AccountTabsLayout() {
	const {accountERC} = useParams();
	const {pathname} = useLocation();
	const navigate = useNavigate();

	const {userAccountModel} = useOneContext();

	const visibleTabs = TABS.filter(
		(tab) =>
			tab.path !== 'account-members' ||
			canAccessAccountMembers(userAccountModel)
	);

	const showTabs = visibleTabs.some((tab) =>
		pathname.endsWith(`/${tab.path}`)
	);

	const activeTabIndex = Math.max(
		0,
		visibleTabs.findIndex((tab) => pathname.includes(`/${tab.path}`))
	);

	return (
		<div className="w-100">
			{showTabs && (
				<>
					<Header title={i18n.translate('account')} />

					<ClayTabs
						active={activeTabIndex}
						className="mb-4"
						onActiveChange={(index) =>
							navigate(
								`/${accountERC}/${visibleTabs[index].path}`
							)
						}
					>
						{visibleTabs.map((tab) => (
							<ClayTabs.Item key={tab.path}>
								{i18n.translate(tab.label)}
							</ClayTabs.Item>
						))}
					</ClayTabs>
				</>
			)}

			<Suspense fallback={null}>
				<Outlet />
			</Suspense>
		</div>
	);
}
