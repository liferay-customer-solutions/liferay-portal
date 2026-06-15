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
import {PRODUCTS, Product} from './products';

const STATUS_DOT_COLORS: {[key: string]: string} = {
	active: 'var(--color-success)',
};

function matchesSearch(product: Product, search: string): boolean {
	return (
		product.name.toLowerCase().includes(search) ||
		product.publisher.toLowerCase().includes(search)
	);
}

export default function Products() {
	const navigate = useNavigate();

	const filters = useMemo<ListFilter<Product>[]>(() => {
		const types = Array.from(
			new Set(PRODUCTS.map((product) => product.type))
		).sort();

		const statuses = Array.from(
			new Set(PRODUCTS.map((product) => product.status))
		).sort();

		return [
			{
				key: 'type',
				label: 'type',
				matches: (product, values) => values.includes(product.type),
				options: types.map((type) => ({
					label: translate(type as Word),
					value: type,
				})),
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
	}, []);

	const columns: ListColumn<Product>[] = [
		{
			heading: 'name',
			key: 'name',
			render: (product) => (
				<span className="list-card-name">
					<span
						className="list-card-icon"
						style={{backgroundColor: product.logoColor}}
					>
						<ClayIcon symbol={product.icon} />
					</span>

					<span className="list-card-name-text">
						<span className="list-card-name-label">
							{product.name}

							{product.badge && (
								<span className="list-card-badge">
									{translate(product.badge)}
								</span>
							)}
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
			render: (product) => translate(product.type),
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
							backgroundColor:
								STATUS_DOT_COLORS[product.status] ??
								'var(--color-neutral-6)',
						}}
					/>

					{translate(product.status as Word)}
				</span>
			),
		},
		{
			key: 'actions',
			render: (product) => (
				<ClayButton
					aria-label={translate('product-details')}
					borderless
					className="text-neutral-7"
					displayType="unstyled"
					onClick={(event) => {
						event.stopPropagation();
						navigate(product.id);
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
				'manage-the-products-within-your-project'
			)}
			title={i18n.translate('products')}
		>
			<FilterableListCard
				columns={columns}
				emptyLabel="no-products-yet"
				filters={filters}
				items={PRODUCTS}
				matchesSearch={matchesSearch}
				onItemClick={(product) => navigate(product.id)}
				rowKey={(product) => product.id}
			/>
		</Page>
	);
}
