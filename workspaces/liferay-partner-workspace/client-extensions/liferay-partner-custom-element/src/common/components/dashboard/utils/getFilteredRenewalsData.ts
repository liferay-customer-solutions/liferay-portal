/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import OpportunityPartnerRole from '../../../interfaces/opportunityPartnerRole';

const TODAY = new Date();
const MILISECONDS_PER_DAY = 1000 * 3600 * 24;

export default function getFilteredRenewals(data: any) {
	const newRenewalsArray: OpportunityPartnerRole[] = [];

	data?.items?.forEach((renewal: OpportunityPartnerRole) => {
		const expirationDate = new Date(renewal.closeDate);
		const differenceOfTime = expirationDate.getTime() - TODAY.getTime();

		const differenceOfDays =
			Math.floor(differenceOfTime / MILISECONDS_PER_DAY) + 1;

		newRenewalsArray.push({
			...renewal,
			expirationDays: differenceOfDays,
		});
	});

	return newRenewalsArray.slice(0, 4);
}
