/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayDropDown from '@clayui/drop-down';
import {ClayCheckbox} from '@clayui/form';
import ClayIcon from '@clayui/icon';
import {useState} from 'react';
import {useMeasuredWidth} from '~/hooks/useMeasuredWidth';
import i18n, {translate} from '~/i18n';
import {
	getMembershipRoleNames,
	hasAdministratorRole,
	isAdministratorRole,
} from '~/pages/MyAccount/AccountMembers/accountRoles';
import HeadlessAdminUser from '~/services/headless/HeadlessAdminUser';
import {Liferay} from '~/services/liferay/liferay';

import '../../AccountMembers.css';

import type {RoleBrief} from '~/types/accounts';

type EditPermissionsModalProps = {
	accountExternalReferenceCode: string;
	accountId: number | string;
	adminCount: number;
	memberName: string;
	memberRoleBriefs: RoleBrief[];
	mutate: () => Promise<unknown>;
	onClose: () => void;
	roleNames: string[];
	userId: number;
};

const EditPermissionsModal = ({
	accountExternalReferenceCode,
	accountId,
	adminCount,
	memberName,
	memberRoleBriefs,
	mutate,
	onClose,
	roleNames,
	userId,
}: EditPermissionsModalProps) => {
	const currentRoleNames = getMembershipRoleNames(memberRoleBriefs);

	const isLastAdmin =
		hasAdministratorRole(memberRoleBriefs) && adminCount <= 1;

	const [active, setActive] = useState(false);
	const [selectedRoles, setSelectedRoles] =
		useState<string[]>(currentRoleNames);

	const {ref: rolesRef, width: menuWidth} =
		useMeasuredWidth<HTMLDivElement>(active);

	const isRoleLocked = (roleName: string) =>
		isLastAdmin &&
		isAdministratorRole(roleName) &&
		currentRoleNames.includes(roleName);

	const toggleRole = (roleName: string) => {
		if (isRoleLocked(roleName)) {
			return;
		}

		setSelectedRoles((previous) =>
			previous.includes(roleName)
				? previous.filter((value) => value !== roleName)
				: [...previous, roleName]
		);
	};

	const clearRoles = () =>
		setSelectedRoles(
			selectedRoles.filter((roleName) => isRoleLocked(roleName))
		);

	const triggerLabel = selectedRoles.length
		? selectedRoles.join(', ')
		: translate('none');

	const onSubmit = async (event: React.FormEvent) => {
		event.preventDefault();

		if (isLastAdmin && !selectedRoles.some(isAdministratorRole)) {
			Liferay.Util.openToast({
				message: i18n.translate(
					'at-least-one-account-admin-is-required-assign-another-account-admin-before-changing-this-role'
				),
				title: i18n.translate('error'),
				type: 'danger',
			});

			return;
		}

		try {
			const {items: accountRoles} =
				await HeadlessAdminUser.getAccountRoles(
					accountExternalReferenceCode
				);

			const currentRoleNameSet = new Set(currentRoleNames);
			const selectedRoleNameSet = new Set(selectedRoles);

			const rolesToAdd = accountRoles.filter(
				(accountRole) =>
					!currentRoleNameSet.has(accountRole.name) &&
					selectedRoleNameSet.has(accountRole.name)
			);

			const rolesToRemove = accountRoles.filter(
				(accountRole) =>
					currentRoleNameSet.has(accountRole.name) &&
					!selectedRoleNameSet.has(accountRole.name)
			);

			await Promise.all([
				...rolesToAdd.map((accountRole) =>
					HeadlessAdminUser.sendRoleAccountUser(
						accountId,
						accountRole.id,
						userId
					)
				),
				...rolesToRemove.map((accountRole) =>
					HeadlessAdminUser.deleteRoleAccountUser(
						accountId,
						accountRole.id,
						userId
					)
				),
			]);

			await mutate();

			Liferay.Util.openToast({
				message: i18n.translate('permissions-successfully-updated'),
				title: i18n.translate('success'),
			});

			onClose();
		}
		catch {
			Liferay.Util.openToast({
				message: i18n.translate('unable-to-update-permissions'),
				title: i18n.translate('error'),
				type: 'danger',
			});
		}
	};

	return (
		<form id="edit-permissions" onSubmit={onSubmit}>
			<p className="text-neutral-7">
				{translate(
					'team-members-will-receive-an-email-invitation-about-the-permissions-changes'
				)}
			</p>

			<label className="account-members-roles-label">
				{i18n.sub('x-roles', memberName)}
			</label>

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
							<span>{triggerLabel}</span>

							<ClayIcon symbol="caret-bottom" />
						</button>
					}
				>
					<ClayDropDown.ItemList>
						{roleNames.map((roleName) => (
							<ClayDropDown.Item
								disabled={isRoleLocked(roleName)}
								key={roleName}
								onClick={() => toggleRole(roleName)}
							>
								<ClayCheckbox
									checked={selectedRoles.includes(roleName)}
									disabled={isRoleLocked(roleName)}
									label={roleName}
									onChange={() => toggleRole(roleName)}
								/>
							</ClayDropDown.Item>
						))}

						<ClayDropDown.Item
							disabled={isLastAdmin}
							onClick={clearRoles}
						>
							<ClayCheckbox
								checked={!selectedRoles.length}
								disabled={isLastAdmin}
								label={translate('none')}
								onChange={clearRoles}
							/>
						</ClayDropDown.Item>
					</ClayDropDown.ItemList>
				</ClayDropDown>
			</div>

			{isLastAdmin && (
				<p className="mt-2 text-neutral-7">
					{translate(
						'at-least-one-account-admin-is-required-assign-another-account-admin-before-changing-this-role'
					)}
				</p>
			)}
		</form>
	);
};

export default EditPermissionsModal;
