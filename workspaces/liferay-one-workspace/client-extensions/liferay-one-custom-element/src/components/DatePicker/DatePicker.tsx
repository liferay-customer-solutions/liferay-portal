/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayDatePicker from '@clayui/date-picker';
import {IYears} from '@clayui/date-picker/lib/types';
import ClayForm from '@clayui/form';
import ClayIcon from '@clayui/icon';
import classNames from 'classnames';
import Badge from '~/components/Badge/Badge';
import {getIconSpriteMap} from '~/services/liferay/liferay';

import './DatePicker.css';

interface IProps {
	badgeClassName?: string;
	className?: string;
	dateFormat?: string;
	error?: string;
	groupStyle?: string;
	helper?: string;
	id?: string;
	label?: string;
	onBlur?: () => void;
	onChange?: (date: string) => void;
	placeholder?: string;
	required?: boolean;
	value?: string;
	years?: IYears;
	yearsCheck?: boolean;
}

const DatePicker: React.FC<IProps> = ({
	badgeClassName,
	className,
	dateFormat = 'MM/dd/yyyy',
	error,
	groupStyle,
	helper,
	id,
	label,
	onBlur,
	onChange,
	placeholder,
	required,
	value = '',
	years,
	yearsCheck,
}) => {
	const handleChange = (nextValue: string) => {
		if (onChange) {
			onChange(nextValue);
		}
	};

	return (
		<ClayForm.Group
			className={classNames(
				'w-100',
				error && 'has-error',
				className,
				groupStyle
			)}
		>
			<label>
				{label}

				{required && (
					<span className="inline-item-after reference-mark text-warning">
						<ClayIcon symbol="asterisk" />
					</span>
				)}
				<ClayDatePicker
					dateFormat={dateFormat}
					id={id}
					onBlur={onBlur}
					onChange={handleChange}
					placeholder={placeholder}
					spritemap={getIconSpriteMap()}
					value={value}
					years={years}
					yearsCheck={yearsCheck}
				/>
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

export default DatePicker;
