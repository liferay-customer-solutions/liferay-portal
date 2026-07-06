/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';
import ClayDropDown from '@clayui/drop-down';
import ClayIcon from '@clayui/icon';
import {useState} from 'react';
import {Word, translate} from '~/i18n';

export type RowAction = {
	label: Word;
	onClick?: () => void;
};

type RowActionsMenuProps = {
	actions: RowAction[];
};

export default function RowActionsMenu({actions}: RowActionsMenuProps) {
	const [active, setActive] = useState(false);

	return (
		<ClayDropDown
			active={active}
			onActiveChange={setActive}
			trigger={
				<ClayButton
					aria-label={translate('actions')}
					borderless
					className="text-neutral-7"
					displayType="unstyled"
					onClick={(event) => event.stopPropagation()}
				>
					<ClayIcon symbol="ellipsis-v" />
				</ClayButton>
			}
		>
			<ClayDropDown.ItemList>
				{actions.map((action) => (
					<ClayDropDown.Item
						key={action.label}
						onClick={(event) => {
							event.stopPropagation();
							setActive(false);
							action.onClick?.();
						}}
					>
						{translate(action.label)}
					</ClayDropDown.Item>
				))}
			</ClayDropDown.ItemList>
		</ClayDropDown>
	);
}
