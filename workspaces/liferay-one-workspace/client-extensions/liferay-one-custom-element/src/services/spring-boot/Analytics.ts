/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {OneSpringBootOAuth2} from './OAuth2Client';

class AnalyticsOAuth2 extends OneSpringBootOAuth2 {
	async provisioningOrder(orderId: number): Promise<void> {
		await this.post(`/provisioning/${orderId}`);
	}
}

const Analytics = new AnalyticsOAuth2('/analytics');

export default Analytics;
