/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

const OAUTH2_APPLICATION_EXTERNAL_REFERENCE_CODE =
	'liferay-one-etc-spring-boot-oaua';

class Invitations {
	private baseURLPromise?: Promise<string>;

	async getAccept(token: string, signal?: AbortSignal) {
		const baseURL = await this.getBaseURL(signal);

		const response = await fetch(
			`${baseURL}/invitations/accept?token=${encodeURIComponent(token)}`,
			{signal}
		);

		if (!response.ok) {
			throw new Error(
				`Unable to accept the invitation: ${response.status}`
			);
		}

		return (await response.json()) as {status: string};
	}

	private getBaseURL(signal?: AbortSignal) {
		if (!this.baseURLPromise) {
			this.baseURLPromise = this.fetchBaseURL(signal).catch((error) => {
				this.baseURLPromise = undefined;

				throw error;
			});
		}

		return this.baseURLPromise;
	}

	private async fetchBaseURL(signal?: AbortSignal) {
		const response = await fetch(
			`/o/oauth2/application?externalReferenceCode=${OAUTH2_APPLICATION_EXTERNAL_REFERENCE_CODE}`,
			{signal}
		);

		if (!response.ok) {
			throw new Error(
				`Unable to resolve the invitation service: ${response.status}`
			);
		}

		const {homePageURL} = (await response.json()) as {
			homePageURL?: string;
		};

		if (!homePageURL) {
			throw new Error('Unable to resolve the invitation service');
		}

		return homePageURL.replace(/\/$/, '');
	}
}

export default new Invitations();
