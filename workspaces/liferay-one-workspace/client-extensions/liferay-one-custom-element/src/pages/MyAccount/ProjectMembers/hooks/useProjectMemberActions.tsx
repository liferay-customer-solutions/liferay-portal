/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import Button from '~/components/Button/Button';
import useModalContext from '~/hooks/useModalContext';
import {translate} from '~/i18n';
import EditCloudContactsModal from '~/pages/MyAccount/ProjectMembers/components/EditCloudContactsModal/EditCloudContactsModal';
import EditProjectPermissionsModal from '~/pages/MyAccount/ProjectMembers/components/EditProjectPermissionsModal/EditProjectPermissionsModal';

import type {
	AccountMemberOption,
	ProjectMembersRow,
} from '~/pages/MyAccount/ProjectMembers/types';

type UseProjectMemberActionsProps = {
	accountExternalReferenceCode: string;
	accountId: number | string;
	accountMemberOptions: AccountMemberOption[];
	mutate: () => Promise<unknown>;
};

export function useProjectMemberActions({
	accountExternalReferenceCode,
	accountId,
	accountMemberOptions,
	mutate,
}: UseProjectMemberActionsProps) {
	const modalContext = useModalContext();

	const cancelButton = (
		<Button
			displayType="secondary"
			key="cancel"
			onClick={modalContext.onClose}
		>
			{translate('cancel')}
		</Button>
	);

	const openEditProjectPermissions = (project: ProjectMembersRow) => {
		modalContext.onOpenModal({
			body: (
				<EditProjectPermissionsModal
					accountId={accountId}
					accountMemberOptions={accountMemberOptions}
					mutate={mutate}
					onClose={modalContext.onClose}
					project={project}
				/>
			),
			footer: [
				cancelButton,
				null,
				<Button
					form="edit-project-permissions"
					key="confirm"
					type="submit"
				>
					{translate('save-changes')}
				</Button>,
			],
			header: translate('project-permissions'),
		});
	};

	const openEditCloudContacts = (project: ProjectMembersRow) => {
		modalContext.onOpenModal({
			body: (
				<EditCloudContactsModal
					accountExternalReferenceCode={accountExternalReferenceCode}
					mutate={mutate}
					onClose={modalContext.onClose}
					project={project}
				/>
			),
			footer: [
				cancelButton,
				null,
				<Button form="edit-cloud-contacts" key="confirm" type="submit">
					{translate('save')}
				</Button>,
			],
			header: translate('edit-cloud-contacts'),
		});
	};

	return {openEditCloudContacts, openEditProjectPermissions};
}
