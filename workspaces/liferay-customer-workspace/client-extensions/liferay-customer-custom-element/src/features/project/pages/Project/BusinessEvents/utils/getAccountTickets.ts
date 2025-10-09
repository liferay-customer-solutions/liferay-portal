/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Liferay} from '~/services/liferay';
import {IBusinessEvent, ITicket} from '~/utils/types';

export default async function getAccountTickets(
	externalReferenceCode: string,
	businessEvent?: IBusinessEvent,
	featureFlags: string[] = []
): Promise<ITicket[]> {
	if (!externalReferenceCode) {
		return [];
	}

	try {
		let ticketsParam = '';

		if (businessEvent && featureFlags.includes('LRSD-8280')) {
			const associatedTickets = JSON.parse(
				businessEvent.associatedTickets || '[]'
			);

			ticketsParam = `?ticketIds=${associatedTickets.join(',')}`;
		}

		const response = await Liferay.OAuth2Client.FromUserAgentApplication(
			'liferay-customer-etc-spring-boot-oaua'
		).fetch(`/accounts/${externalReferenceCode}/tickets${ticketsParam}`);

		const data = await response.json();

		return data || [];
	}
	catch (error) {
		console.error('Error fetching tickets:', error);

		return [];
	}
}
