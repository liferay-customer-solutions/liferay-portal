/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import Page, {PageRendererProps} from '~/components/Page/Page';
import RowActionsMenu from '~/components/RowActionsMenu/RowActionsMenu';
import {ProjectProduct} from '~/hooks/useProjectCommerce';
import i18n, {Word, translate} from '~/i18n';
import {getStatusColor} from '~/pages/MyAccount/Projects/utils/getStatusColor';

import FilterableListCard, {
	ListColumn,
	ListFilter,
} from '../FilterableListCard/FilterableListCard';

export function matchesProductSearch(
	product: ProjectProduct,
	search: string
): boolean {
	return (
		product.name.toLowerCase().includes(search) ||
		product.publisher.toLowerCase().includes(search)
	);
}

export function statusColumn(): ListColumn<ProjectProduct> {
	return {
		heading: 'status',
		key: 'status',
		render: (product) => (
			<span className="list-card-status">
				<span
					className="list-card-status-dot"
					style={{backgroundColor: getStatusColor(product.status)}}
				/>

				{translate(product.status as Word)}
			</span>
		),
	};
}

export function statusFilter(
	products: ProjectProduct[]
): ListFilter<ProjectProduct> {
	const statuses = Array.from(
		new Set(products.map((product) => product.status))
	).sort();

	return {
		key: 'status',
		label: 'status',
		matches: (product, values) => values.includes(product.status),
		options: statuses.map((status) => ({
			label: translate(status as Word),
			value: status,
		})),
	};
}

type ProductListPageProps = {
	columns: ListColumn<ProjectProduct>[];
	description: Word;
	emptyLabel: Word;
	error?: PageRendererProps['error'];
	filters: ListFilter<ProjectProduct>[];
	items: ProjectProduct[];
	loading: boolean;
	onItemClick: (product: ProjectProduct) => void;
	title: Word;
};

export default function ProductListPage({
	columns,
	description,
	emptyLabel,
	error,
	filters,
	items,
	loading,
	onItemClick,
	title,
}: ProductListPageProps) {
	const columnsWithActions: ListColumn<ProjectProduct>[] = [
		...columns,
		{
			key: 'actions',
			render: (product) => (
				<RowActionsMenu
					actions={[
						{
							label: 'view-details',
							onClick: () => onItemClick(product),
						},
					]}
				/>
			),
		},
	];

	return (
		<Page
			description={i18n.translate(description)}
			pageRendererProps={{error, isLoading: loading}}
			title={i18n.translate(title)}
		>
			<FilterableListCard
				columns={columnsWithActions}
				emptyLabel={emptyLabel}
				filters={filters}
				items={items}
				matchesSearch={matchesProductSearch}
				onItemClick={onItemClick}
				rowKey={(product) => product.id}
			/>
		</Page>
	);
}
