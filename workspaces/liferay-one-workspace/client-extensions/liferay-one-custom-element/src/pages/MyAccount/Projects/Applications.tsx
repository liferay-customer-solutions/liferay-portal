/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';
import ClayIcon from '@clayui/icon';
import {useMemo} from 'react';
import {useNavigate} from 'react-router-dom';

import Page from '../../../components/Page';
import i18n, {Word, translate} from '../../../i18n';
import FilterableListCard, {ListColumn, ListFilter} from './FilterableListCard';
import {APPLICATIONS, Application} from './applications';

const STATUS_DOT_COLORS: {[key: string]: string} = {
	completed: 'var(--color-success)',
};

function matchesSearch(application: Application, search: string): boolean {
	return (
		application.name.toLowerCase().includes(search) ||
		application.providedBy.toLowerCase().includes(search) ||
		application.orderId.toLowerCase().includes(search)
	);
}

export default function Applications() {
	const navigate = useNavigate();

	const filters = useMemo<ListFilter<Application>[]>(() => {
		const saleTypes = Array.from(
			new Set(APPLICATIONS.map((application) => application.saleType))
		).sort();

		const statuses = Array.from(
			new Set(APPLICATIONS.map((application) => application.status))
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
			{
				key: 'status',
				label: 'status',
				matches: (application, values) =>
					values.includes(application.status),
				options: statuses.map((status) => ({
					label: translate(status as Word),
					value: status,
				})),
			},
		];
	}, []);

	const columns: ListColumn<Application>[] = [
		{
			heading: 'name',
			key: 'name',
			render: (application) => (
				<span className="list-card-name">
					<span
						className="list-card-icon"
						style={{backgroundColor: application.logoColor}}
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
					<span>{application.providedBy}</span>

					<span className="list-card-subtext">
						{application.providedDate}
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
			render: (application) => application.orderId,
		},
		{
			heading: 'status',
			key: 'status',
			render: (application) => (
				<span className="list-card-status">
					<span
						className="list-card-status-dot"
						style={{
							backgroundColor:
								STATUS_DOT_COLORS[application.status] ??
								'var(--color-neutral-6)',
						}}
					/>

					{translate(application.status as Word)}
				</span>
			),
		},
		{
			key: 'actions',
			render: (application) => (
				<ClayButton
					aria-label={translate('application-details')}
					borderless
					className="text-neutral-7"
					displayType="unstyled"
					onClick={(event) => {
						event.stopPropagation();
						navigate(application.id);
					}}
				>
					<ClayIcon symbol="ellipsis-v" />
				</ClayButton>
			),
		},
	];

	return (
		<Page
			description={i18n.translate(
				'manage-the-applications-within-your-project'
			)}
			title={i18n.translate('applications')}
		>
			<FilterableListCard
				columns={columns}
				emptyLabel="no-applications-yet"
				filters={filters}
				items={APPLICATIONS}
				matchesSearch={matchesSearch}
				onItemClick={(application) => navigate(application.id)}
				rowKey={(application) => application.id}
			/>
		</Page>
	);
}
