/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ProductTypeVocabulary} from '../../enums/Product';
import i18n from '../../i18n';
import {formatDate} from '../../utils/date';
import PublishedProductsListView, {
	renderProductName,
	renderProductStatus,
} from './PublishedProductsListView';

export default function PublishedSolutions() {
	return (
		<PublishedProductsListView
			categoryVocabulary={ProductTypeVocabulary.SOLUTION}
			countWord="x-solutions-available"
			filterSchema="publisherSolutions"
			id="publisher-published-solutions"
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
						id: 'modifiedDate',
						name: i18n.translate('last-update'),
						render: (modifiedDate) => formatDate(modifiedDate),
						sortable: true,
					},
					{
						id: 'workflowStatusInfo',
						name: i18n.translate('status'),
						render: renderProductStatus,
					},
				],
			}}
			title="published-solutions"
		/>
	);
}
