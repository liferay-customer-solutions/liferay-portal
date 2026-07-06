/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useOneContext} from '~/context/OneContextProvider';
import {useFetch} from '~/hooks/useFetch';
import {Liferay} from '~/services/liferay/liferay';

import type {UserProject} from '~/pages/MyAccount/Projects/types';
import type {APIResponse} from '~/types/api';

type ProjectAPIItem = {
	externalReferenceCode: string;
	id: number;
	name: string;
};

type ProjectMembershipAPIItem = {
	r_projectToProjectMembership_c_projectERC: string;
};

export function useUserProjects(): {
	hasAccountProjects: boolean;
	loading: boolean;
	projects: UserProject[];
} {
	const accountId = Liferay.CommerceContext?.account?.accountId;
	const userId = Liferay.ThemeDisplay.getUserId();

	const {userAccountModel} = useOneContext();

	const showAllAccountProjects = Boolean(
		userAccountModel?.isAccountAdministrator || userAccountModel?.isAdmin
	);

	const enabled = Boolean(accountId && userId);

	const {data: membershipData, isLoading: membershipsLoading} = useFetch<
		APIResponse<ProjectMembershipAPIItem>
	>(enabled && !showAllAccountProjects ? '/o/c/projectmemberships' : null, {
		params: {
			fields: 'r_projectToProjectMembership_c_projectERC',
			filter: `r_accountEntryToProjectMembership_accountEntryId eq '${accountId}' and r_userToProjectMembership_userId eq '${userId}'`,
			pageSize: 200,
		},
	});

	const {data: projectData, isLoading: projectsLoading} = useFetch<
		APIResponse<ProjectAPIItem>
	>(enabled ? '/o/c/projects' : null, {
		params: {
			filter: `r_accountEntryToProject_accountEntryId eq '${accountId}'`,
			pageSize: 200,
			sort: 'name:asc',
		},
	});

	const memberProjectExternalReferenceCodes = new Set(
		(membershipData?.items ?? []).map(
			(item) => item.r_projectToProjectMembership_c_projectERC
		)
	);

	const projects: UserProject[] = (projectData?.items ?? [])
		.filter(
			(item) =>
				showAllAccountProjects ||
				memberProjectExternalReferenceCodes.has(
					item.externalReferenceCode
				)
		)
		.map((item) => ({
			externalReferenceCode: item.externalReferenceCode,
			id: item.id,
			name: item.name,
		}));

	return {
		hasAccountProjects: (projectData?.items ?? []).length > 0,
		loading: membershipsLoading || projectsLoading,
		projects,
	};
}
