/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useMemo} from 'react';
import {useFetch} from '~/hooks/useFetch';
import {CLOUD_CONTACT_DESIGNATIONS} from '~/pages/MyAccount/AccountMembers/accountRoles';
import {PROJECT_ADMIN_ERC} from '~/pages/MyAccount/ProjectMembers/projectRoles';
import {Liferay} from '~/services/liferay/liferay';

import type {
	AccountMemberOption,
	ProjectMembersRow,
} from '~/pages/MyAccount/ProjectMembers/types';
import type {UserAccount} from '~/types/accounts';
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
			pageSize: 200,
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
				pageSize: 300,
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
		{params: {pageSize: 100, sort: 'givenName:asc'}}
	);

	const userInfoById = useMemo(() => {
		const map = new Map<number, UserInfo>();

		(userAccountData?.items ?? []).forEach((userAccount) => {
			map.set(userAccount.id, {
				designations: (userAccount.roleBriefs ?? [])
					.map((roleBrief) => roleBrief.name)
					.filter((roleName) =>
						CLOUD_CONTACT_DESIGNATIONS.includes(roleName)
					),
				email: userAccount.emailAddress,
				name: userAccount.name,
			});
		});

		return map;
	}, [userAccountData]);

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

			const members = memberships.map((membership) => {
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

			members.sort((a, b) => a.name.localeCompare(b.name));

			return {
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
	}, [membershipData, projectData, userInfoById]);

	const mutate = async () => {
		await Promise.all([mutateMemberships(), mutateUserAccounts()]);
	};

	return {
		accountMemberOptions,
		loading: projectsLoading || membershipsLoading || userAccountsLoading,
		mutate,
		rows,
	};
}
