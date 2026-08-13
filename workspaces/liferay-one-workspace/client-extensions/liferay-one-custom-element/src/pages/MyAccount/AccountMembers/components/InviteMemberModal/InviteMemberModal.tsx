/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ClayInput} from '@clayui/form';
import {zodResolver} from '@hookform/resolvers/zod';
import {useFieldArray, useForm} from 'react-hook-form';
import Button from '~/components/Button/Button';
import {FieldBase} from '~/components/FieldBase/FieldBase';
import i18n, {sub, translate} from '~/i18n';
import {isPartnerRole} from '~/pages/MyAccount/AccountMembers/accountRoles';
import AccountRolesSelect from '~/pages/MyAccount/AccountMembers/components/AccountRolesSelect/AccountRolesSelect';
import accountSchemas, {MAX_INVITATIONS_COUNT} from '~/schema/accountSchemas';
import FetcherError from '~/services/fetcher/FetcherError';
import {Liferay} from '~/services/liferay/liferay';
import Accounts from '~/services/spring-boot/Accounts';

import '../../AccountMembers.css';

import type {InviteMembersForm} from '~/schema/accountSchemas';

const createEmptyInvite = () => ({
	emailAddress: '',
	familyName: '',
	givenName: '',
	roleNames: [],
});

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
	roleNames: availableRoleNames,
}: InviteMemberModalProps) => {
	const {
		control,
		formState: {errors, isSubmitting},
		handleSubmit,
		register,
		setValue,
		watch,
	} = useForm<InviteMembersForm>({
		defaultValues: {invites: [createEmptyInvite()]},
		resolver: zodResolver(accountSchemas.inviteMembers),
	});

	const {append, fields, remove} = useFieldArray({control, name: 'invites'});

	const invites = watch('invites');

	const toggleRole = (index: number, roleName: string) => {
		const selectedRoleNames = invites[index]?.roleNames ?? [];

		if (selectedRoleNames.includes(roleName)) {
			setValue(
				`invites.${index}.roleNames`,
				selectedRoleNames.filter((value) => value !== roleName)
			);

			return;
		}

		if (isPartnerRole(roleName)) {
			setValue(`invites.${index}.roleNames`, [
				...selectedRoleNames.filter((value) => !isPartnerRole(value)),
				roleName,
			]);

			return;
		}

		setValue(`invites.${index}.roleNames`, [
			...selectedRoleNames,
			roleName,
		]);
	};

	const onSubmit = async (form: InviteMembersForm) => {
		if (isSubmitting) {
			return;
		}

		const alreadyMemberEmailAddresses: string[] = [];
		const failedEmailAddresses: string[] = [];
		const invitedIndexes: number[] = [];

		for (const [index, invite] of form.invites.entries()) {
			try {
				await Accounts.postInvitations(accountExternalReferenceCode, {
					emailAddress: invite.emailAddress,
					familyName: invite.familyName,
					givenName: invite.givenName,
					roleNames: invite.roleNames,
				});

				invitedIndexes.push(index);
			}
			catch (error) {
				if (error instanceof FetcherError && error.status === 409) {
					alreadyMemberEmailAddresses.push(invite.emailAddress);
				}
				else {
					failedEmailAddresses.push(invite.emailAddress);
				}
			}
		}

		const unsentEmailAddresses = [
			...alreadyMemberEmailAddresses,
			...failedEmailAddresses,
		];

		if (invitedIndexes.length) {
			await mutate();
		}

		if (!unsentEmailAddresses.length) {
			Liferay.Util.openToast({
				message:
					invitedIndexes.length > 1
						? translate('invitations-successfully-sent')
						: translate('invitation-successfully-sent'),
				title: translate('success'),
			});

			onClose();

			return;
		}

		remove(invitedIndexes);

		const unsentMessage = [
			alreadyMemberEmailAddresses.length
				? sub('x-is-already-a-member-of-this-account', [
						alreadyMemberEmailAddresses.join(', '),
					])
				: '',
			failedEmailAddresses.length
				? sub('unable-to-send-the-invitation-to-x', [
						failedEmailAddresses.join(', '),
					])
				: '',
		]
			.filter(Boolean)
			.join(' ');

		if (!invitedIndexes.length) {
			Liferay.Util.openToast({
				message: unsentMessage,
				title: translate('error'),
				type: 'danger',
			});

			return;
		}

		Liferay.Util.openToast({
			message: `${sub('x-of-x-invitations-were-sent', [
				String(invitedIndexes.length),
				String(form.invites.length),
			])} ${unsentMessage}`,
			title: translate('warning'),
			type: 'warning',
		});
	};

	return (
		<form id="invite-member" onSubmit={handleSubmit(onSubmit)}>
			<p>
				{i18n.translate(
					'invited-members-receive-an-email-and-join-the-account-after-accepting-the-invitation'
				)}
			</p>

			{fields.map((field, index) => (
				<div
					className={
						index ? 'account-members-invite-row pt-3' : undefined
					}
					key={field.id}
				>
					<FieldBase
						errorMessage={
							errors.invites?.[index]?.givenName?.message
						}
						label={i18n.translate('first-name')}
						required
					>
						<ClayInput
							{...register(`invites.${index}.givenName`)}
							disabled={isSubmitting}
							type="text"
						/>
					</FieldBase>

					<FieldBase
						errorMessage={
							errors.invites?.[index]?.familyName?.message
						}
						label={i18n.translate('last-name')}
						required
					>
						<ClayInput
							{...register(`invites.${index}.familyName`)}
							disabled={isSubmitting}
							type="text"
						/>
					</FieldBase>

					<FieldBase
						errorMessage={
							errors.invites?.[index]?.emailAddress?.message
						}
						label={i18n.translate('email-address')}
						required
					>
						<ClayInput
							{...register(`invites.${index}.emailAddress`)}
							disabled={isSubmitting}
							placeholder={i18n.translate('name-example-com')}
							type="email"
						/>
					</FieldBase>

					<FieldBase label={i18n.translate('roles')}>
						<AccountRolesSelect
							onClearRoles={() =>
								setValue(`invites.${index}.roleNames`, [])
							}
							onToggleRole={(roleName) =>
								toggleRole(index, roleName)
							}
							roleNames={availableRoleNames}
							selectedRoleNames={invites[index]?.roleNames ?? []}
						/>
					</FieldBase>
				</div>
			))}

			<div className="mt-3">
				{fields.length < MAX_INVITATIONS_COUNT && (
					<Button
						className="mr-3"
						disabled={isSubmitting}
						displayType="secondary"
						onClick={() => append(createEmptyInvite())}
						prependIcon="plus"
						small
						type="button"
					>
						{translate('add-more-members')}
					</Button>
				)}

				{fields.length > 1 && (
					<Button
						disabled={isSubmitting}
						displayType="secondary"
						onClick={() => remove(fields.length - 1)}
						prependIcon="hr"
						small
						type="button"
					>
						{translate('remove-this-member')}
					</Button>
				)}
			</div>

			<div className="d-flex justify-content-end mt-4">
				<Button
					className="mr-3"
					disabled={isSubmitting}
					displayType="secondary"
					onClick={onClose}
					type="button"
				>
					{translate('cancel')}
				</Button>

				<Button disabled={isSubmitting} type="submit">
					{translate('send-invitation')}
				</Button>
			</div>
		</form>
	);
};

export default InviteMemberModal;
