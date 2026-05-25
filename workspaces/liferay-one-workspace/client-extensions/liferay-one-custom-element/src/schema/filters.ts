/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Operators} from '../core/SearchBuilder';
import SearchBuilder from '../core/SearchBuilder';

export type AppliedFilters = {
	label: string;
	value: string;
};

export type RenderedFieldOptions = string[] | AppliedFilters[];

type AutoCompleteProps = {
	label?: string;
	onSearch: (keyword: string) => any;
	resource?: string | ((params: Readonly<Record<string, string>>) => string);
	transformData?: (item: any) => any;
};

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

export const filterSchema: FilterSchemas = {};

export type FilterSchemaOption = string;
