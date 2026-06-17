/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import * as OAuth2 from '@liferay/oauth2-provider-web/client';

const BASE_PATH = '/admin/debug-message-queue';
const OAUTH2_APP = 'liferay-one-etc-spring-boot-oaua';

export type RoutingKey = {
	routingKey: string;
	subscriber: string;
};

async function debugMessageQueueFetch(
	path: string,
	options?: RequestInit
): Promise<Response> {
	const oAuth2Client = await OAuth2.FromUserAgentApplication(OAUTH2_APP);

	return oAuth2Client.fetch(`${BASE_PATH}${path}`, options);
}

export async function dispatchMessage(message: {
	message: string;
	properties: string;
	routingKey: string;
}): Promise<Response> {
	const response = await debugMessageQueueFetch('', {
		body: JSON.stringify(message),
		headers: {'Content-Type': 'application/json'},
		method: 'POST',
	});

	if (!response.ok) {
		throw new Error(await response.text());
	}

	return response;
}

export async function getRoutingKeys(): Promise<RoutingKey[]> {
	const response = await debugMessageQueueFetch('/routing-keys');

	if (!response.ok) {
		throw new Error(await response.text());
	}

	return response.json();
}
