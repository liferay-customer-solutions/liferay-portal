/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import CreateFilters from '../../core/CreateFilters';
import {
	FilterSchemaOption,
	filterSchema as filterSchemas,
} from '../../schema/filters';
import {downloadFile} from '../../utils/file';
import {OneSpringBootOAuth2} from './OAuth2Client';

class OneOAuth2 extends OneSpringBootOAuth2 {
	async downloadOrderReport(
		filter: {
			[key: string]: string;
		},
		filterSchema?: FilterSchemaOption
	) {
		const searchBuilder = CreateFilters.createFilter({
			appliedFilter: filter,
			filterSchema: (filterSchemas as any)[filterSchema ?? ''],
		});

		const response = await this.get<Response>(
			`/orders/export?filters=${searchBuilder}`,
			{earlyReturn: true}
		);

		await downloadFile('orders.csv', response);
	}
}

const oneOAuth2 = new OneOAuth2('');

export default oneOAuth2;
