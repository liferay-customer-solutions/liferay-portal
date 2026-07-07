/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';
import ClayDropDown from '@clayui/drop-down';
import {ClayCheckbox, ClayInput} from '@clayui/form';
import ClayIcon from '@clayui/icon';
import {ClayPaginationBarWithBasicItems} from '@clayui/pagination-bar';
import ClayTable from '@clayui/table';
import {useMemo, useState} from 'react';
import Button from '~/components/Button/Button';
import EmptyState from '~/components/EmptyState/EmptyState';
import Page from '~/components/Page/Page';
import RestrictedFeatureMessage from '~/components/RestrictedFeatureMessage/RestrictedFeatureMessage';
import {useOneContext} from '~/context/OneContextProvider';
import {useFetch} from '~/hooks/useFetch';
import i18n, {sub, translate} from '~/i18n';
import {
	canAccessAccountMembers,
	getMembershipRoleNames,
	hasAdministratorRole,
	isAccountManager,
} from '~/pages/MyAccount/AccountMembers/accountRoles';
import {useAccountMemberActions} from '~/pages/MyAccount/AccountMembers/hooks/useAccountMemberActions';
import {useAccountType} from '~/pages/MyAccount/AccountMembers/hooks/useAccountType';
import {useHasProject} from '~/pages/MyAccount/Projects/hooks/useHasProject';
import {Liferay} from '~/services/liferay/liferay';

import './AccountMembers.css';

import type {AccountMemberRow} from '~/pages/MyAccount/AccountMembers/types';
import type {Account, UserAccount} from '~/types/accounts';
import type {APIResponse} from '~/types/api';

const AVATAR_COLORS = [
	'#2e5aac',
	'#e1a325',
	'#cf2c4f',
	'#287d3c',
	'#7d4fc9',
	'#0a7bae',
];

const NO_ROLE = '__no_role__';

const PAGE_SIZE_OPTIONS = [10, 20, 30, 50];

const STATUS_INACTIVE = 5;

type ProjectItem = {
	externalReferenceCode: string;
	name: string;
};

function hasImage(image?: string) {
	return Boolean(image) && !image?.includes('img_id=0');
}

function UserAvatar({image, name}: {image?: string; name: string}) {
	if (hasImage(image)) {
		return <img alt="" className="account-members-avatar" src={image} />;
	}

	const initials = name
		.split(' ')
		.filter(Boolean)
		.slice(0, 2)
		.map((word) => word[0])
		.join('');

	const colorIndex =
		name.split('').reduce((total, char) => total + char.charCodeAt(0), 0) %
		AVATAR_COLORS.length;

	return (
		<span
			className="account-members-avatar"
			style={{backgroundColor: AVATAR_COLORS[colorIndex]}}
		>
			{initials}
		</span>
	);
}

export default function AccountMembers() {
	const accountId = Liferay.CommerceContext?.account?.accountId;
	const currentUserId = Liferay.ThemeDisplay.getUserId();

	const {myUserAccount, userAccountModel} = useOneContext();

	const {hasProject, loading: projectsLoading} = useHasProject();

	const {loading: accountTypeLoading, roleNames} = useAccountType();

	const [keywords, setKeywords] = useState('');
	const [page, setPage] = useState(1);
	const [pageSize, setPageSize] = useState(PAGE_SIZE_OPTIONS[0]);
	const [filterActive, setFilterActive] = useState(false);
	const [selectedRoles, setSelectedRoles] = useState<string[]>([]);

	const canManageMembers = isAccountManager(userAccountModel);

	const {
		data: account,
		error: accountError,
		isLoading: accountLoading,
	} = useFetch<Account>(
		accountId ? `/o/headless-admin-user/v1.0/accounts/${accountId}` : null
	);

	const {
		data,
		error,
		isLoading: loading,
		mutate,
	} = useFetch<APIResponse<UserAccount>>(
		accountId
			? `/o/headless-admin-user/v1.0/accounts/${accountId}/user-accounts`
			: null,
		{params: {pageSize: -1, sort: 'givenName:asc'}}
	);

	const {data: projectData} = useFetch<APIResponse<ProjectItem>>(
		accountId ? '/o/c/projects' : null,
		{
			params: {
				fields: 'externalReferenceCode,name',
				filter: `r_accountEntryToProject_accountEntryId eq '${accountId}'`,
				pageSize: -1,
			},
		}
	);

	const projectNamesByExternalReferenceCode = useMemo(() => {
		const map: Record<string, string> = {};

		(projectData?.items ?? []).forEach((project) => {
			map[project.externalReferenceCode] = project.name;
		});

		return map;
	}, [projectData]);

	const members = useMemo<AccountMemberRow[]>(() => {
		return (data?.items ?? []).map((userAccount) => {
			const accountRoleBriefs =
				userAccount.accountBriefs?.find(
					(accountBrief) => accountBrief.id === accountId
				)?.roleBriefs ?? [];

			return {
				email: userAccount.emailAddress,
				id: userAccount.id,
				image: userAccount.image,
				isAdministrator: hasAdministratorRole(accountRoleBriefs),
				isCurrentUser: String(userAccount.id) === currentUserId,
				name: userAccount.name,
				roleBriefs: accountRoleBriefs,
				roleNames: getMembershipRoleNames(accountRoleBriefs),
				status: userAccount.status ?? 0,
			};
		});
	}, [accountId, currentUserId, data]);

	const adminCount = useMemo(
		() => members.filter((member) => member.isAdministrator).length,
		[members]
	);

	const {openEditPermissionsModal, openInviteModal, openRemoveMemberModal} =
		useAccountMemberActions({
			accountExternalReferenceCode: account?.externalReferenceCode ?? '',
			accountId: accountId ?? '',
			adminCount,
			mutate,
			projectNamesByExternalReferenceCode,
			roleNames,
		});

	const filteredMembers = useMemo(() => {
		const search = keywords.trim().toLowerCase();

		return members.filter((member) => {
			if (selectedRoles.length) {
				const matchesRole = selectedRoles.some((selectedRole) =>
					selectedRole === NO_ROLE
						? !member.roleNames.length
						: member.roleNames.includes(selectedRole)
				);

				if (!matchesRole) {
					return false;
				}
			}

			if (
				search &&
				!member.name.toLowerCase().includes(search) &&
				!member.email.toLowerCase().includes(search)
			) {
				return false;
			}

			return true;
		});
	}, [keywords, members, selectedRoles]);

	const paginatedMembers = useMemo(() => {
		const start = (page - 1) * pageSize;

		return filteredMembers.slice(start, start + pageSize);
	}, [filteredMembers, page, pageSize]);

	const filterOptions = useMemo(() => [...roleNames, NO_ROLE], [roleNames]);

	const toggleRole = (roleName: string) => {
		setPage(1);

		setSelectedRoles((previous) =>
			previous.includes(roleName)
				? previous.filter((value) => value !== roleName)
				: [...previous, roleName]
		);
	};

	const getRoleFilterLabel = (roleName: string) =>
		roleName === NO_ROLE ? translate('no-role') : roleName;

	if (myUserAccount && !canAccessAccountMembers(userAccountModel)) {
		return (
			<Page
				description={i18n.translate(
					'invite-manage-roles-designate-incident-contacts'
				)}
			>
				<EmptyState
					className="mt-5"
					title={translate('you-do-not-have-access-to-this-page')}
					type="NO_ACCESS"
				/>
			</Page>
		);
	}

	if (!projectsLoading && !hasProject) {
		return (
			<Page
				description={i18n.translate(
					'invite-manage-roles-designate-incident-contacts'
				)}
			>
				<RestrictedFeatureMessage />
			</Page>
		);
	}

	return (
		<Page
			description={i18n.translate(
				'invite-manage-roles-designate-incident-contacts'
			)}
			pageRendererProps={{
				error: accountError || error,
				isLoading:
					accountLoading ||
					accountTypeLoading ||
					loading ||
					projectsLoading,
			}}
		>
			<div className="account-members-card mt-3">
				<div className="account-members-toolbar align-items-center d-flex">
					<ClayDropDown
						active={filterActive}
						onActiveChange={setFilterActive}
						trigger={
							<Button
								appendIcon="caret-bottom"
								className="account-members-filter-button"
								displayType="secondary"
								prependIcon="filter"
							>
								{translate('filter')}
							</Button>
						}
					>
						<ClayDropDown.ItemList>
							{filterOptions.map((roleName) => (
								<ClayDropDown.Item
									key={roleName}
									onClick={() => toggleRole(roleName)}
								>
									<ClayCheckbox
										checked={selectedRoles.includes(
											roleName
										)}
										label={getRoleFilterLabel(roleName)}
										onChange={() => toggleRole(roleName)}
									/>
								</ClayDropDown.Item>
							))}
						</ClayDropDown.ItemList>
					</ClayDropDown>

					<ClayInput.Group className="account-members-search">
						<ClayInput.GroupItem>
							<ClayInput
								className="input-group-inset input-group-inset-after"
								id="account-members-search"
								name="account-members-search"
								onChange={(event) => {
									setPage(1);
									setKeywords(event.target.value);
								}}
								placeholder={translate('search')}
								type="text"
								value={keywords}
							/>

							<ClayInput.GroupInsetItem after tag="span">
								<ClayIcon
									className="text-neutral-7"
									symbol="search"
								/>
							</ClayInput.GroupInsetItem>
						</ClayInput.GroupItem>
					</ClayInput.Group>

					{canManageMembers && (
						<Button
							className="account-members-invite-button ml-3"
							onClick={openInviteModal}
							prependIcon="plus"
						>
							{translate('invite-member')}
						</Button>
					)}
				</div>

				{!!selectedRoles.length && (
					<div className="account-members-active-filters">
						{selectedRoles.map((roleName) => (
							<span
								className="account-members-filter-tag"
								key={roleName}
							>
								{getRoleFilterLabel(roleName)}

								<button
									className="account-members-filter-tag-close"
									onClick={() => toggleRole(roleName)}
									type="button"
								>
									<ClayIcon symbol="times" />
								</button>
							</span>
						))}
					</div>
				)}

				{paginatedMembers.length ? (
					<>
						<ClayTable borderless className="account-members-table">
							<ClayTable.Head>
								<ClayTable.Row>
									<ClayTable.Cell headingCell>
										{translate('name')}
									</ClayTable.Cell>

									<ClayTable.Cell headingCell>
										{translate('email')}
									</ClayTable.Cell>

									<ClayTable.Cell headingCell>
										{translate('role')}
									</ClayTable.Cell>

									<ClayTable.Cell headingCell>
										{translate('status')}
									</ClayTable.Cell>

									<ClayTable.Cell headingCell />
								</ClayTable.Row>
							</ClayTable.Head>

							<ClayTable.Body>
								{paginatedMembers.map((member) => {
									const isActive =
										member.status !== STATUS_INACTIVE;

									return (
										<ClayTable.Row key={member.id}>
											<ClayTable.Cell>
												<div className="align-items-center d-flex">
													<UserAvatar
														image={member.image}
														name={member.name}
													/>

													<span className="account-members-name ml-3">
														{member.isCurrentUser
															? sub('x-me', [
																	member.name,
																])
															: member.name}
													</span>
												</div>
											</ClayTable.Cell>

											<ClayTable.Cell>
												{member.email}
											</ClayTable.Cell>

											<ClayTable.Cell>
												{member.roleNames.join(', ')}
											</ClayTable.Cell>

											<ClayTable.Cell>
												<span className="align-items-center d-flex">
													<span
														className={`account-members-status-dot${
															isActive
																? ''
																: ' account-members-status-dot-inactive'
														}`}
													/>

													{isActive
														? translate('active')
														: translate('inactive')}
												</span>
											</ClayTable.Cell>

											<ClayTable.Cell>
												{canManageMembers && (
													<ClayDropDown
														trigger={
															<ClayButton
																aria-label={translate(
																	'manage-user-options'
																)}
																borderless
																className="text-neutral-7"
																displayType="unstyled"
															>
																<ClayIcon symbol="ellipsis-v" />
															</ClayButton>
														}
													>
														<ClayDropDown.ItemList>
															<ClayDropDown.Item
																onClick={() =>
																	openEditPermissionsModal(
																		member
																	)
																}
															>
																{translate(
																	'edit-permissions'
																)}
															</ClayDropDown.Item>

															<ClayDropDown.Item
																onClick={() =>
																	openRemoveMemberModal(
																		member
																	)
																}
															>
																{translate(
																	'remove'
																)}
															</ClayDropDown.Item>
														</ClayDropDown.ItemList>
													</ClayDropDown>
												)}
											</ClayTable.Cell>
										</ClayTable.Row>
									);
								})}
							</ClayTable.Body>
						</ClayTable>

						<div className="account-members-pagination">
							<ClayPaginationBarWithBasicItems
								activeDelta={pageSize}
								activePage={page}
								deltas={PAGE_SIZE_OPTIONS.map((label) => ({
									label,
								}))}
								labels={{
									paginationResults: translate(
										'showing-x-to-x-of-x'
									),
									perPageItems: translate('x-items'),
									selectPerPageItems: translate('x-items'),
								}}
								onDeltaChange={(delta) => {
									setPage(1);
									setPageSize(delta);
								}}
								onPageChange={setPage}
								totalItems={filteredMembers.length}
							/>
						</div>
					</>
				) : (
					<div className="p-4 text-neutral-7">
						{translate('no-account-members-were-found')}
					</div>
				)}
			</div>
		</Page>
	);
}
