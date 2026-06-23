/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import React, {memo} from 'react';
import {Params} from 'react-router-dom';
import Checkbox from '~/components/Checkbox/Checkbox';
import DateRange from '~/components/DateRange/DateRange';
import FormFieldInput from '~/components/FormFieldInput/FormFieldInput';
import FormMultiSelect from '~/components/FormMultiSelect/FormMultiSelect';
import FormSelect from '~/components/FormSelect/FormSelect';
import i18n from '~/i18n';
import {Operators} from '~/utils/SearchBuilder';

type AutoCompleteProps = {
	label?: string;
	onSearch: (keyword: string) => unknown;
	resource?: ((params: Readonly<Params<string>>) => string) | string;
	transformData?: (item: unknown) => unknown;
};

type RenderedFieldOptions = string[] | {label: string; value: string}[];

export type RendererFields = {
	disabled?: boolean;
	isCustomFilter?: boolean;
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

export type Options = {
	label: string;
	value: string;
};

export type FieldOptions = {[key: string]: unknown[]};

type RendererProps = {
	fieldOptions?: FieldOptions;
	fields: RendererFields[];
	filterSchema: string;
	form: Record<string, unknown>;
	isLoading?: boolean;
	onApply: () => void;
	onChange: (event: {
		target: {name: string; type?: string; value: unknown};
	}) => void;
};

const RenderField = ({
	field,
	fieldOptions,
	form,
	isLoading = false,
	onApply,
	onChange,
}: Omit<RendererProps, 'fields'> & {field: RendererFields}) => {
	const {disabled, label, name, options = [], type} = field;

	const currentValue = form[name];

	const isFieldDisabled = () =>
		disabled ??
		(currentValue as string).includes(i18n.sub('no-x', field.label));

	const getFieldValue = () =>
		currentValue === i18n.sub('no-x', field.label)
			? ''
			: (currentValue as string);

	const getOptions = () =>
		fieldOptions?.[name] ||
		(options || []).map((option) =>
			typeof option === 'object'
				? option
				: {
						label: option,
						value: option,
					}
		);

	if (type === 'date-range') {
		return (
			<div className="my-1">
				<DateRange
					label={label}
					onChange={(
						value: string,
						setValue: React.Dispatch<React.SetStateAction<string>>
					) => {
						setValue(value);

						onChange({
							target: {
								name,
								type: 'date-range',
								value,
							},
						});
					}}
					value={
						(currentValue as Array<{value?: string}>)[0]?.value ||
						(currentValue as string)
					}
				/>
			</div>
		);
	}

	if (['date', 'number', 'text', 'textarea'].includes(type)) {
		return (
			<FormFieldInput
				disabled={isFieldDisabled()}
				name={name}
				onChange={onChange}
				onKeyDown={(event) => {
					if (event.key === 'Enter' && type !== 'textarea') {
						onApply();
					}
				}}
				value={getFieldValue()}
				{...(field as Record<string, unknown>)}
			/>
		);
	}

	if (type === 'select') {
		return (
			<FormSelect
				disabled={disabled}
				isLoading={isLoading}
				label={label}
				name={name}
				onChange={onChange}
				options={
					getOptions() as {label: string; value: string | number}[]
				}
				value={
					(currentValue as Array<{value?: string}>)[0]?.value ||
					(currentValue as string)
				}
			/>
		);
	}

	if (type === 'checkbox') {
		const onCheckboxChange = (event: {
			target: {labels?: NodeList; name: string; value: string};
		}) => {
			const labelValue = (event.target.labels?.[0] as HTMLLabelElement)
				?.innerText;
			const inputValue = event.target.value;

			const formValue: unknown[] = Array.isArray(form[name])
				? [...form[name]]
				: [];

			const simplifiedFormValue = formValue.map((item) =>
				typeof item === 'object' && item !== null
					? (item as {value: unknown}).value
					: item
			);

			const newEntry =
				inputValue !== labelValue
					? {label: labelValue, value: inputValue}
					: inputValue;

			const isSelected = simplifiedFormValue.includes(inputValue);

			const newValue = isSelected
				? formValue.filter((item) =>
						typeof item === 'object' && item !== null
							? (item as {value: unknown}).value !== inputValue
							: item !== inputValue
					)
				: [...formValue, newEntry];

			onChange({
				target: {
					name,
					value: newValue,
				},
			});
		};

		return (
			<div>
				<label>{label}</label>

				{getOptions().map((option, index) => {
					const optionValue =
						typeof option === 'string'
							? option
							: (option as {value: string}).value;

					return (
						<Checkbox
							checked={
								Array.isArray(form[name]) &&
								form[name].some((option: Options | string) =>
									typeof option === 'string'
										? option === optionValue
										: option.value === optionValue
								)
							}
							disabled={disabled}
							key={index}
							label={
								typeof option === 'string'
									? option
									: (option as {label: string}).label
							}
							name={name}
							onChange={
								onCheckboxChange as React.ChangeEventHandler<HTMLInputElement>
							}
							value={optionValue}
						/>
					);
				})}
			</div>
		);
	}

	if (type === 'multiselect') {
		return (
			<div className="my-2">
				<label>{label}</label>

				<FormMultiSelect
					disabled={disabled}
					isLoading={field.resource ? isLoading : false}
					name={name}
					onChange={onChange}
					options={getOptions() as Options[]}
					value={currentValue as string | undefined}
				/>
			</div>
		);
	}

	return null;
};

const Renderer: React.FC<RendererProps> = ({fields, ...otherProps}) => (
	<div className="form-renderer">
		{fields.map((field, index) => (
			<div className="mb-4" key={index}>
				<RenderField {...otherProps} field={field} />
			</div>
		))}
	</div>
);

export default memo(Renderer);
