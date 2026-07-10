/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useMemo} from 'react';
import {useNavigate} from 'react-router-dom';
import {useProject} from '~/context/ProjectContext';
import {ProjectProduct, useProjectProducts} from '~/hooks/useProjectCommerce';
import {useProjectOrders} from '~/hooks/useProjectOrders';
import {
	ListColumn,
	ListFilter,
} from '~/pages/MyAccount/Projects/components/FilterableListCard/FilterableListCard';
import ProductListPage, {
	statusColumn,
	statusFilter,
} from '~/pages/MyAccount/Projects/components/ProductListPage/ProductListPage';
import {getLogoColor} from '~/pages/MyAccount/Projects/utils/getLogoColor';
import {isUnassignedProject} from '~/pages/MyAccount/Projects/utils/isUnassignedProject';

export default function Applications() {
	const navigate = useNavigate();
	const {projectId, projects} = useProject();

	const projectName = isUnassignedProject(projectId)
		? undefined
		: projects.find(
				(project) => project.externalReferenceCode === projectId
			)?.name;

	const {error, loading, products} = useProjectProducts(projectId);
	const {placedOrders} = useProjectOrders(projectName);

	const applications = useMemo(
		() =>
			products.filter((product) => product.categoryNames.includes('app')),
		[products]
	);

	const orderIdByProductName = useMemo(() => {
		const map = new Map<string, string>();

		for (const order of placedOrders) {
			for (const item of order.placedOrderItems ?? []) {
				if (!map.has(item.name)) {
					map.set(item.name, String(order.id));
				}
			}
		}

		return map;
	}, [placedOrders]);

	const filters = useMemo<ListFilter<ProjectProduct>[]>(() => {
		const saleTypes = Array.from(
			new Set(applications.map((application) => application.saleType))
		).sort();

		return [
			{
				key: 'sale-type',
				label: 'sale-type',
				matches: (application, values) =>
					values.includes(application.saleType),
				options: saleTypes.map((saleType) => ({
					label: saleType,
					value: saleType,
				})),
			},
			statusFilter(applications),
		];
	}, [applications]);

	const columns: ListColumn<ProjectProduct>[] = [
		{
			heading: 'name',
			key: 'name',
			render: (application) => (
				<span className="list-card-name">
					<span
						className="list-card-icon"
						style={{
							backgroundColor: getLogoColor(application.name),
						}}
					>
						{application.name.charAt(0)}
					</span>

					<span className="list-card-name-label">
						{application.name}
					</span>
				</span>
			),
		},
		{
			heading: 'provided-by',
			key: 'provided-by',
			render: (application) => (
				<span className="d-flex flex-column">
					<span>{application.publisher}</span>

					<span className="list-card-subtext">
						{application.startDate}
					</span>
				</span>
			),
		},
		{
			heading: 'sale-type',
			key: 'sale-type',
			render: (application) => application.saleType,
		},
		{
			heading: 'order-id',
			key: 'order-id',
			render: (application) =>
				orderIdByProductName.get(application.name) ?? '-',
		},
		statusColumn(),
	];

	return (
		<ProductListPage
			columns={columns}
			description="manage-the-applications-within-your-project"
			emptyLabel="no-applications-yet"
			error={error}
			filters={filters}
			items={applications}
			loading={loading}
			onItemClick={(application) =>
				navigate(application.externalReferenceCode)
			}
			title="applications"
		/>
	);
}
