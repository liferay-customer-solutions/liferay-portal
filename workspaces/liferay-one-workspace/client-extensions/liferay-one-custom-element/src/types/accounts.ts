/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import type {CustomField} from './product';

export type Account = {
	customFields?: CustomField[];
	dateCreated: string;
	description: string;
	emailAddress: string;
	externalReferenceCode: string;
	id: number;
	logoURL?: string;
	name: string;
	numberOfUsers: number;
	status: number;
	taxId: string;
	type: string;
};

export type AccountBrief = {
	customFields?: CustomField[];
	externalReferenceCode: string;
	id: number;
	logoURL?: string;
	name: string;
	roleBriefs: RoleBrief[];
};

export type AccountGroup = {
	customFields: {};
	externalReferenceCode: string;
	id: number;
	name: string;
};

export type AccountPostalAddresses = {
	addressCountry: string;
	addressLocality: string;
	addressRegion: string;
	addressType: string;
	id: number;
	name: string;
	phoneNumber: string;
	postalCode: number;
	primary: boolean;
	streetAddressLine1: string;
	streetAddressLine2: string;
	streetAddressLine3: string;
};

export type AccountRole = {
	accountId: number;
	description: string;
	displayName: string;
	id: number;
	name: string;
	roleId: number;
};

export type AccountRoleType =
	| 'Account Administrator'
	| 'SSA Admin'
	| 'SSA User'
	| 'Solution Publisher';

export type RegularRoleType = 'Administrator' | 'Liferay Staff';

export type RoleBrief = {
	id: number;
	name: string;
};

export type UserAccount = {
	accountBriefs: AccountBrief[];
	alternateName: string;
	customFields: CustomField[];
	description: string;
	emailAddress: string;
	externalReferenceCode: string;
	familyName: string;
	givenName: string;
	id: number;
	image: string;
	isCustomerAccount: boolean;
	isPublisherAccount: boolean;
	lastLoginDate: string;
	logoURL: string;
	name: string;
	newsSubscription: boolean;
	password: string;
	roleBriefs: RoleBrief[];
	status?: number;
	type: string;
	userAccountContactInformation: {
		telephones: {
			extension: string;
			phoneNumber: string;
		}[];
	};
};
