/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayIcon from '@clayui/icon';
import ClayTabs from '@clayui/tabs';
import {ReactNode} from 'react';
import {Link, useLocation, useSearchParams} from 'react-router-dom';
import i18n, {Word} from '~/i18n';

export type DetailTab = {
	content: ReactNode;
	key: string;
	label: Word;
};

type ProjectDetailTabsProps = {
	header?: ReactNode;
	tabs: DetailTab[];
};

export default function ProjectDetailTabs({
	header,
	tabs,
}: ProjectDetailTabsProps) {
	const {pathname} = useLocation();
	const [searchParams, setSearchParams] = useSearchParams();

	const backTo = pathname.split('/').slice(0, -1).join('/');

	const activeTabKey = searchParams.get('tab') ?? tabs[0]?.key;
	const activeTabIndex = Math.max(
		0,
		tabs.findIndex((tab) => tab.key === activeTabKey)
	);

	function handleTabChange(index: number) {
		setSearchParams((previousSearchParams) => {
			const nextSearchParams = new URLSearchParams(previousSearchParams);

			nextSearchParams.set('tab', tabs[index].key);

			return nextSearchParams;
		});
	}

	return (
		<div className="w-100">
			<Link
				className="align-items-center d-inline-flex gap-2 mb-4 text-decoration-none"
				to={backTo}
			>
				<ClayIcon symbol="order-arrow-left" />

				{i18n.translate('back-to-project')}
			</Link>

			{header}

			<ClayTabs
				active={activeTabIndex}
				className="mb-4"
				onActiveChange={handleTabChange}
			>
				{tabs.map((tab) => (
					<ClayTabs.Item key={tab.key}>
						{i18n.translate(tab.label)}
					</ClayTabs.Item>
				))}
			</ClayTabs>

			{tabs[activeTabIndex]?.content}
		</div>
	);
}
