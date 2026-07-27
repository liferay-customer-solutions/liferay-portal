/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';
import ClayDropDown from '@clayui/drop-down';
import ClayIcon from '@clayui/icon';
import {useState} from 'react';
import {translate} from '~/i18n';
import PermissionsSelect from '~/pages/MyAccount/ProjectMembers/components/PermissionsSelect/PermissionsSelect';
import fetcher from '~/services/fetcher/fetcher';
import HeadlessAdminUser from '~/services/headless/HeadlessAdminUser';
import {Liferay} from '~/services/liferay/liferay';
import Projects from '~/services/spring-boot/Projects';

import '../../ProjectMembers.css';

import type {
	AccountMemberOption,
	ProjectMembersRow,
} from '~/pages/MyAccount/ProjectMembers/types';

type MemberDropDownProps = {
	filteredOptions: AccountMemberOption[];
	onChange: (option: AccountMemberOption) => void;
	selectedName: string;
};

const MemberDropDown = ({
	filteredOptions,
	onChange,
	selectedName,
}: MemberDropDownProps) => {
	const [active, setActive] = useState(false);

	return (
		<ClayDropDown
			active={active}
			className="project-permissions-role-dropdown"
			menuElementAttrs={{className: 'project-permissions-role-menu'}}
			onActiveChange={setActive}
			trigger={
				<button
					className="align-items-center d-flex form-control justify-content-between project-permissions-role-trigger"
					type="button"
				>
					<span>{selectedName || translate('select-a-member')}</span>

					<ClayIcon symbol="caret-bottom" />
				</button>
			}
		>
			<ClayDropDown.ItemList>
				{filteredOptions.map((option) => (
					<ClayDropDown.Item
						key={option.userId}
						onClick={() => onChange(option)}
					>
						{option.name}
					</ClayDropDown.Item>
				))}
			</ClayDropDown.ItemList>
		</ClayDropDown>
	);
};

type WorkingMember = {
	designations: string[];
	email: string;
	isNew: boolean;
	membershipId: number;
	name: string;
	originalDesignations: string[];
	originalRoleExternalReferenceCode: string;
	removed: boolean;
	roleExternalReferenceCode: string;
	userId: number;
};

type EditProjectPermissionsModalProps = {
	accountExternalReferenceCode: string;
	accountMemberOptions: AccountMemberOption[];
	mutate: () => Promise<unknown>;
	onClose: () => void;
	project: ProjectMembersRow;
};

const EditProjectPermissionsModal = ({
	accountExternalReferenceCode,
	accountMemberOptions,
	mutate,
	onClose,
	project,
}: EditProjectPermissionsModalProps) => {
	const {availableDesignations} = project;

	const [members, setMembers] = useState<WorkingMember[]>(
		project.members.map((member) => ({
			designations: member.designations,
			email: member.email,
			isNew: false,
			membershipId: member.membershipId,
			name: member.name,
			originalDesignations: member.designations,
			originalRoleExternalReferenceCode: member.roleExternalReferenceCode,
			removed: false,
			roleExternalReferenceCode: member.roleExternalReferenceCode,
			userId: member.userId,
		}))
	);
	const [error, setError] = useState('');

	const updateMember = (index: number, patch: Partial<WorkingMember>) =>
		setMembers((previous) =>
			previous.map((member, memberIndex) =>
				memberIndex === index ? {...member, ...patch} : member
			)
		);

	const toggleDesignation = (index: number, designation: string) =>
		setMembers((previous) =>
			previous.map((member, memberIndex) =>
				memberIndex === index
					? {
							...member,
							designations: member.designations.includes(
								designation
							)
								? member.designations.filter(
										(value) => value !== designation
									)
								: [...member.designations, designation],
						}
					: member
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

	const onSubmit = async (event: React.FormEvent) => {
		event.preventDefault();

		const activeMembers = members.filter((member) => !member.removed);

		if (activeMembers.some((member) => !member.userId)) {
			setError(translate('please-select-a-user-for-every-new-member'));

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
						Projects.postProjectMembership(
							project.externalReferenceCode,
							member.userId,
							member.roleExternalReferenceCode
						)
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

			const hasDesignationChanges = members.some(
				(member) =>
					!member.removed &&
					member.userId &&
					availableDesignations.some(
						(designation) =>
							member.designations.includes(designation) !==
							member.originalDesignations.includes(designation)
					)
			);

			if (hasDesignationChanges) {
				const [account, {items: accountRoles}] = await Promise.all([
					HeadlessAdminUser.getAccountByExternalReferenceCode(
						accountExternalReferenceCode
					),
					HeadlessAdminUser.getAccountRoles(
						accountExternalReferenceCode
					),
				]);

				const accountRoleByName = new Map(
					accountRoles.map((accountRole) => [
						accountRole.name,
						accountRole,
					])
				);

				members.forEach((member) => {
					if (member.removed || !member.userId) {
						return;
					}

					availableDesignations.forEach((designation) => {
						const accountRole = accountRoleByName.get(designation);

						if (!accountRole) {
							return;
						}

						const selected =
							member.designations.includes(designation);
						const original =
							member.originalDesignations.includes(designation);

						if (selected && !original) {
							operations.push(
								HeadlessAdminUser.sendRoleAccountUser(
									account.id,
									accountRole.id,
									member.userId
								)
							);
						}
						else if (!selected && original) {
							operations.push(
								HeadlessAdminUser.deleteRoleAccountUser(
									account.id,
									accountRole.id,
									member.userId
								)
							);
						}
					});
				});
			}

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
			<div className="project-permissions-grid">
				<div className="project-permissions-grid-header project-permissions-grid-row">
					<span>{translate('team-member')}</span>

					<span>{translate('role')}</span>

					<span />
				</div>

				{members.map((member, index) =>
					member.removed ? null : (
						<div
							className="project-permissions-grid-row"
							key={index}
						>
							{member.isNew && !member.userId ? (
								<MemberDropDown
									filteredOptions={accountMemberOptions.filter(
										(option) =>
											!members.some(
												(m, i) =>
													i !== index &&
													!m.removed &&
													m.userId === option.userId
											)
									)}
									onChange={(option) =>
										updateMember(index, {
											email: option.email,
											name: option.name,
											userId: option.userId,
										})
									}
									selectedName={member.name}
								/>
							) : (
								<span className="form-control project-permissions-member-box">
									{member.name}
								</span>
							)}

							<PermissionsSelect
								availableDesignations={availableDesignations}
								designations={member.designations}
								onRoleChange={(roleExternalReferenceCode) =>
									updateMember(index, {
										roleExternalReferenceCode,
									})
								}
								onToggleDesignation={(designation) =>
									toggleDesignation(index, designation)
								}
								roleExternalReferenceCode={
									member.roleExternalReferenceCode
								}
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
			</div>

			<ClayButton
				className="project-permissions-add-button"
				displayType="secondary"
				onClick={() =>
					setMembers((previous) => [
						...previous,
						{
							designations: [],
							email: '',
							isNew: true,
							membershipId: 0,
							name: '',
							originalDesignations: [],
							originalRoleExternalReferenceCode: '',
							removed: false,
							roleExternalReferenceCode: '',
							userId: 0,
						},
					])
				}
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
