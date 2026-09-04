/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useNavigate} from 'react-router-dom';
import i18n from '~/i18n';
import {formatDate} from '~/utils/dateUtils';
import {
	ProductWorkflowStatusCode,
	getProductPageURL,
} from '~/utils/productUtils';

import PublishedProductsListView, {
	renderAppType,
	renderProductName,
	renderProductStatus,
	renderProductVersion,
} from '../components/PublishedProductsListView/PublishedProductsListView';

import type {Product} from '~/types/product';

export default function PublishedApps() {
	const navigate = useNavigate();

	return (
		<PublishedProductsListView
			categoryVocabulary="app"
			ctaLabel="publish-new-app"
			description="manage-and-publish-apps-on-the-marketplace"
			emptyStateDescription="publish-your-first-app-to-make-it-available"
			emptyStateTitle="you-havent-published-any-apps-yet"
			filterSchema="publisherApps"
			id="publisher-published-apps"
			onCtaClick={() => navigate('/newapp/publisher')}
			tableProps={{
				actions: [
					{
						icon: 'view',
						name: i18n.translate('view'),
						onClick: (product: Product) =>
							navigate(`/published-apps/${product.productId}`),
					},
					{
						hidden: (product: Product) =>
							product.productStatus !==
							ProductWorkflowStatusCode.APPROVED,
						icon: 'shortcut',
						name: i18n.translate('open-in-marketplace'),
						onClick: (product: Product) =>
							window.open(
								getProductPageURL(product.urls),
								'_blank'
							),
					},
				],
				columns: [
					{
						id: 'name',
						name: i18n.translate('name'),
						render: renderProductName,
						sortable: true,
					},
					{
						id: 'productSpecifications',
						name: i18n.translate('version'),
						render: renderProductVersion,
					},
					{
						id: 'productSpecifications',
						name: i18n.translate('app-type'),
						render: renderAppType,
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
			title="published-apps"
		/>
	);
}
