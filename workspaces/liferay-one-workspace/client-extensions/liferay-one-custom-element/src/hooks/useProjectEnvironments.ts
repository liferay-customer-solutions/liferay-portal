/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useFetch} from '~/hooks/useFetch';
import {Liferay} from '~/services/liferay/liferay';

import type {APIResponse} from '~/types/api';

export type ProjectEnvironment = {
	activationCode: string;
	activationMode: string;
	adminEmailAddress: string;
	adminFirstName: string;
	adminLastName: string;
	aiHubURL: string;
	allowedEmailDomains: string;
	analyticsCloudOwnerEmailAddress: string;
	currentEntitlementHash: string;
	disasterRecoveryRegion: string;
	domains: string;
	externalReferenceCode: string;
	friendlyURL: string;
	githubUsername: string;
	hostName: string;
	id: string;
	name: string;
	offering: string;
	ownerEmailAddress: string;
	projectExternalReferenceCode: string;
	projectId: string;
	region: string;
	status: string;
	timeZone: string;
	tokenMonthlyAllowance: string;
	type: string;
	workspaceName: string;
};

type EnvironmentNode = {
	activationCode?: string;
	activationMode?: string;
	activationStatus?: string;
	adminEmailAddress?: string;
	adminFirstName?: string;
	adminLastName?: string;
	aiHubURL?: string;
	allowedEmailDomains?: string;
	analyticsCloudOwnerEmailAddress?: string;
	currentEntitlementHash?: string;
	disasterRecoveryRegion?: string;
	domains?: string;
	externalReferenceCode: string;
	friendlyURL?: string;
	githubUsername?: string;
	hostName?: string;
	id: number;
	name?: string;
	offering?: string;
	ownerEmailAddress?: string;
	projectId?: string;
	r_projectToEnvironment_c_projectERC?: string;
	region?: string;
	timeZone?: string;
	tokenMonthlyAllowance?: string;
	type?: string;
	workspaceName?: string;
};

export function useProjectEnvironments() {
	const accountId = Liferay.CommerceContext.account?.accountId;

	const {
		data,
		error,
		isLoading: loading,
		mutate,
	} = useFetch<APIResponse<EnvironmentNode>>(
		accountId ? '/o/c/environments' : null,
		{
			params: {
				filter: `r_accountEntryToEnvironment_accountEntryId eq '${accountId}'`,
				pageSize: 200,
				sort: 'offering:asc',
			},
		}
	);

	const environments: ProjectEnvironment[] = (data?.items ?? []).map(
		(node) => ({
			activationCode: node.activationCode ?? '',
			activationMode: node.activationMode ?? '',
			adminEmailAddress: node.adminEmailAddress ?? '',
			adminFirstName: node.adminFirstName ?? '',
			adminLastName: node.adminLastName ?? '',
			aiHubURL: node.aiHubURL ?? '',
			allowedEmailDomains: node.allowedEmailDomains ?? '',
			analyticsCloudOwnerEmailAddress:
				node.analyticsCloudOwnerEmailAddress ?? '',
			currentEntitlementHash: node.currentEntitlementHash ?? '',
			disasterRecoveryRegion: node.disasterRecoveryRegion ?? '',
			domains: node.domains ?? '',
			externalReferenceCode: node.externalReferenceCode,
			friendlyURL: node.friendlyURL ?? '',
			githubUsername: node.githubUsername ?? '',
			hostName: node.hostName ?? '',
			id: String(node.id),
			name: node.name ?? '',
			offering: node.offering ?? '',
			ownerEmailAddress: node.ownerEmailAddress ?? '',
			projectExternalReferenceCode:
				node.r_projectToEnvironment_c_projectERC ?? '',
			projectId: node.projectId ?? '',
			region: node.region ?? '',
			status: node.activationStatus ?? '',
			timeZone: node.timeZone ?? '',
			tokenMonthlyAllowance: node.tokenMonthlyAllowance ?? '',
			type: node.type ?? '',
			workspaceName: node.workspaceName ?? '',
		})
	);

	return {environments, error, loading, mutate};
}

export default useProjectEnvironments;
