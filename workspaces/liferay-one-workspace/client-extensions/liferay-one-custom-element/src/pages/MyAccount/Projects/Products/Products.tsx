/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayIcon from '@clayui/icon';
import {useMemo} from 'react';
import {useNavigate} from 'react-router-dom';
import {useProject} from '~/context/ProjectContext';
import {ProjectProduct, useProjectProducts} from '~/hooks/useProjectCommerce';
import i18n from '~/i18n';
import {
	ListColumn,
	ListFilter,
} from '~/pages/MyAccount/Projects/components/FilterableListCard/FilterableListCard';
import ProductListPage, {
	statusColumn,
	statusFilter,
} from '~/pages/MyAccount/Projects/components/ProductListPage/ProductListPage';
import {PRODUCT_CATEGORY} from '~/pages/MyAccount/Projects/utils/constants';
import {getLogoColor} from '~/pages/MyAccount/Projects/utils/getLogoColor';
import {getProductIcon} from '~/pages/MyAccount/Projects/utils/getProductIcon';

export default function Products() {
	const navigate = useNavigate();
	const {projectId} = useProject();

	const {error, loading, products} = useProjectProducts(projectId);

	const liferayProducts = useMemo(
		() =>
			products.filter((product) =>
				product.categoryNames.includes(PRODUCT_CATEGORY.LIFERAY_PRODUCT)
			),
		[products]
	);

	const filters = useMemo<ListFilter<ProjectProduct>[]>(() => {
		const types = Array.from(
			new Set(liferayProducts.map((product) => product.type))
		).sort();

		return [
			{
				key: 'type',
				label: 'type',
				matches: (product, values) => values.includes(product.type),
				options: types.map((type) => ({label: type, value: type})),
			},
			statusFilter(liferayProducts),
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
		statusColumn(),
	];

	return (
		<ProductListPage
			columns={columns}
			description="manage-the-products-within-your-project"
			emptyLabel="no-products-yet"
			error={error}
			filters={filters}
			items={liferayProducts}
			loading={loading}
			onItemClick={(product) => navigate(product.externalReferenceCode)}
			title="products"
		/>
	);
}
