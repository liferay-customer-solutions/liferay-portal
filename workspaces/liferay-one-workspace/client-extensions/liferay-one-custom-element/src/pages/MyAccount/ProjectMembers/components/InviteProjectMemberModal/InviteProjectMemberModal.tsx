/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ClayInput} from '@clayui/form';
import {useState} from 'react';
import {FieldBase} from '~/components/FieldBase/FieldBase';
import {translate} from '~/i18n';
import {
	fetchRoleExternalReferenceCodesByName,
	getRoleExternalReferenceCodes,
} from '~/pages/MyAccount/AccountMembers/accountRoles';
import PermissionsSelect from '~/pages/MyAccount/ProjectMembers/components/PermissionsSelect/PermissionsSelect';
import {Liferay} from '~/services/liferay/liferay';
import Accounts from '~/services/spring-boot/Accounts';
import {EMAIL_PATTERN} from '~/utils/formValidationUtils';

import '~/pages/MyAccount/ProjectMembers/ProjectMembers.css';

import type {ProjectMembersRow} from '~/pages/MyAccount/ProjectMembers/types';

type InviteProjectMemberModalProps = {
	accountExternalReferenceCode: string;
	mutate: () => Promise<unknown>;
	onClose: () => void;
	project: ProjectMembersRow;
};

const InviteProjectMemberModal = ({
	accountExternalReferenceCode,
	mutate,
	onClose,
	project,
}: InviteProjectMemberModalProps) => {
	const [emailAddress, setEmailAddress] = useState('');
	const [emailError, setEmailError] = useState('');
	const [familyName, setFamilyName] = useState('');
	const [familyNameError, setFamilyNameError] = useState('');
	const [givenName, setGivenName] = useState('');
	const [givenNameError, setGivenNameError] = useState('');
	const [roleError, setRoleError] = useState('');
	const [roleExternalReferenceCode, setRoleExternalReferenceCode] =
		useState('');
	const [selectedDesignations, setSelectedDesignations] = useState<string[]>(
		[]
	);

	const toggleDesignation = (designation: string) =>
		setSelectedDesignations((previous) =>
			previous.includes(designation)
				? previous.filter((value) => value !== designation)
				: [...previous, designation]
		);

	const onSubmit = async (event: React.FormEvent) => {
		event.preventDefault();

		const trimmedEmail = emailAddress.trim();
		const trimmedFamilyName = familyName.trim();
		const trimmedGivenName = givenName.trim();

		let hasError = false;

		if (!trimmedGivenName) {
			setGivenNameError(translate('please-enter-a-valid-first-name'));
			hasError = true;
		}

		if (!trimmedFamilyName) {
			setFamilyNameError(translate('please-enter-a-valid-last-name'));
			hasError = true;
		}

		if (!EMAIL_PATTERN.test(trimmedEmail)) {
			setEmailError(translate('please-enter-a-valid-email-address'));
			hasError = true;
		}

		if (!roleExternalReferenceCode) {
			setRoleError(translate('a-role-is-required'));
			hasError = true;
		}

		if (hasError) {
			return;
		}

		try {
			const roleExternalReferenceCodes = selectedDesignations.length
				? getRoleExternalReferenceCodes(
						selectedDesignations,
						await fetchRoleExternalReferenceCodesByName(
							accountExternalReferenceCode
						)
					)
				: [];

			if (!roleExternalReferenceCodes) {
				throw new Error('Unable to find the selected account roles');
			}

			await Accounts.postInvitations(accountExternalReferenceCode, {
				emailAddress: trimmedEmail,
				familyName: trimmedFamilyName,
				givenName: trimmedGivenName,
				projectExternalReferenceCode: project.externalReferenceCode,
				projectRoleExternalReferenceCode: roleExternalReferenceCode,
				roleExternalReferenceCodes,
			});

			await mutate();

			Liferay.Util.openToast({
				message: translate('invitation-successfully-sent'),
				title: translate('success'),
			});

			onClose();
		}
		catch {
			Liferay.Util.openToast({
				message: translate('unable-to-invite-project-member'),
				title: translate('error'),
				type: 'danger',
			});
		}
	};

	return (
		<form id="invite-project-member" onSubmit={onSubmit}>
			<p className="text-neutral-7">
				{translate(
					'invite-a-new-member-to-this-project-they-will-be-added-as-an-account-member-once-they-accept-the-invitation'
				)}
			</p>

			<FieldBase
				errorMessage={givenNameError}
				label={translate('first-name')}
				required
			>
				<ClayInput
					onChange={(event) => {
						setGivenNameError('');
						setGivenName(event.target.value);
					}}
					type="text"
					value={givenName}
				/>
			</FieldBase>

			<FieldBase
				errorMessage={familyNameError}
				label={translate('last-name')}
				required
			>
				<ClayInput
					onChange={(event) => {
						setFamilyNameError('');
						setFamilyName(event.target.value);
					}}
					type="text"
					value={familyName}
				/>
			</FieldBase>

			<FieldBase
				errorMessage={emailError}
				label={translate('email-address')}
				required
			>
				<ClayInput
					onChange={(event) => {
						setEmailError('');
						setEmailAddress(event.target.value);
					}}
					placeholder={translate('name-example-com')}
					type="email"
					value={emailAddress}
				/>
			</FieldBase>

			<FieldBase
				errorMessage={roleError}
				label={translate('role')}
				required
			>
				<PermissionsSelect
					availableDesignations={project.availableDesignations}
					designations={selectedDesignations}
					onRoleChange={(erc) => {
						setRoleError('');
						setRoleExternalReferenceCode(erc);
					}}
					onToggleDesignation={toggleDesignation}
					roleExternalReferenceCode={roleExternalReferenceCode}
				/>
			</FieldBase>
		</form>
	);
};

export default InviteProjectMemberModal;
