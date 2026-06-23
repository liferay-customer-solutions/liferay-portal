/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {
	FilterSchemaOption,
	filterSchema as filterSchemas,
} from '~/types/filters';
import CreateFilters from '~/utils/CreateFilters';
import {downloadFile} from '~/utils/downloadFile';

import {OneSpringBootOAuth2} from './OAuth2Client';

class OrdersOAuth2 extends OneSpringBootOAuth2 {
	async downloadOrderReport(
		filter: {
			[key: string]: string;
		},
		filterSchema?: FilterSchemaOption
	) {
		const searchBuilder = CreateFilters.createFilter({
			appliedFilter: filter,
			filterSchema: filterSchemas[filterSchema as FilterSchemaOption],
		});

		const response = await this.get<Response>(
			`/export?filters=${searchBuilder}`,
			{earlyReturn: true}
		);

		await downloadFile('orders.csv', response);
	}
}

const Orders = new OrdersOAuth2('/orders');

export default Orders;
