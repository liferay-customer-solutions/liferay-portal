/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ProductTypeVocabulary} from '../../enums/Product';
import i18n from '../../i18n';
import {formatDate} from '../../utils/date';
import PublishedProductsListView, {
	renderAppType,
	renderLiferayVersion,
	renderProductName,
	renderProductStatus,
} from './PublishedProductsListView';

export default function PublishedApps() {
	return (
		<PublishedProductsListView
			categoryVocabulary={ProductTypeVocabulary.APP}
			countWord="x-apps-available"
			filterSchema="publisherApps"
			id="publisher-published-apps"
			tableProps={{
				actions: [
					{
						icon: 'view',
						name: i18n.translate('view-details'),
						onClick: (product: Product) =>
							window.open(product.urls?.en_US, '_blank'),
					},
				],
				columns: [
					{
						clickable: true,
						id: 'name',
						name: i18n.translate('name'),
						render: renderProductName,
						sortable: true,
					},
					{
						id: 'productSpecifications',
						name: i18n.translate('app-type'),
						render: renderAppType,
					},
					{
						id: 'catalog',
						name: i18n.translate('publisher'),
						render: (catalog) => catalog?.name,
					},
					{
						id: 'modifiedDate',
						name: i18n.translate('last-update'),
						render: (modifiedDate) => formatDate(modifiedDate),
						sortable: true,
					},
					{
						id: 'productSpecifications',
						name: i18n.translate('liferay-version'),
						render: renderLiferayVersion,
					},
					{
						id: 'workflowStatusInfo',
						name: i18n.translate('status'),
						render: renderProductStatus,
					},
				],
			}}
			title="published-apps"
		/>
	);
}
