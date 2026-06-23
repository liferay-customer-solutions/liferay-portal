/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Params} from 'react-router-dom';
import i18n from '~/i18n';
import {PublisherPayoutStatus} from '~/pages/Admin/Payments/Payments';
import SearchBuilder, {Operators} from '~/utils/SearchBuilder';
import {OrderWorkflowStatusCode, PaymentStatus} from '~/utils/orderUtils';
import {ProductWorkflowStatusCode} from '~/utils/productUtils';

import type {ListTypeDefinition} from '~/types/listTypeDefinition';

const LIFERAY_VERSION_PICKLIST = 'LIFERAY-VERSIONS';

type AutoCompleteProps = {
	label?: string;
	onSearch: (keyword: string) => unknown;
	resource?: string | ((params: Readonly<Params<string>>) => string);
	transformData?: (item: unknown) => unknown;
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
	appliedFilter?: Record<string, unknown>;
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

export function overrides(
	object: RendererFields,
	newObject: Partial<RendererFields>
): RendererFields {
	return {
		...object,
		...newObject,
	};
}

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
						value: 'client-extension',
					},
					{
						label: i18n.translate('cloud-app'),
						value: 'cloud',
					},
					{
						label: i18n.translate('composite-app'),
						value: 'composite-app',
					},
					{
						label: i18n.translate('dxp-app'),
						value: 'dxp',
					},
					{
						label: i18n.translate('low-code-configuration'),
						value: 'low-code-configuration',
					},
					{
						label: i18n.translate('other'),
						value: 'other',
					},
				],
				type: 'checkbox',
			}),
			overrides(baseFilters.categories, {
				options: [
					{
						label: i18n.translate('batch'),
						value: `${'Batch'}`,
					},
					{
						label: i18n.translate('checkout'),
						value: `${'Checkout'}`,
					},
					{
						label: i18n.translate('fragment'),
						value: `${'Fragments'}`,
					},
					{
						label: i18n.translate('object-action'),
						value: `${'Object action'}`,
					},
					{
						label: i18n.translate('other'),
						value: `${'Other'}`,
					},
					{
						label: i18n.translate('payment-method'),
						value: `${'Payment methods'}`,
					},
					{
						label: i18n.translate('prompt'),
						value: `${'Prompt'}`,
					},
					{
						label: i18n.translate('site-initializer'),
						value: `${'Site Initializer'}`,
					},
					{
						label: i18n.translate('theme'),
						value: `${'Theme'}`,
					},
					{
						label: i18n.translate('workflow-action'),
						value: `${'Workflow Action'}`,
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
					(item as ListTypeDefinition).listTypeEntries.map(
						(entry) => ({
							label: entry.name,
							value: entry.name,
						})
					),
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
	administratorOrders: {
		fields: [
			overrides(baseFilters.type, {
				label: i18n.translate('app-type'),
				name: 'orderTypeExternalReferenceCode',
				resource:
					'o/headless-commerce-admin-order/v1.0/order-types?pageSize=-1&sort=name:asc',
				transformData: (item: unknown) => {
					const {items = []} = item as {
						items?: {
							externalReferenceCode: string;
							name: {[locale: string]: string};
						}[];
					};

					return items.map(({externalReferenceCode, name}) => ({
						label: name?.en_US ?? externalReferenceCode,
						value: externalReferenceCode,
					}));
				},
				type: 'checkbox',
			}),
			overrides(baseFilters.status, {
				label: i18n.translate('order-status'),
				name: 'orderStatus',
				options: [
					{
						label: i18n.translate('canceled'),
						value: `${OrderWorkflowStatusCode.CANCELLED}`,
					},
					{
						label: i18n.translate('completed'),
						value: `${OrderWorkflowStatusCode.COMPLETED}`,
					},
					{
						label: i18n.translate('in-progress'),
						value: `${OrderWorkflowStatusCode.IN_PROGRESS}`,
					},
					{
						label: i18n.translate('on-hold'),
						value: `${OrderWorkflowStatusCode.ON_HOLD}`,
					},
					{
						label: i18n.translate('pending'),
						value: `${OrderWorkflowStatusCode.PENDING}`,
					},
					{
						label: i18n.translate('processing'),
						value: `${OrderWorkflowStatusCode.PROCESSING}`,
					},
				],
				removeQuoteMark: true,
				type: 'multiselect',
			}),
			baseFilters.dateCreated,
		],
		name: 'administratorOrders',
	},
	administratorPublishers: {
		fields: [
			overrides(baseFilters.type, {
				label: i18n.translate('account-type'),
				name: 'customFields/AccountType',
				options: [
					'Marketplace Developer',
					'Strategic Partner',
					'Technology Partner',
				],
				type: 'multiselect',
			}),
			overrides(baseFilters.dateCreated, {
				name: 'dateCreated',
			}),
		],
		name: 'administratorPublishers',
	},
	administratorSSATrials: {
		fields: [
			{
				label: i18n.translate('created-by'),
				name: 'author',
				operator: 'contains',
				type: 'text',
			},
			overrides(baseFilters.status, {
				label: 'Trial Status',
				name: 'orderStatus',
				operator: 'lambda',
				options: [
					{
						label: i18n.translate('active'),
						value: `${OrderWorkflowStatusCode.IN_PROGRESS}`,
					},
					{
						label: i18n.translate('expired'),
						value: `${OrderWorkflowStatusCode.COMPLETED}`,
					},
					{
						label: i18n.translate('processing'),
						value: `${OrderWorkflowStatusCode.PROCESSING}`,
					},
				],
				removeQuoteMark: true,
				type: 'multiselect',
			}),
		],
		name: 'administratorSSATrials',
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
	financeDashboardOrders: {
		fields: [
			baseFilters.dateCreated,
			overrides(baseFilters.dateCreated, {
				label: i18n.translate('modified-date'),
				name: 'modifiedDate',
			}),
			overrides(baseFilters.status, {
				label: i18n.translate('payment-status'),
				name: 'paymentStatusInfo/code',
				options: [
					{
						label: i18n.translate('canceled'),
						value: `${PaymentStatus.CANCELED}`,
					},
					{
						label: i18n.translate('failed'),
						value: `${PaymentStatus.FAILED}`,
					},
					{
						label: i18n.translate('paid'),
						value: `${PaymentStatus.PAID}`,
					},
					{
						label: i18n.translate('unpaid'),
						value: `${PaymentStatus.PENDING}`,
					},
				],
				removeQuoteMark: true,
				type: 'multiselect',
			}),
			overrides(baseFilters.status, {
				label: i18n.translate('order-status'),
				name: 'orderStatus',
				options: [
					{
						label: i18n.translate('canceled'),
						value: `${OrderWorkflowStatusCode.CANCELLED}`,
					},
					{
						label: i18n.translate('completed'),
						value: `${OrderWorkflowStatusCode.COMPLETED}`,
					},
					{
						label: i18n.translate('in-progress'),
						value: `${OrderWorkflowStatusCode.IN_PROGRESS}`,
					},
					{
						label: i18n.translate('on-hold'),
						value: `${OrderWorkflowStatusCode.ON_HOLD}`,
					},
					{
						label: i18n.translate('pending'),
						value: `${OrderWorkflowStatusCode.PENDING}`,
					},
					{
						label: i18n.translate('processing'),
						value: `${OrderWorkflowStatusCode.PROCESSING}`,
					},
				],
				removeQuoteMark: true,
				type: 'multiselect',
			}),
		],
		name: 'financeOrders',
	},
	financeDashboardPayments: {
		fields: [
			overrides(baseFilters.dateCreated, {
				name: 'dateCreated',
			}),
			overrides(baseFilters.status, {
				label: i18n.translate('payment-status'),
				name: 'paymentStatus',
				operator: 'eq',
				options: [
					{
						label: i18n.translate('paid'),
						value: PublisherPayoutStatus.PAID,
					},
					{
						label: i18n.translate('unpaid'),
						value: PublisherPayoutStatus.UNPAID,
					},
				],
				type: 'select',
			}),
		],
		name: 'financePayments',
	},
	publisherApps: {
		fields: [
			overrides(baseFilters.type, {
				label: i18n.translate('app-type'),
				name: 'specificationValues|appType',
				operator: 'lambda',
				options: [
					{
						label: i18n.translate('client-extension'),
						value: 'client-extension',
					},
					{
						label: i18n.translate('cloud-app'),
						value: 'cloud',
					},
					{
						label: i18n.translate('composite-app'),
						value: 'composite-app',
					},
					{
						label: i18n.translate('dxp-app'),
						value: 'dxp',
					},
					{
						label: i18n.translate('low-code-configuration'),
						value: 'low-code-configuration',
					},
					{
						label: i18n.translate('other'),
						value: 'other',
					},
				],
				type: 'checkbox',
			}),
			overrides(baseFilters.version, {
				label: i18n.translate('liferay-version'),
				name: 'specificationValues|liferayVersion',
				operator: 'lambda',
				resource: `o/headless-admin-list-type/v1.0/list-type-definitions/by-external-reference-code/${LIFERAY_VERSION_PICKLIST}`,
				transformData: (item) =>
					(item as ListTypeDefinition).listTypeEntries.map(
						(entry) => ({
							label: entry.name,
							value: entry.name,
						})
					),
				type: 'multiselect',
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
		name: 'publisherApps',
	},
	publisherSolutions: {
		fields: [
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
		name: 'publisherSolutions',
	},
};

export type FilterSchemaOption = keyof typeof filterSchema;
