/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

export interface IBusinessEvent {
	actualEventDate?: string;
	associatedTickets?: string;
	currentLiferayVersion?: {key: string; name: string};
	description?: string;
	eventStatus?: {key: string; name: string};
	eventType?: {key: string; name: string};
	id?: string;
	lastComment?: string;
	name?: string;
	newLiferayVersion?: {key: string; name: string};
	plannedEventDate?: string;
	plannedEventTime?: ITimeInput | string;
	timeZone?: {key: string; name: string};
}

export interface IBusinessEventVersion {
	author?: string;
	change?: {name: string};
	comment?: string;
	createdDate?: string;
}

export interface ITicket {
	link: string;
	selected?: boolean;
	status: string;
	subject: string;
	ticketId: string;
}
