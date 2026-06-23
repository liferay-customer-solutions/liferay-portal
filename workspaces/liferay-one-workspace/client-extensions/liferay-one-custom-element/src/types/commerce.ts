/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import type {Account} from './accounts';

export type Catalog = {
	accountId: number | null;
	currencyCode: string;
	defaultLanguageId: string;
	externalReferenceCode: string;
	id: number;
	name: string;
	system: boolean;
};

export type Channel = {
	channelId: number;
	currencyCode: string;
	externalReferenceCode: string;
	id: number;
	name: string;
	siteGroupId: number;
	type: string;
};

export type CommerceAccount = {
	active: boolean;
	logoURL: string;
	taxId: string;
} & Omit<Account, 'description'>;

export type CommerceOption = {
	id: number;
	key: string;
	name: string;
};

export type CurrencyAbbreviation = 'USD';
