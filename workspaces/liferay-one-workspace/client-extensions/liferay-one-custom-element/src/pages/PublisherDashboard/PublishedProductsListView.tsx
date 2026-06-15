/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import EmptyState from '../../components/EmptyState';
import ListView, {ListViewProps} from '../../components/ListView';
import Loading from '../../components/Loading';
import Page from '../../components/Page';
import SearchBuilder from '../../core/SearchBuilder';
import {
	ProductSpecificationKey,
	ProductTypeLabels,
	ProductTypeVocabulary,
	ProductWorkflowStatusCode,
	ProductWorkflowStatusLabel,
} from '../../enums/Product';
import {useFetch} from '../../hooks/useFetch';
import usePublisherCatalog from '../../hooks/usePublisherCatalog';
import i18n, {Word} from '../../i18n';
import {formatDate} from '../../utils/date';

export const PRODUCTS_RESOURCE = `/o/headless-commerce-admin-catalog/v1.0/products?${new URLSearchParams(
	{
		'nestedFields': 'catalog,productSpecifications',
		'productSpecifications.pageSize': '-1',
		'sort': 'createDate:desc',
	}
)}`;

const STATUS_DOT_COLOR: Record<number, string> = {
	[ProductWorkflowStatusCode.APPROVED]: 'var(--color-success)',
	[ProductWorkflowStatusCode.DRAFT]: 'var(--color-neutral-5)',
	[ProductWorkflowStatusCode.PENDING]: 'var(--color-warning)',
};

export function buildCatalogCategoryFilter(
	catalogId: number,
	categoryVocabulary: ProductTypeVocabulary
) {
	return new SearchBuilder({useURIEncode: false})
		.eq('catalogId', catalogId, {unquote: true})
		.and()
		.lambda('categoryNames', categoryVocabulary)
		.build();
}

function specificationValue(
	productSpecifications: ProductSpecification[],
	key: ProductSpecificationKey
) {
	return productSpecifications?.find(
		({specificationKey}) => specificationKey === key
	)?.value?.en_US;
}

export function renderProductName(name: Product['name'], product: Product) {
	return (
		<div className="align-items-center d-flex">
			<img
				alt=""
				src={product.thumbnail}
				style={{
					borderRadius: '0.5rem',
					height: '2rem',
					objectFit: 'cover',
					width: '2rem',
				}}
			/>

			<a
				className="font-weight-semi-bold ml-2 text-nowrap"
				href={product.urls?.en_US}
			>
				{name?.en_US}
			</a>
		</div>
	);
}

export function renderAppType(productSpecifications: ProductSpecification[]) {
	const type = specificationValue(
		productSpecifications,
		ProductSpecificationKey.APP_TYPE
	);

	return (
		<span className="text-capitalize">
			{ProductTypeLabels[type as keyof typeof ProductTypeLabels] ?? type}
		</span>
	);
}

export function renderLiferayVersion(
	productSpecifications: ProductSpecification[]
) {
	return (
		specificationValue(
			productSpecifications,
			ProductSpecificationKey.LIFERAY_VERSION
		) ?? '-'
	);
}

export function renderProductStatus(
	workflowStatusInfo: Product['workflowStatusInfo']
) {
	return (
		<span className="align-items-center d-flex">
			<span
				style={{
					backgroundColor:
						STATUS_DOT_COLOR[workflowStatusInfo.code] ??
						'var(--color-neutral-5)',
					borderRadius: '50%',
					display: 'inline-block',
					height: '0.5rem',
					marginRight: '0.5rem',
					width: '0.5rem',
				}}
			/>

			{ProductWorkflowStatusLabel[
				workflowStatusInfo.code as keyof typeof ProductWorkflowStatusLabel
			] ?? workflowStatusInfo.label}
		</span>
	);
}

type PublishedProductsListViewProps = {
	categoryVocabulary: ProductTypeVocabulary;
	countWord: Word;
	filterSchema: string;
	id: string;
	tableProps: ListViewProps<Product>['tableProps'];
	title: Word;
};

export default function PublishedProductsListView({
	categoryVocabulary,
	countWord,
	filterSchema,
	id,
	tableProps,
	title,
}: PublishedProductsListViewProps) {
	const {data: catalog, isLoading} = usePublisherCatalog();

	const catalogId = catalog?.id;

	const baseFilter = catalogId
		? buildCatalogCategoryFilter(catalogId, categoryVocabulary)
		: undefined;

	const {data: countData} = useFetch(catalogId ? PRODUCTS_RESOURCE : null, {
		params: {filter: baseFilter, pageSize: 1},
	});

	if (isLoading) {
		return <Loading className="mt-3" />;
	}

	if (!catalog) {
		return (
			<Page title={i18n.translate(title)}>
				<EmptyState
					title={i18n.translate('no-publisher-catalog-found')}
					type="EMPTY_STATE"
				/>
			</Page>
		);
	}

	return (
		<Page
			description={
				countData?.totalCount != null
					? i18n.sub(countWord, String(countData.totalCount))
					: undefined
			}
			title={i18n.translate(title)}
		>
			<ListView<Product>
				defaultFilters={{filter: baseFilter!}}
				id={id}
				managementToolbarProps={{
					filterSchema,
					searchVisible: true,
					visible: true,
				}}
				resource={PRODUCTS_RESOURCE}
				tableProps={tableProps}
			/>
		</Page>
	);
}
