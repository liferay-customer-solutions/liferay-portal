/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Button} from '..';
import classNames from 'classnames';
import {useState} from 'react';

interface GroupButton {
	label: string;
	value: string;
}

interface RoundedGroupButtonsProps {
	groupButtons: GroupButton[];
	handleOnChange: (value: string) => void;
	id?: string;
}

const RoundedGroupButtons: React.FC<RoundedGroupButtonsProps> = ({
	groupButtons,
	handleOnChange,
	id,
	...props
}) => {
	const [selectedButton, setSelectedButton] = useState(
		groupButtons[0]?.value
	);

	return (
		<div
			className="bg-neutral-1 border border-light btn-group rounded-pill"
			id={id}
			role="group"
		>
			{groupButtons?.map(({label, value}, index) => (
				<Button
					className={classNames('btn px-4 py-1 rounded-pill', {
						'bg-transparent text-neutral-4':
							selectedButton !== value,
						'bg-white border border-primary label-primary text-brand-primary':
							selectedButton === value,
					})}
					key={`${index}-${value}`}
					onClick={() => {
						setSelectedButton(value);
						handleOnChange(value);
					}}
					value={value}
					{...props}
				>
					{label}
				</Button>
			))}
		</div>
	);
};

export default RoundedGroupButtons;
