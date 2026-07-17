/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayIcon from '@clayui/icon';
import {useMemo} from 'react';
import {Link, useNavigate, useParams} from 'react-router-dom';
import RowActionsMenu from '~/components/RowActionsMenu/RowActionsMenu';
import {useProject} from '~/context/ProjectContext';
import {ProjectOrder, useProjectOrders} from '~/hooks/useProjectOrders';
import {translate} from '~/i18n';
import {getStatusColor} from '~/pages/MyAccount/Projects/utils/getStatusColor';

import FilterableListCard, {
	ListColumn,
	ListFilter,
} from '../FilterableListCard/FilterableListCard';

function matchesSearch(order: ProjectOrder, search: string): boolean {
	return order.orderId.toLowerCase().includes(search);
}

export default function OrdersTab() {
	const {projectId, projects} = useProject();
	const {accountERC} = useParams();
	const navigate = useNavigate();

	const projectName = projects.find(
		(project) => project.externalReferenceCode === projectId
	)?.name;

	const {orders} = useProjectOrders(projectName);

	const filters = useMemo<ListFilter<ProjectOrder>[]>(() => {
		const statuses = Array.from(
			new Set(orders.map((order) => order.status))
		).sort();

		return [
			{
				key: 'status',
				label: 'status',
				matches: (order, values) => values.includes(order.status),
				options: statuses.map((status) => ({
					label: status,
					value: status,
				})),
			},
		];
	}, [orders]);

	const columns: ListColumn<ProjectOrder>[] = [
		{
			expanded: true,
			heading: 'order-id',
			key: 'order-id',
			render: (order) => (
				<span style={{fontWeight: 600}}>{order.orderId}</span>
			),
		},
		{
			heading: 'date',
			key: 'date',
			render: (order) => order.date,
			width: '1%',
		},
		{
			heading: 'total',
			key: 'total',
			render: (order) => order.total,
			width: '1%',
		},
		{
			heading: 'status',
			key: 'status',
			render: (order) => (
				<span className="list-card-status">
					<span
						className="list-card-status-dot"
						style={{
							backgroundColor: getStatusColor(
								order.status.toLowerCase()
							),
						}}
					/>

					{order.status}
				</span>
			),
			width: '1%',
		},
		{
			key: 'actions',
			render: (order) => (
				<RowActionsMenu
					actions={[
						{
							label: 'view-details',
							onClick: () =>
								navigate(`/${accountERC}/orders/${order.id}`),
						},
					]}
				/>
			),
			width: '1%',
		},
	];

	return (
		<FilterableListCard
			action={
				<Link
					className="align-items-center d-flex font-weight-bold mr-2 text-dark text-decoration-none"
					to={`/${accountERC}/orders`}
				>
					{translate('view-all-account-orders')}

					<ClayIcon className="ml-1" symbol="shortcut" />
				</Link>
			}
			columns={columns}
			emptyLabel="no-orders-yet"
			filters={filters}
			items={orders}
			matchesSearch={matchesSearch}
			onItemClick={() => {}}
			rowKey={(order) => order.id}
			title="orders-list"
		/>
	);
}
