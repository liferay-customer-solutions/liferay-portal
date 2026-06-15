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
import {INVOICES, Invoice, getStatusColor} from './tabData';

function matchesSearch(invoice: Invoice, search: string): boolean {
	return invoice.invoiceId.toLowerCase().includes(search);
}

export default function InvoicesCard() {
	const filters = useMemo<ListFilter<Invoice>[]>(() => {
		const statuses = Array.from(
			new Set(INVOICES.map((invoice) => invoice.status))
		).sort();

		return [
			{
				key: 'status',
				label: 'status',
				matches: (invoice, values) => values.includes(invoice.status),
				options: statuses.map((status) => ({
					label: translate(status as Word),
					value: status,
				})),
			},
		];
	}, []);

	const columns: ListColumn<Invoice>[] = [
		{
			heading: 'invoice-id',
			key: 'invoice-id',
			render: (invoice) => (
				<span style={{fontWeight: 600}}>{invoice.invoiceId}</span>
			),
		},
		{
			heading: 'date',
			key: 'date',
			render: (invoice) => invoice.date,
		},
		{
			heading: 'type',
			key: 'type',
			render: (invoice) => translate(invoice.type),
		},
		{
			heading: 'amount',
			key: 'amount',
			render: (invoice) => invoice.amount,
		},
		{
			heading: 'status',
			key: 'status',
			render: (invoice) => (
				<span className="list-card-status">
					<span
						className="list-card-status-dot"
						style={{
							backgroundColor: getStatusColor(invoice.status),
						}}
					/>

					{translate(invoice.status as Word)}
				</span>
			),
		},
		{
			key: 'actions',
			render: () => (
				<RowActionsMenu
					actions={[
						{label: 'view-details'},
						{label: 'download-invoice'},
					]}
				/>
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
					{translate('view-all-account-invoices')}

					<ClayIcon symbol="shortcut" />
				</Link>
			}
			columns={columns}
			emptyLabel="no-invoices-yet"
			filters={filters}
			items={INVOICES}
			matchesSearch={matchesSearch}
			onItemClick={() => {}}
			rowKey={(invoice) => invoice.id}
			title="invoices-list"
		/>
	);
}
