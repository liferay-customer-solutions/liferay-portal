/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {differenceInDays, format} from 'date-fns';
import {useFetch} from '~/hooks/useFetch';
import {Word} from '~/i18n';
import {Liferay} from '~/services/liferay/liferay';

import type {APIResponse} from '~/types/api';

export type ProjectActivationKeyProduct = {
	externalReferenceCode: string;
	name: string;
	sizing: string;
};

export type ProjectActivationKey = {
	active: boolean;
	badge?: Word;
	clusterSize: string;
	description: string;
	domain: string;
	environmentType: Word;
	expirationDate: string;
	expirationDateValue: string;
	hostName: string;
	id: string;
	keyType: string;
	licenseKeyId: string;
	licenseType: string;
	name: string;
	products: ProjectActivationKeyProduct[];
	startDate: string;
	startDateValue: string;
	status: string;
};

type LicenseKeyNode = {
	active: boolean;
	additionalInfo?: string;
	customExpirationDate?: string;
	dateCreated?: string;
	description?: string;
	domains?: string;
	externalReferenceCode: string;
	hostName?: string;
	id?: number;
	licenseType?: string;
	maxClusterNodes?: number;
	maxServers?: number;
	name: string;
	startDate?: string;
};

const NEW_KEY_WINDOW_DAYS = 15;

const NON_PRODUCTION_LICENSE_TYPES = ['developer', 'developer-cluster', 'free'];

const RENEWAL_WINDOW_DAYS = 90;

function getBadge(node: LicenseKeyNode): Word | undefined {
	if (
		node.dateCreated &&
		differenceInDays(new Date(), new Date(node.dateCreated)) <=
			NEW_KEY_WINDOW_DAYS
	) {
		return 'new-activation-key';
	}

	if (!node.active || !node.customExpirationDate) {
		return undefined;
	}

	const daysUntilExpiration = differenceInDays(
		new Date(node.customExpirationDate),
		new Date()
	);

	if (
		daysUntilExpiration >= 0 &&
		daysUntilExpiration <= RENEWAL_WINDOW_DAYS
	) {
		return 'to-be-renewed';
	}

	return undefined;
}

function getClusterSize(node: LicenseKeyNode): string {
	const nodes = node.maxClusterNodes ?? node.maxServers;

	return nodes ? String(nodes) : '';
}

function getEnvironmentType(licenseType?: string): Word {
	if (licenseType && NON_PRODUCTION_LICENSE_TYPES.includes(licenseType)) {
		return 'non-production';
	}

	return 'production';
}

function getKeyType(licenseType?: string): string {
	if (!licenseType) {
		return '';
	}

	return licenseType
		.split('-')
		.map((word) => word.charAt(0).toUpperCase() + word.slice(1))
		.join(' ');
}

function formatDate(value?: string): string {
	return value ? format(new Date(value), 'MMM d, yyyy') : '';
}

function getDateValue(value?: string): string {
	return value ? format(new Date(value), 'yyyy-MM-dd') : '';
}

function getProducts(additionalInfo?: string): ProjectActivationKeyProduct[] {
	if (!additionalInfo) {
		return [];
	}

	try {
		const parsed = JSON.parse(additionalInfo);

		return (parsed.products ?? []).map(
			(product: Partial<ProjectActivationKeyProduct>) => ({
				externalReferenceCode: product.externalReferenceCode ?? '',
				name: product.name ?? '',
				sizing: product.sizing ?? '',
			})
		);
	}
	catch {
		return [];
	}
}

export function useProjectActivationKeys(productName?: string) {
	const accountId = Liferay.CommerceContext.account?.accountId;

	const filters = [
		`r_accountEntryToLicenseKey_accountEntryId eq '${accountId}'`,
	];

	if (productName) {
		filters.push(`productName eq '${productName}'`);
	}

	const {
		data,
		error,
		isLoading: loading,
		revalidate,
	} = useFetch<APIResponse<LicenseKeyNode>>(
		accountId ? '/o/c/licensekeys' : null,
		{
			params: {
				filter: filters.join(' and '),
				pageSize: 200,
				sort: 'startDate:desc',
			},
		}
	);

	const activationKeys: ProjectActivationKey[] = (data?.items ?? []).map(
		(node) => ({
			active: node.active,
			badge: getBadge(node),
			clusterSize: getClusterSize(node),
			description: node.description ?? '',
			domain: node.domains ?? '',
			environmentType: getEnvironmentType(node.licenseType),
			expirationDate: formatDate(node.customExpirationDate),
			expirationDateValue: getDateValue(node.customExpirationDate),
			hostName: node.hostName ?? '',
			id: node.externalReferenceCode,
			keyType: getKeyType(node.licenseType),
			licenseKeyId: node.id ? String(node.id) : '',
			licenseType: node.licenseType ?? '',
			name: node.name,
			products: getProducts(node.additionalInfo),
			startDate: formatDate(node.startDate),
			startDateValue: getDateValue(node.startDate),
			status: node.active ? 'active' : 'expired',
		})
	);

	return {activationKeys, error, loading, revalidate};
}
