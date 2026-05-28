/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Params} from 'react-router-dom';

import SearchBuilder, {Operators} from '../core/SearchBuilder';
import {MarketplaceCategory} from '../enums/Categories';
import {ProductType, ProductWorkflowStatusCode} from '../enums/Product';
import i18n from '../i18n';

const LIFERAY_VERSION_PICKLIST = 'LIFERAY-VERSIONS';

type AutoCompleteProps = {
	label?: string;
	onSearch: (keyword: string) => any;
	resource?: string | ((params: Readonly<Params<string>>) => string);
	transformData?: (item: any) => any;
};

export type AppliedFilters = {
	label: string;
	value: string;
};

export type RenderedFieldOptions = string[] | AppliedFilters[];

export type RendererFields = {
	disabled?: boolean;
	label: string;
	name: string;
	operator?: Operators;
	optionalOperator?: Operators;
	options?: RenderedFieldOptions;
	placeholder?: string;
	removeQuoteMark?: boolean;
	requestOperator?: string;
	type:
		| 'autocomplete'
		| 'checkbox'
		| 'date'
		| 'date-range'
		| 'multiselect'
		| 'number'
		| 'select'
		| 'text'
		| 'textarea';
} & Partial<AutoCompleteProps>;

export type Filters = {
	[key: string]: RendererFields[];
};

export type Filter = {
	[key: string]: RendererFields;
};

export type FilterVariables = {
	appliedFilter?: {
		[key: string]: string | AppliedFilters;
	};
	defaultFilter?: string | SearchBuilder;
	filterSchema: FilterSchema;
};

export type FilterSchema = {
	fields: RendererFields[];
	name?: string;
	onApply?: (filterVariables: FilterVariables) => string;
	placeholder?: string;
};

export type FilterSchemas = {
	[key: string]: FilterSchema;
};

export const baseFilters: Filter = {
	categories: {
		label: i18n.translate('category'),
		name: 'categoryNames',
		operator: 'lambda',
		type: 'select',
	},
	dateCreated: {
		label: i18n.translate('date-created'),
		name: 'createDate',
		type: 'date-range',
	},
	status: {
		label: i18n.translate('status'),
		name: 'status',
		type: 'select',
	},
	type: {
		label: i18n.translate('type'),
		name: 'type',
		type: 'select',
	},
	version: {
		label: i18n.translate('version'),
		name: 'version',
		type: 'select',
	},
};

export const overrides = (
	object: RendererFields,
	newObject: Partial<RendererFields>
) => ({
	...object,
	...newObject,
});

export const filterSchema: FilterSchemas = {
	administratorApps: {
		fields: [
			overrides(baseFilters.type, {
				label: i18n.translate('app-type'),
				name: 'specificationValues|appType',
				operator: 'lambda',
				options: [
					{
						label: i18n.translate('client-extension'),
						value: ProductType.CLIENT_EXTENSION,
					},
					{
						label: i18n.translate('cloud-app'),
						value: ProductType.CLOUD,
					},
					{
						label: i18n.translate('composite-app'),
						value: ProductType.COMPOSITE_APP,
					},
					{
						label: i18n.translate('dxp-app'),
						value: ProductType.DXP,
					},
					{
						label: i18n.translate('low-code-configuration'),
						value: ProductType.LOW_CODE_CONFIGURATION,
					},
					{
						label: i18n.translate('other'),
						value: ProductType.OTHER,
					},
				],
				type: 'checkbox',
			}),
			overrides(baseFilters.categories, {
				options: [
					{
						label: i18n.translate('batch'),
						value: `${MarketplaceCategory.BATCH}`,
					},
					{
						label: i18n.translate('checkout'),
						value: `${MarketplaceCategory.CHECKOUT}`,
					},
					{
						label: i18n.translate('fragment'),
						value: `${MarketplaceCategory.FRAGMENTS}`,
					},
					{
						label: i18n.translate('object-action'),
						value: `${MarketplaceCategory.OBJECT_ACTION}`,
					},
					{
						label: i18n.translate('other'),
						value: `${MarketplaceCategory.OTHER}`,
					},
					{
						label: i18n.translate('payment-method'),
						value: `${MarketplaceCategory.PAYMENT_METHODS}`,
					},
					{
						label: i18n.translate('prompt'),
						value: `${MarketplaceCategory.PROMPT}`,
					},
					{
						label: i18n.translate('site-initializer'),
						value: `${MarketplaceCategory.SITE_INITIALIZER}`,
					},
					{
						label: i18n.translate('theme'),
						value: `${MarketplaceCategory.THEME}`,
					},
					{
						label: i18n.translate('workflow-action'),
						value: `${MarketplaceCategory.WORKFLOW_ACTION}`,
					},
				],
				type: 'select',
			}),
			baseFilters.dateCreated,
			overrides(baseFilters.version, {
				label: i18n.translate('liferay-version'),
				name: 'specificationValues|liferayVersion',
				operator: 'lambda',
				resource: `o/headless-admin-list-type/v1.0/list-type-definitions/by-external-reference-code/${LIFERAY_VERSION_PICKLIST}`,
				transformData: (item) =>
					item.listTypeEntries.map((entry: any) => ({
						label: entry.name,
						value: entry.name,
					})),
				type: 'multiselect',
			}),
			overrides(baseFilters.dateCreated, {
				label: i18n.translate('modified-date'),
				name: 'modifiedDate',
			}),
			overrides(baseFilters.status, {
				name: 'statusCode',
				options: [
					{
						label: i18n.translate('approved'),
						value: `${ProductWorkflowStatusCode.APPROVED}`,
					},
					{
						label: i18n.translate('draft'),
						value: `${ProductWorkflowStatusCode.DRAFT}`,
					},
					{
						label: i18n.translate('pending'),
						value: `${ProductWorkflowStatusCode.PENDING}`,
					},
				],
				removeQuoteMark: true,
				type: 'select',
			}),
		],
		name: 'administratorApps',
	},
	administratorSolutions: {
		fields: [
			baseFilters.dateCreated,
			overrides(baseFilters.dateCreated, {
				label: i18n.translate('modified-date'),
				name: 'modifiedDate',
			}),
			overrides(baseFilters.status, {
				name: 'statusCode',
				options: [
					{
						label: i18n.translate('approved'),
						value: `${ProductWorkflowStatusCode.APPROVED}`,
					},
					{
						label: i18n.translate('draft'),
						value: `${ProductWorkflowStatusCode.DRAFT}`,
					},
					{
						label: i18n.translate('pending'),
						value: `${ProductWorkflowStatusCode.PENDING}`,
					},
				],
				removeQuoteMark: true,
				type: 'multiselect',
			}),
		],
		name: 'administratorSolutions',
	},
};

export type FilterSchemaOption = keyof typeof filterSchema;
