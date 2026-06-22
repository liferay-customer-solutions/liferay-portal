/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import * as OAuth2 from '@liferay/oauth2-provider-web/client';

import FetcherError from '../../../../services/fetcher/FetcherError';

const BASE_PATH = '/admin/debug-message-queue';
const OAUTH2_APP = 'liferay-one-etc-spring-boot-oaua';

export type RoutingKey = {
	routingKey: string;
	subscriber: string;
};

async function parseError(response: Error | Response): Promise<Response> {
	if (response instanceof Response && !response.ok) {
		const error = new FetcherError('An error occurred while fetching.');

		error.info = await response.json().catch(() => undefined);
		error.status = response.status;

		throw error;
	}

	return response as Response;
}

async function request<T>(path: string, options?: RequestInit): Promise<T> {
	const oAuth2Client = await OAuth2.FromUserAgentApplication(OAUTH2_APP);

	const response = await oAuth2Client
		.fetch(`${BASE_PATH}${path}`, options)
		.catch(parseError);

	if (!(response instanceof Response)) {
		return response as T;
	}

	if (!response.ok) {
		throw await parseError(response);
	}

	if (response.headers.get('Content-Length') === '0') {
		return undefined as T;
	}

	return response.json();
}

export function dispatchMessage(message: {
	message: string;
	properties: string;
	routingKey: string;
}): Promise<void> {
	return request('', {
		body: JSON.stringify(message),
		headers: {'Content-Type': 'application/json'},
		method: 'POST',
	});
}

export function getRoutingKeys(): Promise<RoutingKey[]> {
	return request('/routing-keys');
}
