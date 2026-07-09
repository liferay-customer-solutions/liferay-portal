/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

export type DetailsEnvironmentMock = {
	expirationDate: string;
	instanceSize: string;
	keysProvisioned: string;
	purchased: string;
	startDate: string;
	status: string;
};

export type DetailsMock = {
	criticalIncidentContacts: string;
	expirationDate: string;
	hasPaasExperience: boolean;
	incidentReportContacts: string;
	nonProduction: DetailsEnvironmentMock;
	paasUsers: string;
	privacyBreachContacts: string;
	production: DetailsEnvironmentMock;
	purchased: string;
	securityBreachContacts: string;
	startDate: string;
	status: string;
	tierName: string;
};

export const detailsMockData: DetailsMock = {
	criticalIncidentContacts: 'ops-oncall@acme.com',
	expirationDate: 'Jan 5, 2027',
	hasPaasExperience: true,
	incidentReportContacts: 'incidents@acme.com',
	nonProduction: {
		expirationDate: 'Jan 5, 2027',
		instanceSize: '4 CPUs, 16 GB RAM',
		keysProvisioned: '1 of 2',
		purchased: '2',
		startDate: 'Jan 5, 2026',
		status: 'Active',
	},
	paasUsers: '25',
	privacyBreachContacts: 'privacy@acme.com',
	production: {
		expirationDate: 'Jan 5, 2027',
		instanceSize: '8 CPUs, 32 GB RAM',
		keysProvisioned: '2 of 2',
		purchased: '5',
		startDate: 'Jan 5, 2026',
		status: 'Active',
	},
	purchased: '5',
	securityBreachContacts: 'security@acme.com',
	startDate: 'Jan 5, 2026',
	status: 'Active',
	tierName: 'Standard',
};

export default detailsMockData;
