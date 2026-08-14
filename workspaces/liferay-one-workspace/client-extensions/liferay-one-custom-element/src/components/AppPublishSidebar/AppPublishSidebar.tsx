/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayIcon from '@clayui/icon';
import classNames from 'classnames';
import {Link} from 'react-router-dom';

import '~/components/SideNav/SideNav.css';

import './AppPublishSidebar.css';

import type {AppFlowItem} from '~/pages/PublisherDashboard/pages/NewAppFlow/constants';

type AppPublishSidebar = {
	activeIndex: number;
	items: Pick<AppFlowItem, 'hide' | 'label' | 'path'>[];
	navigable?: boolean;
};

const getIcon = ({
	checked,
	selected,
}: {
	checked: boolean;
	selected: boolean;
}) => {
	if (checked) {
		return 'check';
	}

	if (selected) {
		return 'radio-button';
	}

	return 'circle';
};

const AppPublishSidebar: React.FC<AppPublishSidebar> = ({
	activeIndex,
	items,
	navigable = false,
}) => (
	<nav className="side-nav">
		<div className="side-nav-panel">
			<ul className="side-nav-list">
				{items.map(({hide, label, path}, index) => {
					if (hide) {
						return null;
					}

					const checked = index < activeIndex;
					const selected = activeIndex === index;

					const className = classNames('side-nav-link', {
						'side-nav-link-active': selected,
						'side-nav-link-complete': checked,
					});

					const content = (
						<>
							<ClayIcon
								aria-label={
									selected ? 'radio selected' : 'circle fill'
								}
								className="app-flow-step-icon side-nav-icon"
								symbol={getIcon({checked, selected})}
							/>

							<span className="side-nav-label">{label}</span>
						</>
					);

					return (
						<li className="side-nav-item" key={index}>
							{navigable ? (
								<Link className={className} to={path}>
									{content}
								</Link>
							) : (
								<span className={className}>{content}</span>
							)}
						</li>
					);
				})}
			</ul>
		</div>
	</nav>
);

export default AppPublishSidebar;
