/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import i18n from '~/i18n';

import {DetailsRow} from '../components/DetailsCard/DetailsCard';
import {DetailsSection} from '../components/SectionedDetailsCard/SectionedDetailsCard';

import type {DetailsEnvironmentMock, DetailsMock} from './detailsMockData';
import type {DetailsProfile} from './getDetailsProfile';

type DetailsContext = {
	accountName: string;
	mock: DetailsMock;
	orderInfo: {
		orderDate: string;
		orderId: string;
		purchaseNumber: string;
		purchasedBy: string;
	};
};

function orderRows(context: DetailsContext): DetailsRow[] {
	const {accountName, orderInfo} = context;

	return [
		{label: i18n.translate('order-id'), value: orderInfo.orderId},
		{label: i18n.translate('order-date'), value: orderInfo.orderDate},
		{
			label: i18n.translate('purchase-number'),
			value: orderInfo.purchaseNumber,
		},
		{label: i18n.translate('customer-account'), value: accountName},
		{label: i18n.translate('purchased-by'), value: orderInfo.purchasedBy},
	];
}

function datesStatusRows(mock: DetailsMock): DetailsRow[] {
	return [
		{label: i18n.translate('start-date'), value: mock.startDate},
		{label: i18n.translate('expiration-date'), value: mock.expirationDate},
		{label: i18n.translate('status'), value: mock.status},
	];
}

function instanceEnvironmentRows(
	environment: DetailsEnvironmentMock
): DetailsRow[] {
	return [
		{label: i18n.translate('instance-size'), value: environment.instanceSize},
		{
			label: i18n.translate('keys-provisioned'),
			value: environment.keysProvisioned,
		},
		{label: i18n.translate('start-date'), value: environment.startDate},
		{
			label: i18n.translate('expiration-date'),
			value: environment.expirationDate,
		},
		{label: i18n.translate('status'), value: environment.status},
	];
}

function commerceEnvironmentRows(
	environment: DetailsEnvironmentMock
): DetailsRow[] {
	return [
		{label: i18n.translate('purchased'), value: environment.purchased},
		{label: i18n.translate('start-date'), value: environment.startDate},
		{
			label: i18n.translate('expiration-date'),
			value: environment.expirationDate,
		},
		{label: i18n.translate('status'), value: environment.status},
	];
}

export function buildDetailsSections(
	profile: DetailsProfile,
	context: DetailsContext
): DetailsSection[] {
	const {mock} = context;

	switch (profile) {
		case 'analytics':
			return [
				{
					rows: [
						{label: i18n.translate('tier-name'), value: mock.tierName},
						{label: i18n.translate('purchased'), value: mock.purchased},
						...datesStatusRows(mock),
						{
							label: i18n.translate('critical-incident-contacts'),
							value: mock.criticalIncidentContacts,
						},
					],
				},
			];

		case 'basic-incident':
			return [
				{
					rows: [
						...orderRows(context),
						{
							label: i18n.translate('incident-report-contacts'),
							value: mock.incidentReportContacts,
						},
					],
				},
			];

		case 'dates-status':
			return [{rows: datesStatusRows(mock)}];

		case 'env-commerce':
			return [
				{
					rows: commerceEnvironmentRows(mock.production),
					title: i18n.translate('production'),
				},
				{
					rows: commerceEnvironmentRows(mock.nonProduction),
					title: i18n.translate('non-production'),
				},
			];

		case 'env-instance':
			return [
				{
					rows: instanceEnvironmentRows(mock.production),
					title: i18n.translate('production'),
				},
				{
					rows: instanceEnvironmentRows(mock.nonProduction),
					title: i18n.translate('non-production'),
				},
			];

		case 'paas':
			return [
				{
					rows: [
						{label: i18n.translate('tier-name'), value: mock.tierName},
						...datesStatusRows(mock),
						{
							label: i18n.translate('critical-incident-contacts'),
							value: mock.criticalIncidentContacts,
						},
						...(mock.hasPaasExperience
							? [
									{
										label: i18n.translate('paas-users'),
										value: mock.paasUsers,
									},
								]
							: []),
					],
				},
			];

		case 'saas':
			return [
				{
					rows: [
						{label: i18n.translate('tier-name'), value: mock.tierName},
						...datesStatusRows(mock),
						{
							label: i18n.translate('critical-incident-contacts'),
							value: mock.criticalIncidentContacts,
						},
						{
							label: i18n.translate('privacy-breach-contacts'),
							value: mock.privacyBreachContacts,
						},
						{
							label: i18n.translate('security-breach-contacts'),
							value: mock.securityBreachContacts,
						},
					],
				},
			];

		default:
			return [{rows: orderRows(context)}];
	}
}

export default buildDetailsSections;
