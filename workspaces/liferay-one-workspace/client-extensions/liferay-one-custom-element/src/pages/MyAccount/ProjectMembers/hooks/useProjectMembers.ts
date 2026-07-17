/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useMemo} from 'react';
import {useFetch} from '~/hooks/useFetch';
import {useAccountProjectContactRoles} from '~/hooks/useProjectCommerce';
import {
	PROJECT_ADMIN_ERC,
	PROJECT_ROLE_ERCS,
} from '~/pages/MyAccount/ProjectMembers/projectRoles';
import {Liferay} from '~/services/liferay/liferay';

import type {
	AccountMemberOption,
	ProjectMembersRow,
} from '~/pages/MyAccount/ProjectMembers/types';
import type {AccountRole, UserAccount} from '~/types/accounts';
import type {APIResponse} from '~/types/api';

type ProjectItem = {
	externalReferenceCode: string;
	id: number;
	name: string;
};

type ProjectMembershipItem = {
	id: number;
	r_projectToProjectMembership_c_projectERC: string;
	r_userToProjectMembership_userId: number;
	roleExternalReferenceCode: string;
};

type UserInfo = {
	designations: string[];
	email: string;
	name: string;
};

export function useProjectMembers() {
	const accountId = Liferay.CommerceContext?.account?.accountId;

	const {data: projectData, isLoading: projectsLoading} = useFetch<
		APIResponse<ProjectItem>
	>(accountId ? '/o/c/projects' : null, {
		params: {
			fields: 'externalReferenceCode,id,name',
			filter: `r_accountEntryToProject_accountEntryId eq '${accountId}'`,
			pageSize: -1,
			sort: 'name:asc',
		},
	});

	const {
		data: membershipData,
		isLoading: membershipsLoading,
		mutate: mutateMemberships,
	} = useFetch<APIResponse<ProjectMembershipItem>>(
		accountId ? '/o/c/projectmemberships' : null,
		{
			params: {
				fields: 'id,r_projectToProjectMembership_c_projectERC,r_userToProjectMembership_userId,roleExternalReferenceCode',
				filter: `r_accountEntryToProjectMembership_accountEntryId eq '${accountId}'`,
				pageSize: -1,
			},
		}
	);

	const {
		data: userAccountData,
		isLoading: userAccountsLoading,
		mutate: mutateUserAccounts,
	} = useFetch<APIResponse<UserAccount>>(
		accountId
			? `/o/headless-admin-user/v1.0/accounts/${accountId}/user-accounts`
			: null,
		{params: {pageSize: -1, sort: 'givenName:asc'}}
	);

	const {
		contactRoleExternalReferenceCodesByProjectId,
		loading: contactRolesLoading,
	} = useAccountProjectContactRoles();

	const {data: accountRoleData, isLoading: accountRolesLoading} = useFetch<
		APIResponse<AccountRole>
	>(
		accountId
			? `/o/headless-admin-user/v1.0/accounts/${accountId}/account-roles`
			: null,
		{params: {pageSize: -1}}
	);

	const contactRoleNameByExternalReferenceCode = useMemo(() => {
		const map = new Map<string, string>();

		(accountRoleData?.items ?? []).forEach((accountRole) => {
			if (accountRole.externalReferenceCode) {
				map.set(accountRole.externalReferenceCode, accountRole.name);
			}
		});

		return map;
	}, [accountRoleData]);

	const contactDesignationNames = useMemo(() => {
		const names = new Set<string>();

		contactRoleExternalReferenceCodesByProjectId.forEach(
			(externalReferenceCodes) =>
				externalReferenceCodes.forEach((externalReferenceCode) => {
					const name = contactRoleNameByExternalReferenceCode.get(
						externalReferenceCode
					);

					if (name) {
						names.add(name);
					}
				})
		);

		return names;
	}, [
		contactRoleExternalReferenceCodesByProjectId,
		contactRoleNameByExternalReferenceCode,
	]);

	const userInfoById = useMemo(() => {
		const map = new Map<number, UserInfo>();

		(userAccountData?.items ?? []).forEach((userAccount) => {
			const accountRoleBriefs =
				userAccount.accountBriefs?.find(
					(accountBrief) => accountBrief.id === accountId
				)?.roleBriefs ?? [];

			map.set(userAccount.id, {
				designations: accountRoleBriefs
					.map((roleBrief) => roleBrief.name)
					.filter((roleName) =>
						contactDesignationNames.has(roleName)
					),
				email: userAccount.emailAddress,
				name: userAccount.name,
			});
		});

		return map;
	}, [accountId, contactDesignationNames, userAccountData]);

	const accountMemberOptions = useMemo<AccountMemberOption[]>(
		() =>
			(userAccountData?.items ?? []).map((userAccount) => ({
				email: userAccount.emailAddress,
				name: userAccount.name,
				userId: userAccount.id,
			})),
		[userAccountData]
	);

	const rows = useMemo<ProjectMembersRow[]>(() => {
		const membershipsByProject = new Map<string, ProjectMembershipItem[]>();

		(membershipData?.items ?? []).forEach((membership) => {
			const projectExternalReferenceCode =
				membership.r_projectToProjectMembership_c_projectERC;

			const memberships =
				membershipsByProject.get(projectExternalReferenceCode) ?? [];

			memberships.push(membership);

			membershipsByProject.set(projectExternalReferenceCode, memberships);
		});

		return (projectData?.items ?? []).map((project) => {
			const memberships =
				membershipsByProject.get(project.externalReferenceCode) ?? [];

			const members = memberships
				.filter((membership) =>
					PROJECT_ROLE_ERCS.includes(
						membership.roleExternalReferenceCode
					)
				)
				.map((membership) => {
					const userId = membership.r_userToProjectMembership_userId;

					const userInfo = userInfoById.get(userId);

					return {
						designations: userInfo?.designations ?? [],
						email: userInfo?.email ?? '',
						membershipId: membership.id,
						name: userInfo?.name ?? '',
						roleExternalReferenceCode:
							membership.roleExternalReferenceCode,
						userId,
					};
				});

			members.sort((a, b) => {
				if (!a.name) {
					return b.name ? 1 : 0;
				}

				if (!b.name) {
					return -1;
				}

				return a.name.localeCompare(b.name);
			});

			return {
				availableDesignations: (
					contactRoleExternalReferenceCodesByProjectId.get(
						project.id
					) ?? []
				)
					.map((externalReferenceCode) =>
						contactRoleNameByExternalReferenceCode.get(
							externalReferenceCode
						)
					)
					.filter((name): name is string => Boolean(name)),
				externalReferenceCode: project.externalReferenceCode,
				hasProjectAdmin: members.some(
					(member) =>
						member.roleExternalReferenceCode === PROJECT_ADMIN_ERC
				),
				id: project.id,
				members,
				name: project.name,
			};
		});
	}, [
		contactRoleExternalReferenceCodesByProjectId,
		contactRoleNameByExternalReferenceCode,
		membershipData,
		projectData,
		userInfoById,
	]);

	const mutate = async () => {
		await Promise.all([mutateMemberships(), mutateUserAccounts()]);
	};

	return {
		accountMemberOptions,
		loading:
			accountRolesLoading ||
			contactRolesLoading ||
			membershipsLoading ||
			projectsLoading ||
			userAccountsLoading,
		mutate,
		rows,
	};
}
