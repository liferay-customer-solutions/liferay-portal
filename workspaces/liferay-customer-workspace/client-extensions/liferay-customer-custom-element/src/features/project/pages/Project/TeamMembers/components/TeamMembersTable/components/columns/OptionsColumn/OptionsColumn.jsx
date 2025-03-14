/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';
import ClayDropDown from '@clayui/drop-down';
import ClayIcon from '@clayui/icon';
import {ClayTooltipProvider} from '@clayui/tooltip';
import i18n from '~/utils/I18n';
import MenuUserActions from './components/MenuUserActions';

const OptionsColumn = ({
	edit,
	highPriorityContactsNames,
	onCancel,
	onEdit,
	onRemove,
	onSave,
	saveDisabled,
	userAccount,
}) => {
	const userOptions = [
		{
			customOptionStyle: 'pr-5',
			label: i18n.translate('edit'),
			onClick: () => {
				onEdit();
			},
		},
		{
			customOptionStyle: 'pr-5',
			disabled: highPriorityContactsNames.includes(
				userAccount.emailAddress
			),
			label: i18n.translate('remove'),
			onClick: () => onRemove(),
			tooltip: i18n.translate(
				'this-team-member-is-assigned-as-an-incident-contact-and-cannot-be-removed'
			),
		},
	];

	return edit ? (
		<MenuUserActions
			onCancel={() => onCancel()}
			onSave={() => onSave()}
			saveDisabled={saveDisabled}
		/>
	) : (
		<ClayTooltipProvider>
        			<ClayDropDown
        				trigger={
        					<ClayButton
        						aria-label={i18n.translate('manage-user-options')}
        						displayType="unstyled"
        						small
        					>
        						<ClayIcon symbol="ellipsis-v" />
        					</ClayButton>
        				}
        				menuElementAttrs={{className: 'p-0'}}
        				menuWidth="shrink"
        			>
        				{userOptions.map(({label, onClick, disabled, tooltip}, index) => (
        					<ClayDropDown.Item key={index} onClick={onClick} disabled={disabled}>
        						{tooltip ? (
        							<span title={tooltip}>{label}</span>
        						) : (
        							label
        						)}
        					</ClayDropDown.Item>
        				))}
        			</ClayDropDown>
        		</ClayTooltipProvider>
	);
};

export default OptionsColumn;
