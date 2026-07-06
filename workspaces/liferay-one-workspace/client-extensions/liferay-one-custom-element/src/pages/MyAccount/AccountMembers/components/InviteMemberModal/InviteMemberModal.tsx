/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ClayInput} from '@clayui/form';
import {useState} from 'react';
import {FieldBase} from '~/components/FieldBase/FieldBase';
import i18n from '~/i18n';
import MultiSelect from '~/pages/Admin/SSADashboard/components/MultiSelect/MultiSelect';
import HeadlessAdminUser from '~/services/headless/HeadlessAdminUser';
import {Liferay} from '~/services/liferay/liferay';

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

type RoleItem = {label: string; value: string};

type InviteMemberModalProps = {
	accountExternalReferenceCode: string;
	mutate: () => Promise<unknown>;
	onClose: () => void;
	roleNames: string[];
};

const InviteMemberModal = ({
	accountExternalReferenceCode,
	mutate,
	onClose,
	roleNames,
}: InviteMemberModalProps) => {
	const [emailAddress, setEmailAddress] = useState('');
	const [emailError, setEmailError] = useState('');
	const [selectedRoles, setSelectedRoles] = useState<RoleItem[]>([]);

	const sourceItems: RoleItem[] = roleNames
		.filter(
			(roleName) =>
				!selectedRoles.some((selected) => selected.value === roleName)
		)
		.map((roleName) => ({label: roleName, value: roleName}));

	const onSubmit = async (event: React.FormEvent) => {
		event.preventDefault();

		const trimmedEmail = emailAddress.trim();

		if (!EMAIL_PATTERN.test(trimmedEmail)) {
			setEmailError(i18n.translate('please-enter-a-valid-email-address'));

			return;
		}

		try {
			const userAccount =
				await HeadlessAdminUser.postAccountUserAccountByEmailAddress(
					accountExternalReferenceCode,
					trimmedEmail
				);

			if (selectedRoles.length) {
				const {items: accountRoles} =
					await HeadlessAdminUser.getAccountRoles(
						accountExternalReferenceCode
					);

				const selectedRoleNames = new Set(
					selectedRoles.map((role) => role.value)
				);

				await Promise.all(
					accountRoles
						.filter((accountRole) =>
							selectedRoleNames.has(accountRole.name)
						)
						.map((accountRole) =>
							HeadlessAdminUser.sendRoleAccountUser(
								accountRole.accountId,
								accountRole.id,
								userAccount.id
							)
						)
				);
			}

			await mutate();

			Liferay.Util.openToast({
				message: i18n.translate('member-successfully-invited'),
				title: i18n.translate('success'),
			});

			onClose();
		}
		catch {
			Liferay.Util.openToast({
				message: i18n.translate('unable-to-invite-member'),
				title: i18n.translate('error'),
				type: 'danger',
			});
		}
	};

	return (
		<form id="invite-member" onSubmit={onSubmit}>
			<p>
				{i18n.translate(
					'invite-a-new-member-by-email-address-they-will-be-added-to-the-account-once-they-accept-the-invitation'
				)}
			</p>

			<FieldBase
				errorMessage={emailError}
				label={i18n.translate('email-address')}
				required
			>
				<ClayInput
					onChange={(event) => {
						setEmailError('');
						setEmailAddress(event.target.value);
					}}
					placeholder={i18n.translate('name-example-com')}
					type="email"
					value={emailAddress}
				/>
			</FieldBase>

			<MultiSelect
				inputName={i18n.translate('roles')}
				label={i18n.translate('roles')}
				multiselectKey={`invite-roles-${selectedRoles.length}`}
				onItemsChange={(roles) => setSelectedRoles(roles as RoleItem[])}
				selectedItems={selectedRoles}
				sourceItems={sourceItems}
			/>
		</form>
	);
};

export default InviteMemberModal;
