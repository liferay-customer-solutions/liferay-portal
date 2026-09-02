/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import FetcherError from '~/services/fetcher/FetcherError';
import {downloadFile} from '~/utils/downloadFileUtils';

import {OneSpringBootOAuth2} from './OAuth2Client';

class CloudOAuth2 extends OneSpringBootOAuth2 {
	async downloadOfflineActivationBundle(
		dxpVersion: string,
		environmentId: string
	) {
		const response = await this.post<Response>(
			`/environments/${environmentId}/offline-activation-bundle`,
			{dxpVersion},
			{earlyReturn: true}
		);

		if (!response.ok) {
			throw this.toFetcherError(response);
		}

		await downloadFile(
			`${environmentId}-${dxpVersion}-offline-activation-bundle.zip`,
			response
		);
	}

	async getProjectsEntitlementsDisasterRecovery(
		projectExternalReferenceCode: string
	) {
		return this.get<{hasDisasterRecoveryEntitlement: boolean}>(
			`/projects/${projectExternalReferenceCode}/entitlements` +
				'/disaster-recovery'
		);
	}

	async offlineActivation(activationCode: string, token: string) {
		const response = await this.post<Response>(
			'/environments/offline-activation',
			{activationCode, token},
			{earlyReturn: true}
		);

		if (!response.ok) {
			throw this.toFetcherError(response);
		}
	}

	async postEnvironmentsActivationRequest(
		environmentProfile: string,
		fields: Record<string, unknown>,
		projectExternalReferenceCode: string
	) {
		const response = await this.post<Response>(
			'/environments/activation-request',
			{environmentProfile, ...fields, projectExternalReferenceCode},
			{earlyReturn: true}
		);

		if (!response.ok) {
			throw this.toFetcherError(response);
		}
	}

	private toFetcherError(response: Response) {
		const error = new FetcherError(
			'An error occurred while fetching the data.'
		);

		error.status = response.status;

		return error;
	}
}

const Cloud = new CloudOAuth2('/cloud');

export default Cloud;
