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
	renderProductName,
	renderProductStatus,
} from '../components/PublishedProductsListView/PublishedProductsListView';

import type {Product} from '~/types/product';

export default function PublishedSolutions() {
	const navigate = useNavigate();

	return (
		<PublishedProductsListView
			categoryVocabulary="solution"
			ctaLabel="new-solution-template"
			description="manage-and-publish-solutions-on-the-marketplace"
			emptyStateDescription="publish-your-first-solution-to-make-it-available"
			emptyStateTitle="you-havent-published-any-solutions-yet"
			filterSchema="publisherSolutions"
			id="publisher-published-solutions"
			onCtaClick={() => navigate('/newsolution/publisher')}
			tableProps={{
				actions: [
					{
						hidden: (product: Product) =>
							product.productStatus ===
							ProductWorkflowStatusCode.PENDING,
						icon: 'pencil',
						name: i18n.translate('edit'),
						onClick: (product: Product) =>
							navigate(
								`/newsolution/${product.productId}/publisher/profile`
							),
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
