/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ClayInput} from '@clayui/form';
import {InputHTMLAttributes, forwardRef} from 'react';
import BaseWrapper from '~/components/BaseWrapper/BaseWrapper';

type InputProps = {
	disabled?: boolean;
	errors?: Record<string, {message?: string}>;
	id?: string;
	label?: string;
	name: string;
	register?: (
		name: string,
		options?: Record<string, unknown>
	) => Record<string, unknown> | void;
	required?: boolean;
	type?: string;
} & InputHTMLAttributes<HTMLInputElement>;

const FormFieldInput = forwardRef<HTMLInputElement, InputProps>(
	(
		{
			disabled = false,
			errors = {},
			label,
			name,
			register = () => {},
			id = name,
			type,
			value,
			required = false,
			onBlur,
			...otherProps
		},
		ref
	) => (
		<BaseWrapper
			boldLabel
			disabled={disabled}
			error={errors[name]?.message}
			id={id}
			label={label}
			required={required}
		>
			<ClayInput
				className="rounded-xs"
				component={type === 'textarea' ? 'textarea' : 'input'}
				disabled={disabled}
				id={id}
				name={name}
				ref={ref}
				type={type}
				value={value}
				{...otherProps}
				{...register(name, {onBlur, required})}
			/>
		</BaseWrapper>
	)
);

export default FormFieldInput;
