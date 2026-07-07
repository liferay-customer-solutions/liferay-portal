/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayDropDown from '@clayui/drop-down';
import {ClayCheckbox, ClayInput} from '@clayui/form';
import ClayIcon from '@clayui/icon';
import {useState} from 'react';
import {FieldBase} from '~/components/FieldBase/FieldBase';
import {useMeasuredWidth} from '~/hooks/useMeasuredWidth';
import i18n, {translate} from '~/i18n';
import HeadlessAdminUser from '~/services/headless/HeadlessAdminUser';
import {Liferay} from '~/services/liferay/liferay';

import '../../AccountMembers.css';

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

type InviteMemberModalProps = {
	accountExternalReferenceCode: string;
	accountId: number | string;
	mutate: () => Promise<unknown>;
	onClose: () => void;
	roleNames: string[];
};

const InviteMemberModal = ({
	accountExternalReferenceCode,
	accountId,
	mutate,
	onClose,
	roleNames,
}: InviteMemberModalProps) => {
	const [active, setActive] = useState(false);
	const [emailAddress, setEmailAddress] = useState('');
	const [emailError, setEmailError] = useState('');
	const [selectedRoles, setSelectedRoles] = useState<string[]>([]);

	const {ref: rolesRef, width: menuWidth} =
		useMeasuredWidth<HTMLDivElement>(active);

	const toggleRole = (roleName: string) =>
		setSelectedRoles((previous) =>
			previous.includes(roleName)
				? previous.filter((value) => value !== roleName)
				: [...previous, roleName]
		);

	const triggerLabel = selectedRoles.length
		? selectedRoles.join(', ')
		: translate('none');

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

				const selectedRoleNames = new Set(selectedRoles);

				await Promise.all(
					accountRoles
						.filter((accountRole) =>
							selectedRoleNames.has(accountRole.name)
						)
						.map((accountRole) =>
							HeadlessAdminUser.sendRoleAccountUser(
								accountId,
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

			<FieldBase label={i18n.translate('roles')}>
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
									key={roleName}
									onClick={() => toggleRole(roleName)}
								>
									<ClayCheckbox
										checked={selectedRoles.includes(
											roleName
										)}
										label={roleName}
										onChange={() => toggleRole(roleName)}
									/>
								</ClayDropDown.Item>
							))}

							<ClayDropDown.Item
								onClick={() => setSelectedRoles([])}
							>
								<ClayCheckbox
									checked={!selectedRoles.length}
									label={translate('none')}
									onChange={() => setSelectedRoles([])}
								/>
							</ClayDropDown.Item>
						</ClayDropDown.ItemList>
					</ClayDropDown>
				</div>
			</FieldBase>
		</form>
	);
};

export default InviteMemberModal;
