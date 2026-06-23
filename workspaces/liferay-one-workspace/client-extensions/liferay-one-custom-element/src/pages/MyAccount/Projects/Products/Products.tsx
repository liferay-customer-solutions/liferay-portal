/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayIcon from '@clayui/icon';
import {useMemo} from 'react';
import {useNavigate} from 'react-router-dom';
import Page from '~/components/Page/Page';
import {useProject} from '~/context/ProjectContext';
import {ProjectProduct, useProjectProducts} from '~/hooks/useProjectCommerce';
import i18n, {Word, translate} from '~/i18n';
import FilterableListCard, {
	ListColumn,
	ListFilter,
} from '~/pages/MyAccount/Projects/components/FilterableListCard/FilterableListCard';
import RowActionsMenu from '~/pages/MyAccount/Projects/components/RowActionsMenu/RowActionsMenu';
import {getLogoColor} from '~/pages/MyAccount/Projects/utils/getLogoColor';
import {getProductIcon} from '~/pages/MyAccount/Projects/utils/getProductIcon';
import {getStatusColor} from '~/pages/MyAccount/Projects/utils/getStatusColor';

function matchesSearch(product: ProjectProduct, search: string): boolean {
	return (
		product.name.toLowerCase().includes(search) ||
		product.publisher.toLowerCase().includes(search)
	);
}

export default function Products() {
	const navigate = useNavigate();
	const {projectId} = useProject();

	const {error, loading, products} = useProjectProducts(projectId);

	const liferayProducts = useMemo(
		() =>
			products.filter((product) =>
				product.categoryNames.includes('liferay-product')
			),
		[products]
	);

	const filters = useMemo<ListFilter<ProjectProduct>[]>(() => {
		const types = Array.from(
			new Set(liferayProducts.map((product) => product.type))
		).sort();

		const statuses = Array.from(
			new Set(liferayProducts.map((product) => product.status))
		).sort();

		return [
			{
				key: 'type',
				label: 'type',
				matches: (product, values) => values.includes(product.type),
				options: types.map((type) => ({label: type, value: type})),
			},
			{
				key: 'status',
				label: 'status',
				matches: (product, values) => values.includes(product.status),
				options: statuses.map((status) => ({
					label: translate(status as Word),
					value: status,
				})),
			},
		];
	}, [liferayProducts]);

	const columns: ListColumn<ProjectProduct>[] = [
		{
			heading: 'name',
			key: 'name',
			render: (product) => (
				<span className="list-card-name">
					<span
						className="list-card-icon"
						style={{backgroundColor: getLogoColor(product.name)}}
					>
						<ClayIcon symbol={getProductIcon(product.type)} />
					</span>

					<span className="list-card-name-text">
						<span className="list-card-name-label">
							{product.name}
						</span>

						<span className="list-card-subtext">
							{i18n.sub('by-x', product.publisher)}
						</span>
					</span>
				</span>
			),
		},
		{
			heading: 'type',
			key: 'type',
			render: (product) => product.type,
		},
		{
			heading: 'start-date',
			key: 'start-date',
			render: (product) => product.startDate,
		},
		{
			heading: 'status',
			key: 'status',
			render: (product) => (
				<span className="list-card-status">
					<span
						className="list-card-status-dot"
						style={{
							backgroundColor: getStatusColor(product.status),
						}}
					/>

					{translate(product.status as Word)}
				</span>
			),
		},
		{
			key: 'actions',
			render: (product) => (
				<RowActionsMenu
					actions={[
						{
							label: 'view-details',
							onClick: () =>
								navigate(product.externalReferenceCode),
						},
					]}
				/>
			),
		},
	];

	return (
		<Page
			description={i18n.translate(
				'manage-the-products-within-your-project'
			)}
			pageRendererProps={{error, isLoading: loading}}
			title={i18n.translate('products')}
		>
			<FilterableListCard
				columns={columns}
				emptyLabel="no-products-yet"
				filters={filters}
				items={liferayProducts}
				matchesSearch={matchesSearch}
				onItemClick={(product) =>
					navigate(product.externalReferenceCode)
				}
				rowKey={(product) => product.id}
			/>
		</Page>
	);
}
