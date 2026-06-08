/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayForm, {ClaySelect} from '@clayui/form';
import ClayIcon from '@clayui/icon';
import classNames from 'classnames';
import {useField} from 'formik';

import {
	required as requiredValidation,
	validate,
} from '../../../../../utils/validations.form';
import Badge from '../../../../../components/Badge/Badge';

import './Select.css';

export interface IOption {
	disabled?: boolean;
	label: string;
	value: string;
}

interface IProps {
	badgeClassName?: string;
	className?: string;
	groupStyle?: string;
	helper?: string;
	id?: string;
	label: string;
	name: string;
	onBlur?: () => void;
	onChange?: (value: string) => void;
	options: IOption[];
	required?: boolean;
	validations?: Function[];
}

const Select: React.FC<IProps> = ({
	badgeClassName,
	className,
	groupStyle,
	helper,
	id,
	label,
	name,
	onChange,
	onBlur,
	options,
	required,
	validations = [],
}) => {
	if (required) {
		validations = validations
			? [...validations, (value: string) => requiredValidation(value)]
			: [(value: string) => requiredValidation(value)];
	}

	const [field, meta, helpers] = useField({
		className,
		id,
		name,
		required,
		validate: (value) => validate(validations, value),
	});

	const getStyleStatus = () => {
		if (meta.touched) {
			return meta.error ? 'has-error' : 'has-success';
		}

		return;
	};

	const handleBlur = () => {
		helpers.setTouched(true);

		if (onBlur) {
			onBlur();
		}
	};

	const handleChange = (event: React.ChangeEvent<HTMLSelectElement>) => {
		const value = event.target.value;

		helpers.setValue(value);

		if (onChange) {
			onChange(value);
		}
	};

	return (
		<ClayForm.Group
			className={classNames('w-100', getStyleStatus(), groupStyle)}
		>
			<label>
				{label}

				{required && (
					<span className="inline-item-after reference-mark text-warning">
						<ClayIcon symbol="asterisk" />
					</span>
				)}

				<div className="position-relative">
					<ClayIcon className="select-icon" symbol="caret-bottom" />

					<ClaySelect
						aria-label={label}
						id={id}
						name={name}
						onBlur={handleBlur}
						onChange={handleChange}
						value={field.value}
					>
						{options.map(({disabled, label, value}, index) => (
							<ClaySelect.Option
								disabled={disabled}
								key={`${value}-${index}`}
								label={label}
								value={value}
							/>
						))}
					</ClaySelect>
				</div>
			</label>

			{meta.touched && meta.error && required && (
				<Badge badgeClassName={badgeClassName}>
					<span className="pl-1">{meta.error}</span>
				</Badge>
			)}

			{helper && (
				<div className="ml-3 pl-3 text-neutral-6 text-paragraph-sm">
					{helper}
				</div>
			)}
		</ClayForm.Group>
	);
};

export default Select;
