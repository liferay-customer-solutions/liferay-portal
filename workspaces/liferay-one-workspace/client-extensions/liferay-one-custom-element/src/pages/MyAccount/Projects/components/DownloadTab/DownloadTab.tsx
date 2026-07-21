/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useLiferayBundles} from '~/hooks/useLiferayBundles';

import DownloadListCard, {
	DownloadItem,
} from '../DownloadListCard/DownloadListCard';

import type {VirtualItem} from '~/types/orders';

import type {ProjectItemKind} from '../../types';

type DownloadTabProps = {
	kind: ProjectItemKind;
	virtualItems?: VirtualItem[];
};

export default function DownloadTab({
	kind,
	virtualItems = [],
}: DownloadTabProps) {
	const {bundles} = useLiferayBundles();

	const isProduct = kind === 'product';

	const items: DownloadItem[] = isProduct
		? bundles
		: virtualItems.map((virtualItem, index) => ({
				id: `${index}-${virtualItem.version}`,
				link: virtualItem.url,
				name: virtualItem.version,
			}));

	return (
		<DownloadListCard
			emptyLabel={isProduct ? 'no-bundles-yet' : 'no-versions-yet'}
			heading={isProduct ? 'bundle-name' : 'supported-version'}
			items={items}
			title={isProduct ? 'bundle-list' : 'versions-list'}
		/>
	);
}
