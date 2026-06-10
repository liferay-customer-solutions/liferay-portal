/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ReactNode, Suspense} from 'react';
import {Outlet} from 'react-router-dom';

import SideNav, {NavItem} from './SideNav';

type AppLayoutProps = {
	breadcrumb?: ReactNode;
	contentHeader?: ReactNode;
	header?: ReactNode;
	navItems: NavItem[];
	title?: string;
};

export default function AppLayout({
	breadcrumb,
	contentHeader,
	header,
	navItems,
	title,
}: AppLayoutProps) {
	return (
		<div
			style={{
				paddingBottom: 'var(--spacer-4)',
				paddingTop: 'var(--spacer-4)',
			}}
		>
			{breadcrumb}

			<div className="d-flex" style={{gap: 'var(--spacer-3)'}}>
				<SideNav header={header} items={navItems} title={title} />

				<main className="flex-fill overflow-auto">
					{contentHeader}

					<Suspense fallback={null}>
						<Outlet />
					</Suspense>
				</main>
			</div>
		</div>
	);
}
