/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import classNames from 'classnames';
import {SelectHTMLAttributes} from 'react';
import BaseWrapper from '~/components/BaseWrapper/BaseWrapper';

type InputSelectProps = {
	className?: string;
	defaultOption?: boolean;
	disabled?: boolean;
	errors?: Record<string, {message?: string}>;
	forceSelectOption?: boolean;
	id?: string;
	isLoading?: boolean;
	label?: string;
	name: string;
	options: {label: string; value: string | number}[];
	register?: (
		name: string,
		options?: Record<string, unknown>
	) => Record<string, unknown> | void;
	registerOptions?: Record<string, unknown>;
	required?: boolean;
	type?: string;
} & SelectHTMLAttributes<HTMLSelectElement>;

const FormSelect: React.FC<InputSelectProps> = ({
	className,
	disabled = false,
	registerOptions,
	defaultOption = true,
	errors = {},
	defaultValue,
	label,
	name,
	register = () => {},
	id = name,
	isLoading,
	options,
	forceSelectOption = false,
	required = false,
	...otherProps
}) => {
	return (
		<BaseWrapper
			boldLabel
			disabled={disabled}
			error={errors[name]?.message}
			label={label}
			required={required}
		>
			<select
				className={classNames('form-control rounded-xs', className)}
				defaultValue={defaultValue}
				disabled={disabled}
				id={id}
				name={name}
				{...otherProps}
				{...register(name, {required, ...registerOptions})}
			>
				{defaultOption && <option value=""></option>}

				{isLoading ? (
					<option value="">Loading...</option>
				) : (
					options?.map(({label, value}, index) => {
						const valueOption =
							name.includes('teamToComponents/name') ||
							name.includes('componentToCaseResult/name')
								? label
								: value;

						return (
							<option
								key={index}
								label={label}
								selected={
									forceSelectOption
										? value === defaultValue
										: undefined
								}
								value={valueOption}
							>
								{label}
							</option>
						);
					})
				)}
			</select>
		</BaseWrapper>
	);
};

export default FormSelect;
