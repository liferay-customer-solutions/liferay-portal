/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useLiferayBundles} from '~/hooks/useLiferayBundles';

import DownloadListCard from '../DownloadListCard/DownloadListCard';

import type {ProjectItemKind} from '../../types';

type DownloadTabProps = {
	kind: ProjectItemKind;
};

export default function DownloadTab({kind}: DownloadTabProps) {
	const {bundles} = useLiferayBundles();

	const isProduct = kind === 'product';

	return (
		<DownloadListCard
			emptyLabel={isProduct ? 'no-bundles-yet' : 'no-versions-yet'}
			heading={isProduct ? 'bundle-name' : 'supported-version'}
			items={bundles}
			title={isProduct ? 'bundle-list' : 'versions-list'}
		/>
	);
}
