/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayIcon from '@clayui/icon';
import {useMemo} from 'react';
import {Link} from 'react-router-dom';

import {Word, translate} from '../../../i18n';
import FilterableListCard, {ListColumn, ListFilter} from './FilterableListCard';
import RowActionsMenu from './RowActionsMenu';
import {ORDERS, Order, getStatusColor} from './tabData';

function matchesSearch(order: Order, search: string): boolean {
	return order.orderId.toLowerCase().includes(search);
}

export default function OrdersCard() {
	const filters = useMemo<ListFilter<Order>[]>(() => {
		const statuses = Array.from(
			new Set(ORDERS.map((order) => order.status))
		).sort();

		return [
			{
				key: 'status',
				label: 'status',
				matches: (order, values) => values.includes(order.status),
				options: statuses.map((status) => ({
					label: translate(status as Word),
					value: status,
				})),
			},
		];
	}, []);

	const columns: ListColumn<Order>[] = [
		{
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
		},
		{
			heading: 'total',
			key: 'total',
			render: (order) => order.total,
		},
		{
			heading: 'status',
			key: 'status',
			render: (order) => (
				<span className="list-card-status">
					<span
						className="list-card-status-dot"
						style={{backgroundColor: getStatusColor(order.status)}}
					/>

					{translate(order.status as Word)}
				</span>
			),
		},
		{
			key: 'actions',
			render: () => (
				<RowActionsMenu actions={[{label: 'view-details'}]} />
			),
		},
	];

	return (
		<FilterableListCard
			action={
				<Link
					className="align-items-center d-flex gap-2 text-decoration-none"
					to="/orders"
				>
					{translate('view-all-account-orders')}

					<ClayIcon symbol="shortcut" />
				</Link>
			}
			columns={columns}
			emptyLabel="no-orders-yet"
			filters={filters}
			items={ORDERS}
			matchesSearch={matchesSearch}
			onItemClick={() => {}}
			rowKey={(order) => order.id}
			title="orders-list"
		/>
	);
}
