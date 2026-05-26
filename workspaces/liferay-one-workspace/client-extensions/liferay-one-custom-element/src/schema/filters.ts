/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Params} from 'react-router-dom';

import SearchBuilder, {Operators} from '../core/SearchBuilder';
import {OrderWorkflowStatusCode} from '../enums/Order';
import i18n from '../i18n';

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

const baseFilters: Filter = {
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
};

const overrides = (
	object: RendererFields,
	newObject: Partial<RendererFields>
) => ({
	...object,
	...newObject,
});

export const filterSchema: FilterSchemas = {
	administratorOrders: {
		fields: [
			overrides(baseFilters.type, {
				label: i18n.translate('app-type'),
				name: 'orderTypeExternalReferenceCode',
				resource:
					'o/headless-commerce-admin-order/v1.0/order-types?pageSize=-1&sort=name:asc',
				transformData: ({items = []}) =>
					items.map(
						({
							externalReferenceCode,
							name,
						}: {
							externalReferenceCode: string;
							name: {[locale: string]: string};
						}) => ({
							label: name?.en_US ?? externalReferenceCode,
							value: externalReferenceCode,
						})
					),
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
};

export type FilterSchemaOption = keyof typeof filterSchema;
