/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import Navbar from '~/components/AppPublishNavbar/AppPublishNavbar';
import Sidebar from '~/components/AppPublishSidebar/AppPublishSidebar';

type PropsWithChildren = {
	children?: any;
};

const Body: React.FC<PropsWithChildren> = ({children}) => (
	<div className="d-flex mt-5 w-100">{children}</div>
);

const Content: React.FC<PropsWithChildren> = ({children}) => (
	<div className="flex-grow-1 ml-5 new-app-body-container">{children}</div>
);

const AppPublish: React.FC<PropsWithChildren> & {
	Body: typeof Body;
	Content: typeof Content;
	Navbar: typeof Navbar;
	Sidebar: typeof Sidebar;
} = ({children}) => <div className="publish-app w-100">{children}</div>;

AppPublish.Body = Body;
AppPublish.Content = Content;
AppPublish.Navbar = Navbar;
AppPublish.Sidebar = Sidebar;

export default AppPublish;
