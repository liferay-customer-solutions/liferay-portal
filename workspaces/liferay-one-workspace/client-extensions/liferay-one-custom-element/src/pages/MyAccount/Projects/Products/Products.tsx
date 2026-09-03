/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayIcon from '@clayui/icon';
import {useMemo} from 'react';
import {useNavigate} from 'react-router-dom';
import {useProject} from '~/context/ProjectContext';
import {ProjectProduct, useProjectProducts} from '~/hooks/useProjectCommerce';
import {
	getOrderTypeByProductName,
	useProjectOrders,
} from '~/hooks/useProjectOrders';
import i18n from '~/i18n';
import {
	ListColumn,
	ListFilter,
} from '~/pages/MyAccount/Projects/components/FilterableListCard/FilterableListCard';
import ProductListPage, {
	statusColumn,
	statusFilter,
} from '~/pages/MyAccount/Projects/components/ProductListPage/ProductListPage';
import {getLogoColor} from '~/pages/MyAccount/Projects/utils/getLogoColor';
import {getProductIcon} from '~/pages/MyAccount/Projects/utils/getProductIcon';
import {isApplicationOrderType} from '~/pages/MyAccount/Projects/utils/isApplicationOrderType';
import {isUnassignedProject} from '~/pages/MyAccount/Projects/utils/isUnassignedProject';

export default function Products() {
	const navigate = useNavigate();
	const {projectId, projects, selectedContractERC} = useProject();

	const projectName = isUnassignedProject(projectId)
		? undefined
		: projects.find(
				(project) => project.externalReferenceCode === projectId
			)?.name;

	const {error, loading, products} = useProjectProducts(
		projectId,
		selectedContractERC
	);

	const {loading: ordersLoading, placedOrders} =
		useProjectOrders(projectName);

	const projectProducts = useMemo(() => {
		const orderTypeByProductName = getOrderTypeByProductName(placedOrders);

		return products.filter(
			(product) =>
				!isApplicationOrderType(
					orderTypeByProductName.get(product.name)
				)
		);
	}, [placedOrders, products]);

	const filters = useMemo<ListFilter<ProjectProduct>[]>(() => {
		const types = Array.from(
			new Set(projectProducts.map((product) => product.type))
		).sort();

		return [
			{
				key: 'type',
				label: 'type',
				matches: (product, values) => values.includes(product.type),
				options: types.map((type) => ({label: type, value: type})),
			},
			statusFilter(projectProducts),
		];
	}, [projectProducts]);

	const columns: ListColumn<ProjectProduct>[] = [
		{
			expanded: true,
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
			width: '1%',
		},
		{
			heading: 'start-date',
			key: 'start-date',
			noWrap: true,
			render: (product) => product.startDate,
			width: '1%',
		},
		{
			heading: 'end-date',
			key: 'end-date',
			noWrap: true,
			render: (product) => product.endDate,
			width: '1%',
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
			items={projectProducts}
			loading={loading || ordersLoading}
			onItemClick={(product) => navigate(product.externalReferenceCode)}
			title="products"
		/>
	);
}
