/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import Button from '../../../components/Button/Button';
import {Word, translate} from '../../../i18n';
import FilterableListCard, {ListColumn} from './FilterableListCard';
import {DownloadItem} from './tabData';

type DownloadListCardProps = {
	emptyLabel: Word;
	heading: Word;
	items: DownloadItem[];
	title: Word;
};

function matchesSearch(item: DownloadItem, search: string): boolean {
	return item.name.toLowerCase().includes(search);
}

export default function DownloadListCard({
	emptyLabel,
	heading,
	items,
	title,
}: DownloadListCardProps) {
	const columns: ListColumn<DownloadItem>[] = [
		{
			heading,
			key: 'name',
			render: (item) => (
				<span style={{fontWeight: 600}}>{item.name}</span>
			),
		},
		{
			key: 'download',
			render: () => (
				<Button
					displayType="secondary"
					onClick={(event) => event.stopPropagation()}
				>
					{translate('download')}
				</Button>
			),
		},
	];

	return (
		<FilterableListCard
			columns={columns}
			emptyLabel={emptyLabel}
			filters={[]}
			items={items}
			matchesSearch={matchesSearch}
			onItemClick={() => {}}
			rowKey={(item) => item.id}
			title={title}
		/>
	);
}
