/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import IAccountBrief from './accountBrief';
import IOrganizationBrief from './organizationBrief';
import IRoleBrief from './rolelBrief';

export default interface IUserAccount {
	accountBriefs: IAccountBrief[] | undefined;
	accountKey: string | undefined;
	code?: string;
	email: string | undefined;
	firstName: string | undefined;
	id: number | undefined;
	isAccountAdmin: boolean;
	isOmniAdmin: boolean;
	isProvisioning: boolean;
	isStaff: boolean;
	lastName: string | undefined;
	organizationBriefs: IOrganizationBrief[] | undefined;
	partnershipCurrent?: string;
	partnershipCurrentEndDate?: string;
	partnershipExpired?: string;
	partnershipExpiredEndDate?: string;
	partnershipFuture?: string;
	partnershipFutureStartDate?: string;
	region: string;
	roleBriefs: IRoleBrief[] | undefined;
	screenName: string | undefined;
	slaCurrent?: string;
	slaCurrentEndDate?: string;
	slaExpired?: string;
	slaExpiredEndDate?: string;
	slaFuture?: string;
	slaFutureStartDate?: string;
	status: string;
	userId: number | undefined;
	userName: string | undefined;
	uuid: string | undefined;
}
