/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import * as OAuth2 from '@liferay/oauth2-provider-web/client';

const OAUTH2_APP = 'liferay-one-etc-spring-boot-oaua';
const BASE_PATH = '/jira';

async function jiraFetch(
	path: string,
	options?: RequestInit
): Promise<Response> {
	const oauth2Client = await OAuth2.FromUserAgentApplication(OAUTH2_APP);

	return oauth2Client.fetch(`${BASE_PATH}${path}`, options);
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
async function jiraFetchJSON<T = any>(
	path: string,
	options?: RequestInit
): Promise<T> {
	const response = await jiraFetch(path, options);

	if (!response.ok) {
		throw new Error(`Jira API error: ${response.statusText}`);
	}

	return response.json();
}

export async function createBusinessEvent(
	externalReferenceCode: string,
	businessEvent: Record<string, unknown>
) {
	const response = await jiraFetch(
		`/accounts/${externalReferenceCode}/business-events`,
		{
			body: JSON.stringify(businessEvent),
			headers: {'Content-Type': 'application/json'},
			method: 'POST',
		}
	);

	if (!response.ok) {
		throw new Error(
			`Failed to create business event: ${response.statusText}`
		);
	}

	return response;
}

export async function getAccountTickets(
	externalReferenceCode: string,
	ticketIds?: string[]
) {
	const params = ticketIds?.length
		? `?${ticketIds.map((id) => `ticketIds=${id}`).join('&')}`
		: '';

	return jiraFetchJSON(`/accounts/${externalReferenceCode}/tickets${params}`);
}

export async function getBusinessEventById(
	externalReferenceCode: string,
	id: string
) {
	return jiraFetchJSON(
		`/accounts/${externalReferenceCode}/business-events/${id}`
	);
}

export async function getBusinessEventFieldOptions(fieldName: string) {
	const response = await jiraFetch(
		`/business-events/fields/${fieldName}/options`
	);

	if (!response.ok) {
		return {items: []};
	}

	return response.json();
}

export async function getBusinessEvents(externalReferenceCode: string) {
	const response = await jiraFetch(
		`/accounts/${externalReferenceCode}/business-events`
	);

	if (!response.ok) {
		return {items: []};
	}

	return response.json();
}

export async function getBusinessEventVersions(
	externalReferenceCode: string,
	id: string
) {
	return jiraFetchJSON(
		`/accounts/${externalReferenceCode}/business-events/${id}/versions`
	);
}

export async function getProductVersions() {
	const response = await jiraFetch('/product-versions');

	if (!response.ok) {
		return {items: []};
	}

	return response.json();
}

export async function updateBusinessEvent(
	externalReferenceCode: string,
	id: string,
	fieldsToPatch: Record<string, unknown>
) {
	return jiraFetchJSON(
		`/accounts/${externalReferenceCode}/business-events/${id}`,
		{
			body: JSON.stringify(fieldsToPatch),
			headers: {'Content-Type': 'application/json'},
			method: 'PUT',
		}
	);
}
