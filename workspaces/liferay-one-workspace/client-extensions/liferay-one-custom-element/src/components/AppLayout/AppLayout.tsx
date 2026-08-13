/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayLoadingIndicator from '@clayui/loading-indicator';
import {ReactNode, Suspense} from 'react';
import {Outlet} from 'react-router-dom';
import SideNav, {NavItem} from '~/components/SideNav/SideNav';

import '~/components/SideNav/SideNav.css';

type AppLayoutProps = {
	breadcrumb?: ReactNode;
	contentHeader?: ReactNode;
	header?: ReactNode;
	headerBackground?: boolean;
	navItems: NavItem[];
	title?: string;
};

export default function AppLayout({
	breadcrumb,
	contentHeader,
	header,
	headerBackground,
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

			<div className="side-nav-layout">
				<SideNav
					header={header}
					headerBackground={headerBackground}
					items={navItems}
					title={title}
				/>

				<main className="flex-fill overflow-auto">
					{contentHeader}

					<Suspense fallback={<ClayLoadingIndicator />}>
						<Outlet />
					</Suspense>
				</main>
			</div>
		</div>
	);
}
