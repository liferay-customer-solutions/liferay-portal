/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {InputHTMLAttributes, useEffect, useRef, useState} from 'react';
import ReactSelect, {GroupBase, PropsValue, SelectInstance} from 'react-select';
import BaseWrapper from '~/components/BaseWrapper/BaseWrapper';

type Option = {label: string; value: string};

type MultiSelectProps = {
	isLoading: boolean;
	label?: string;
	options: Option[];
} & InputHTMLAttributes<HTMLSelectElement>;

const FormMultiSelect: React.FC<MultiSelectProps> = ({
	disabled,
	isLoading,
	label,
	name = '',
	onChange,
	options,
	value,
}) => {
	const [visible, setVisible] = useState(false);
	const multiselectRef = useRef<{blur?: () => void} | null>(null);

	useEffect(() => {
		const current = multiselectRef.current;

		if (!visible) {
			current?.blur?.();
		}
	}, [visible]);

	return (
		<BaseWrapper boldLabel label={label}>
			<ReactSelect
				classNamePrefix="marketplace-multi-select"
				closeMenuOnSelect
				isDisabled={disabled}
				isLoading={isLoading}
				isMulti
				menuIsOpen={visible}
				menuPosition="fixed"
				menuShouldBlockScroll
				name={name}
				onBlur={() => setVisible(false)}
				onChange={(value) => {
					if (onChange) {
						onChange({
							target: {name, value},
						} as unknown as React.ChangeEvent<HTMLSelectElement>);
					}
				}}
				onFocus={() => !visible && setVisible(true)}
				onKeyDown={(event) => {
					if (event.key === 'Escape' && visible === true) {
						event.stopPropagation();
						setVisible(false);
					}

					return;
				}}
				onMenuClose={() => setVisible(false)}
				openMenuOnClick
				options={options}
				ref={
					multiselectRef as unknown as React.RefObject<
						SelectInstance<unknown, true, GroupBase<unknown>>
					>
				}
				tabSelectsValue={false}
				value={value as PropsValue<unknown>}
			/>
		</BaseWrapper>
	);
};

export default FormMultiSelect;
