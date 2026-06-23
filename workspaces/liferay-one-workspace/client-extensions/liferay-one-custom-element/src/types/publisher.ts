/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import type {Order} from './orders';

export type PublisherDetailsEntry = {
	catalogId?: number;
	dateCreated?: string;
	dateModified?: string;
	description?: string;
	email?: string;
	externalReferenceCode?: string;
	fullName?: string;
	id?: number;
	location?: string;
	paypalAccount?: string;
	phone?: string;
	privateEmail?: string;
	publisherName?: string;
	publisherProfileImageURL?: string;
	role?: string;
	salesEmail?: string;
	showContactForm?: boolean;
	showOnHomePage?: boolean;
	supportEmail?: string;
	websiteURL?: string;
};

export type PublisherRequestInfo = {
	creator: {name: string};
	dateCreated: string;
	emailAddress?: string;
	extension?: string;
	firstName?: string;
	id?: number;
	lastName?: string;
	phone?: {
		code: string;
		flag: string;
	};
	phoneNumber?: string;
	publisherType: string[];
	requestDescription?: string;
	requestStatus?: {
		key: string;
		name: string;
	};
};

export type PublisherSalesSummaryEntry = {
	dateCreated: string;
	dateModified: string;
	externalReferenceCode: string;
	id: number;
	objectEntryFolderExternalReferenceCode: string;
	objectEntryFolderId: number;
	paidBy: string;
	paidDate: string;
	paymentStatus: {
		key: string;
		name: string;
	};
	publisherName: string;
	publisherToAccountERC: string;
	publisherToCommerceOrder: Order[];
	quarter: string;
	r_accountEntryToPublisher_accountEntryERC: string;
	r_accountEntryToPublisher_accountEntryId: number;
	scopeId: number;
};
