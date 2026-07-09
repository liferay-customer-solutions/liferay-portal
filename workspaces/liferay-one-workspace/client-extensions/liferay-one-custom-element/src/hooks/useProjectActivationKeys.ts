/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {differenceInDays, format} from 'date-fns';
import {useFetch} from '~/hooks/useFetch';
import {Word} from '~/i18n';
import {Liferay} from '~/services/liferay/liferay';

import type {APIResponse} from '~/types/api';

export type ProjectActivationKey = {
	badge?: Word;
	domain: string;
	expirationDate: string;
	id: string;
	name: string;
	startDate: string;
	status: string;
};

type LicenseKeyNode = {
	active: boolean;
	customExpirationDate?: string;
	dateCreated?: string;
	domains?: string;
	externalReferenceCode: string;
	name: string;
	startDate?: string;
};

const NEW_KEY_WINDOW_DAYS = 15;

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

function formatDate(value?: string): string {
	return value ? format(new Date(value), 'MMM d, yyyy') : '';
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
			badge: getBadge(node),
			domain: node.domains ?? '',
			expirationDate: formatDate(node.customExpirationDate),
			id: node.externalReferenceCode,
			name: node.name,
			startDate: formatDate(node.startDate),
			status: node.active ? 'active' : 'expired',
		})
	);

	return {activationKeys, error, loading};
}
