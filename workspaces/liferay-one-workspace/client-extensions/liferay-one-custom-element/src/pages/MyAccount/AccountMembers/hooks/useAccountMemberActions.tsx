/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import Button from '~/components/Button/Button';
import {useConfirmationModal} from '~/hooks/useConfirmationModal';
import useModalContext from '~/hooks/useModalContext';
import {sub, translate} from '~/i18n';
import EditPermissionsModal from '~/pages/MyAccount/AccountMembers/components/EditPermissionsModal/EditPermissionsModal';
import InviteMemberModal from '~/pages/MyAccount/AccountMembers/components/InviteMemberModal/InviteMemberModal';
import fetcher from '~/services/fetcher/fetcher';
import HeadlessAdminUser from '~/services/headless/HeadlessAdminUser';
import {Liferay} from '~/services/liferay/liferay';

import type {AccountMemberRow} from '~/pages/MyAccount/AccountMembers/types';
import type {APIResponse} from '~/types/api';

type ProjectMembershipItem = {
	id: number;
	r_projectToProjectMembership_c_projectERC: string;
};

type UseAccountMemberActionsProps = {
	accountExternalReferenceCode: string;
	accountId: number | string;
	adminCount: number;
	mutate: () => Promise<unknown>;
	projectNamesByExternalReferenceCode: Record<string, string>;
	roleNames: string[];
};

export function useAccountMemberActions({
	accountExternalReferenceCode,
	accountId,
	adminCount,
	mutate,
	projectNamesByExternalReferenceCode,
	roleNames,
}: UseAccountMemberActionsProps) {
	const modalContext = useModalContext();
	const {openModal} = useConfirmationModal();

	const cancelButton = (
		<Button
			displayType="secondary"
			key="cancel"
			onClick={modalContext.onClose}
		>
			{translate('cancel')}
		</Button>
	);

	const openInviteModal = () => {
		modalContext.onOpenModal({
			body: (
				<InviteMemberModal
					accountExternalReferenceCode={accountExternalReferenceCode}
					accountId={accountId}
					mutate={mutate}
					onClose={modalContext.onClose}
					roleNames={roleNames}
				/>
			),
			footer: [
				cancelButton,
				null,
				<Button form="invite-member" key="confirm" type="submit">
					{translate('send-invitation')}
				</Button>,
			],
			header: translate('invite-member'),
		});
	};

	const openEditPermissionsModal = (member: AccountMemberRow) => {
		modalContext.onOpenModal({
			body: (
				<EditPermissionsModal
					accountExternalReferenceCode={accountExternalReferenceCode}
					accountId={accountId}
					adminCount={adminCount}
					memberName={member.name}
					memberRoleBriefs={member.roleBriefs}
					mutate={mutate}
					onClose={modalContext.onClose}
					roleNames={roleNames}
					userId={member.id}
				/>
			),
			footer: [
				cancelButton,
				null,
				<Button form="edit-permissions" key="confirm" type="submit">
					{translate('save-changes')}
				</Button>,
			],
			header: translate('account-permissions'),
		});
	};

	const openRemoveMemberModal = async (member: AccountMemberRow) => {
		if (member.isAdministrator && adminCount <= 1) {
			modalContext.onOpenModal({
				body: (
					<p>
						{translate(
							'at-least-one-account-admin-is-required-assign-another-account-admin-before-removing-this-member'
						)}
					</p>
				),
				footer: [
					null,
					null,
					<Button key="ok" onClick={modalContext.onClose}>
						{translate('ok')}
					</Button>,
				],
				header: translate('remove-member'),
				status: 'warning',
			});

			return;
		}

		let memberships: ProjectMembershipItem[] = [];

		try {
			const response = await fetcher<APIResponse<ProjectMembershipItem>>(
				`/o/c/projectmemberships?filter=${encodeURIComponent(
					`r_accountEntryToProjectMembership_accountEntryId eq '${accountId}' and r_userToProjectMembership_userId eq '${member.id}'`
				)}&pageSize=200`
			);

			memberships = response.items ?? [];
		}
		catch {
			Liferay.Util.openToast({
				message: translate('unable-to-remove-member'),
				title: translate('error'),
				type: 'danger',
			});

			return;
		}

		const projectNames = memberships
			.map(
				(membership) =>
					projectNamesByExternalReferenceCode[
						membership.r_projectToProjectMembership_c_projectERC
					]
			)
			.filter(Boolean);

		openModal({
			body: projectNames.length ? (
				<p>
					{sub(
						'x-is-a-member-of-x-on-this-account-removing-them-from-the-account-will-also-remove-their-access-to-these-projects-are-you-sure-you-want-to-proceed',
						[member.name, projectNames.join(', ')]
					)}
				</p>
			) : (
				<p>
					{sub(
						'are-you-sure-you-want-to-remove-x-from-this-account',
						member.name
					)}
				</p>
			),
			header: translate('remove-member'),
			onConfirm: async () => {
				try {
					await Promise.all(
						memberships.map((membership) =>
							fetcher.delete(
								`/o/c/projectmemberships/${membership.id}`
							)
						)
					);

					await HeadlessAdminUser.deleteAccountUserAccountByEmailAddress(
						accountExternalReferenceCode,
						member.email
					);

					await mutate();

					Liferay.Util.openToast({
						message: translate('member-successfully-removed'),
						title: translate('success'),
					});
				}
				catch {
					Liferay.Util.openToast({
						message: translate('unable-to-remove-member'),
						title: translate('error'),
						type: 'danger',
					});
				}
			},
			status: 'danger',
		});
	};

	return {openEditPermissionsModal, openInviteModal, openRemoveMemberModal};
}
