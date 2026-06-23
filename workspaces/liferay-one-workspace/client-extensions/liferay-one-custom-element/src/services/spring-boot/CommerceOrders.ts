/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {OneSpringBootOAuth2} from './OAuth2Client';

class CommerceOrdersOAuth2 extends OneSpringBootOAuth2 {
	async taxCalculate(orderId: number | string) {
		await this.post(`/${orderId}/tax-calculate`);
	}
}

const CommerceOrders = new CommerceOrdersOAuth2('/commerce-orders');

export default CommerceOrders;
