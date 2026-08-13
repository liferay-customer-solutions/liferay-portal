/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayDropDown from '@clayui/drop-down';
import {ClayCheckbox} from '@clayui/form';
import ClayIcon from '@clayui/icon';
import {useState} from 'react';
import {useMeasuredWidth} from '~/hooks/useMeasuredWidth';
import {translate} from '~/i18n';
import {isPartnerRole} from '~/pages/MyAccount/AccountMembers/accountRoles';

import '../../AccountMembers.css';

type AccountRolesSelectProps = {
	onClearRoles: () => void;
	onToggleRole: (roleName: string) => void;
	roleNames: string[];
	selectedRoleNames: string[];
};

const AccountRolesSelect = ({
	onClearRoles,
	onToggleRole,
	roleNames,
	selectedRoleNames,
}: AccountRolesSelectProps) => {
	const [active, setActive] = useState(false);

	const {ref: rolesRef, width: menuWidth} =
		useMeasuredWidth<HTMLDivElement>(active);

	const partnerRoleNames = roleNames.filter((roleName) =>
		isPartnerRole(roleName)
	);
	const standardRoleNames = roleNames.filter(
		(roleName) => !isPartnerRole(roleName)
	);

	const hasPartnerRoles = !!partnerRoleNames.length;

	const renderItem = (roleName: string) => (
		<ClayDropDown.Item
			key={roleName}
			onClick={() => onToggleRole(roleName)}
		>
			<ClayCheckbox
				checked={selectedRoleNames.includes(roleName)}
				label={roleName}
				onChange={() => onToggleRole(roleName)}
			/>
		</ClayDropDown.Item>
	);

	return (
		<div ref={rolesRef}>
			<ClayDropDown
				active={active}
				className="account-members-roles-dropdown"
				closeOnClick={false}
				menuElementAttrs={{
					className: 'account-members-roles-menu',
					style: menuWidth ? {width: menuWidth} : undefined,
				}}
				onActiveChange={setActive}
				trigger={
					<button
						className="account-members-roles-trigger align-items-center d-flex form-control justify-content-between"
						type="button"
					>
						<span>
							{selectedRoleNames.length
								? selectedRoleNames.join(', ')
								: translate('none')}
						</span>

						<ClayIcon symbol="caret-bottom" />
					</button>
				}
			>
				<ClayDropDown.ItemList>
					{hasPartnerRoles && (
						<li className="dropdown-subheader">
							{translate('account-roles')}
						</li>
					)}

					{standardRoleNames.map(renderItem)}

					{hasPartnerRoles && (
						<>
							<ClayDropDown.Divider />

							<li className="dropdown-subheader">
								{translate('partner-roles')}
							</li>

							{partnerRoleNames.map(renderItem)}
						</>
					)}

					<ClayDropDown.Divider />

					<ClayDropDown.Item onClick={onClearRoles}>
						<ClayCheckbox
							checked={!selectedRoleNames.length}
							label={translate('none')}
							onChange={onClearRoles}
						/>
					</ClayDropDown.Item>
				</ClayDropDown.ItemList>
			</ClayDropDown>
		</div>
	);
};

export default AccountRolesSelect;
