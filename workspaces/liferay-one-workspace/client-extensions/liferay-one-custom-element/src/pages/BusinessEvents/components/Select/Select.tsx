/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayForm, {ClaySelect} from '@clayui/form';
import ClayIcon from '@clayui/icon';
import classNames from 'classnames';
import Badge from '~/components/Badge/Badge';

import './Select.css';

export interface IOption {
	disabled?: boolean;
	label: string;
	value: string;
}

interface IProps {
	badgeClassName?: string;
	className?: string;
	error?: string;
	groupStyle?: string;
	helper?: string;
	id?: string;
	label: string;
	onBlur?: () => void;
	onChange?: (value: string) => void;
	options: IOption[];
	required?: boolean;
	value?: string;
}

const Select: React.FC<IProps> = ({
	badgeClassName,
	error,
	groupStyle,
	helper,
	id,
	label,
	onBlur,
	onChange,
	options,
	required,
	value = '',
}) => {
	const handleChange = (event: React.ChangeEvent<HTMLSelectElement>) => {
		if (onChange) {
			onChange(event.target.value);
		}
	};

	return (
		<ClayForm.Group
			className={classNames('w-100', error && 'has-error', groupStyle)}
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
						onBlur={onBlur}
						onChange={handleChange}
						value={value}
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

			{error && required && (
				<Badge badgeClassName={badgeClassName}>
					<span className="pl-1">{error}</span>
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
