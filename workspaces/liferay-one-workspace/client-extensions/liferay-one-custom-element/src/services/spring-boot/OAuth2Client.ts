/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import * as OAuth2 from '@liferay/oauth2-provider-web/client';
import FetcherError from '~/services/fetcher/FetcherError';

type Options<T> = RequestInit & {
	earlyReturn?: boolean;

	parseResponse?: (response: Response) => T;
};

class OAuth2Client {
	private oAuth2ClientPromise?: ReturnType<
		typeof OAuth2.FromUserAgentApplication
	>;

	constructor(
		protected agentName: string,
		protected basePath: string
	) {}

	private getOAuth2Client() {
		if (!this.oAuth2ClientPromise) {
			this.oAuth2ClientPromise = OAuth2.FromUserAgentApplication(
				this.agentName
			);
		}

		return this.oAuth2ClientPromise;
	}

	private async parseError(response: Response | Error) {
		if (response instanceof Response && !response.ok) {
			const error = new FetcherError(
				'An error occurred while fetching the data.'
			);

			if (response.headers.get('Content-Length') !== '0') {
				error.info = await response.json();
			}

			error.status = response.status;

			throw error;
		}

		if (response instanceof Response) {
			return response;
		}

		throw response;
	}

	private fetcher = async <T = unknown>(
		resource: RequestInfo,
		options?: Options<T>
	): Promise<T> => {
		const oAuth2Client = await this.getOAuth2Client();

		const response = (await oAuth2Client
			.fetch(`${this.basePath + resource}`, options)
			.catch(this.parseError.bind(this))) as Response;

		if (options?.earlyReturn || !(response instanceof Response)) {
			return response as T;
		}

		if (!response.ok) {
			throw this.parseError(response);
		}

		if (
			options?.method === 'DELETE' ||
			response.status === 204 ||
			response.headers.get('Content-Length') === '0'
		) {
			return {} as T;
		}

		if (options?.parseResponse) {
			return options.parseResponse(response);
		}

		return response.json();
	};

	protected async delete(resource: RequestInfo) {
		await this.fetcher(resource, {method: 'DELETE'});
	}

	protected get<T>(resource: RequestInfo, options?: Options<T>): Promise<T> {
		return this.fetcher(resource, options);
	}

	protected patch<T>(
		resource: RequestInfo,
		data?: unknown,
		options?: Options<T>
	) {
		return this.fetcher(resource, {
			...options,
			body: JSON.stringify(data),
			method: 'patch',
		});
	}

	protected put<T>(
		resource: RequestInfo,
		data?: unknown,
		options?: Options<T>
	) {
		return this.fetcher(resource, {
			...options,
			body: JSON.stringify(data),
			method: 'put',
		});
	}

	protected post<T>(
		resource: RequestInfo,
		data?: FormData | unknown,
		options?: Options<T>
	) {
		return this.fetcher(resource, {
			...options,
			body: data instanceof FormData ? data : JSON.stringify(data),
			method: 'POST',
		});
	}
}

export class OneSpringBootOAuth2 extends OAuth2Client {
	constructor(resource: string) {
		super('liferay-one-etc-spring-boot-oaua', resource);
	}
}

export default OAuth2Client;
