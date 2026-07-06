/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';
import ClayDropDown from '@clayui/drop-down';
import {ClayCheckbox, ClaySelect} from '@clayui/form';
import ClayIcon from '@clayui/icon';
import {useMemo, useState} from 'react';
import {translate} from '~/i18n';
import {
	PROJECT_ROLE_ERCS,
	getProjectRoleLabel,
} from '~/pages/MyAccount/ProjectMembers/projectRoles';
import fetcher from '~/services/fetcher/fetcher';
import {Liferay} from '~/services/liferay/liferay';
import UserAccounts from '~/services/spring-boot/UserAccounts';

import '../../ProjectMembers.css';

import type {
	AccountMemberOption,
	ProjectMembersRow,
} from '~/pages/MyAccount/ProjectMembers/types';

type WorkingMember = {
	email: string;
	isNew: boolean;
	membershipId: number;
	name: string;
	originalRoleExternalReferenceCode: string;
	removed: boolean;
	roleExternalReferenceCode: string;
	userId: number;
};

type EditProjectPermissionsModalProps = {
	accountId: number | string;
	accountMemberOptions: AccountMemberOption[];
	mutate: () => Promise<unknown>;
	onClose: () => void;
	project: ProjectMembersRow;
};

function RoleSelect({
	onChange,
	value,
}: {
	onChange: (roleExternalReferenceCode: string) => void;
	value: string;
}) {
	const [active, setActive] = useState(false);

	const select = (roleExternalReferenceCode: string) => {
		onChange(roleExternalReferenceCode);
		setActive(false);
	};

	return (
		<ClayDropDown
			active={active}
			className="project-permissions-role-dropdown"
			closeOnClick={false}
			menuElementAttrs={{className: 'project-permissions-role-menu'}}
			onActiveChange={setActive}
			trigger={
				<button
					className="align-items-center d-flex form-control justify-content-between project-permissions-role-trigger"
					type="button"
				>
					<span>
						{value
							? getProjectRoleLabel(value)
							: translate('select-a-role')}
					</span>

					<ClayIcon symbol="caret-bottom" />
				</button>
			}
		>
			<ClayDropDown.ItemList>
				{PROJECT_ROLE_ERCS.map((roleExternalReferenceCode) => (
					<ClayDropDown.Item
						key={roleExternalReferenceCode}
						onClick={() => select(roleExternalReferenceCode)}
					>
						<ClayCheckbox
							checked={value === roleExternalReferenceCode}
							label={getProjectRoleLabel(
								roleExternalReferenceCode
							)}
							onChange={() => select(roleExternalReferenceCode)}
						/>
					</ClayDropDown.Item>
				))}
			</ClayDropDown.ItemList>
		</ClayDropDown>
	);
}

const EditProjectPermissionsModal = ({
	accountId,
	accountMemberOptions,
	mutate,
	onClose,
	project,
}: EditProjectPermissionsModalProps) => {
	const [members, setMembers] = useState<WorkingMember[]>(
		project.members.map((member) => ({
			email: member.email,
			isNew: false,
			membershipId: member.membershipId,
			name: member.name,
			originalRoleExternalReferenceCode: member.roleExternalReferenceCode,
			removed: false,
			roleExternalReferenceCode: member.roleExternalReferenceCode,
			userId: member.userId,
		}))
	);
	const [error, setError] = useState('');

	const availableToAdd = useMemo(
		() =>
			accountMemberOptions.filter(
				(option) =>
					!members.some(
						(member) =>
							member.userId === option.userId && !member.removed
					)
			),
		[accountMemberOptions, members]
	);

	const updateMember = (index: number, patch: Partial<WorkingMember>) =>
		setMembers((previous) =>
			previous.map((member, memberIndex) =>
				memberIndex === index ? {...member, ...patch} : member
			)
		);

	const removeMember = (index: number) =>
		setMembers((previous) =>
			previous
				.map((member, memberIndex) =>
					memberIndex === index ? {...member, removed: true} : member
				)
				.filter((member) => !(member.isNew && member.removed))
		);

	const selectNewMember = (index: number, userId: number) => {
		const option = accountMemberOptions.find(
			(accountMemberOption) => accountMemberOption.userId === userId
		);

		if (option) {
			updateMember(index, {
				email: option.email,
				name: option.name,
				userId: option.userId,
			});
		}
	};

	const addMember = () => {
		setError('');

		setMembers((previous) => [
			...previous,
			{
				email: '',
				isNew: true,
				membershipId: 0,
				name: '',
				originalRoleExternalReferenceCode: '',
				removed: false,
				roleExternalReferenceCode: '',
				userId: 0,
			},
		]);
	};

	const onSubmit = async (event: React.FormEvent) => {
		event.preventDefault();

		const activeMembers = members.filter((member) => !member.removed);

		if (activeMembers.some((member) => !member.userId)) {
			setError(translate('select-a-member-and-a-role'));

			return;
		}

		if (activeMembers.some((member) => !member.roleExternalReferenceCode)) {
			setError(translate('a-role-is-required-for-every-member'));

			return;
		}

		try {
			const operations: Promise<unknown>[] = [];

			members.forEach((member) => {
				if (member.isNew && !member.removed) {
					operations.push(
						UserAccounts.postAssignments({
							accountId,
							projects: [
								{
									projectExternalReferenceCode:
										project.externalReferenceCode,
									roleExternalReferenceCode:
										member.roleExternalReferenceCode,
								},
							],
							userId: member.userId,
						})
					);
				}
				else if (!member.isNew && member.removed) {
					operations.push(
						fetcher.delete(
							`/o/c/projectmemberships/${member.membershipId}`
						)
					);
				}
				else if (
					!member.isNew &&
					member.roleExternalReferenceCode !==
						member.originalRoleExternalReferenceCode
				) {
					operations.push(
						fetcher.patch(
							`/o/c/projectmemberships/${member.membershipId}`,
							{
								roleExternalReferenceCode:
									member.roleExternalReferenceCode,
							}
						)
					);
				}
			});

			await Promise.all(operations);

			await mutate();

			Liferay.Util.openToast({
				message: translate('project-members-successfully-updated'),
				title: translate('success'),
			});

			onClose();
		}
		catch {
			Liferay.Util.openToast({
				message: translate('unable-to-update-project-members'),
				title: translate('error'),
				type: 'danger',
			});
		}
	};

	return (
		<form id="edit-project-permissions" onSubmit={onSubmit}>
			<p className="text-neutral-7">
				{translate(
					'team-members-will-receive-an-email-invitation-about-the-permissions-changes'
				)}
			</p>

			<div className="project-permissions-grid project-permissions-grid-header">
				<span>{translate('team-member')}</span>

				<span>{translate('role')}</span>

				<span />
			</div>

			{members.map((member, index) =>
				member.removed ? null : (
					<div className="project-permissions-grid" key={index}>
						{member.isNew && !member.userId ? (
							<ClaySelect
								aria-label={translate('team-member')}
								onChange={(event) =>
									selectNewMember(
										index,
										Number(event.target.value)
									)
								}
								value=""
							>
								<ClaySelect.Option
									label={translate('select-a-member')}
									value=""
								/>

								{availableToAdd.map((option) => (
									<ClaySelect.Option
										key={option.userId}
										label={option.name}
										value={String(option.userId)}
									/>
								))}
							</ClaySelect>
						) : (
							<span className="form-control project-permissions-member-box">
								{member.name}
							</span>
						)}

						<RoleSelect
							onChange={(roleExternalReferenceCode) =>
								updateMember(index, {roleExternalReferenceCode})
							}
							value={member.roleExternalReferenceCode}
						/>

						<ClayButton
							className="project-permissions-remove-button"
							onClick={() => removeMember(index)}
							type="button"
						>
							<ClayIcon className="mr-2" symbol="hr" />

							{translate('remove')}
						</ClayButton>
					</div>
				)
			)}

			<ClayButton
				className="mt-3 project-permissions-add-button"
				displayType="link"
				onClick={addMember}
				type="button"
			>
				<ClayIcon className="mr-2" symbol="plus" />

				{translate('add-more-people')}
			</ClayButton>

			{error && <div className="mt-2 text-danger">{error}</div>}
		</form>
	);
};

export default EditProjectPermissionsModal;
