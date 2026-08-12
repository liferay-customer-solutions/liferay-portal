/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import type {AppFlowItem} from '~/pages/PublisherDashboard/pages/NewAppFlow/constants';
import ClayIcon from '@clayui/icon';
import classNames from 'classnames';

import './AppPublishSidebar.scss';

type AppPublishSidebar = {
	activeIndex: number;
	items: AppFlowItem[];
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

const AppPublishSidebar: React.FC<AppPublishSidebar> = ({activeIndex, items}) => (
	<ul className="app-flow-list-container app-flow-list-ul mt-0 pt-5">
		{items.map(({hide, label}, index) => {
			if (hide) {
				return null;
			}

			const checked = index < activeIndex;
			const selected = activeIndex === index;

			return (
				<div className="app-flow-list-item-container" key={index}>
					<ClayIcon
						aria-label={selected ? 'radio selected' : 'circle fill'}
						className={classNames(
							'app-flow-list-item-icon text-muted',
							{
								'app-flow-list-item-icon-checked': checked,
								'app-flow-list-item-icon-selected': selected,
							}
						)}
						symbol={getIcon({checked, selected})}
					/>

					<li
						className={classNames('app-flow-list-item-text', {
							'app-flow-list-item-text-checked':
								checked || selected,
						})}
					>
						{label}
					</li>
				</div>
			);
		})}
	</ul>
);

export default AppPublishSidebar;
