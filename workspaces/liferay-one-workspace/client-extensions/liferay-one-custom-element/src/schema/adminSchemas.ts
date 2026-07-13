/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {z} from 'zod';
import i18n from '~/i18n';
import {
	checkRegExp,
	domainRegex,
	dsrLicenseKeyBaseSchema,
	ipv4Regex,
	macAddressRegex,
	personalInformationSchema,
	requiredDate,
	requiredKeyName,
	requiredTimeInput,
} from '~/utils/schemaUtils';

export const adminSchemas = {
	activationKey: z.object({
		...personalInformationSchema,
		domain: z.string().min(3, 'Domain is required'),
		notifyMeAboutProducts: z.boolean(),
		purpose: z.string().min(3, 'Purpose is required'),
		termsAndConditions: z.boolean().refine((value) => value === true),
		userAgreement: z.boolean().refine((value) => value === true),
	}),
	aiHubForm: z.object({
		...personalInformationSchema,
		administratorEmailAddress: z
			.string()
			.email('Please fill in valid email'),
		aiHubAccountName: z.string().min(3, 'AI Hub Account Name is required'),
		purpose: z.string().min(3, 'Purpose is required'),
		termsAndConditions: z.boolean().refine((value) => value === true),
		userAgreement: z.boolean().refine((value) => value === true),
	}),
	aiHubOpenBetaForm: z.object({
		...personalInformationSchema,
		administratorEmailAddress: z
			.string()
			.email('Please fill in valid email'),
		aiHubAccountName: z.string().min(3, 'AI Hub Account Name is required'),
	}),
	analyticsProvisioning: z.object({
		_refAllowedEmailDomains: z.array(z.any()),
		_refIncidentReportContacts: z.array(z.any()),
		acceptTerms: z.boolean().refine((value) => value, {
			message: 'You must agree with the terms',
		}),
		allowedEmailDomains: z
			.array(z.string())
			.optional()
			.default([])
			.refine(
				(values) =>
					values.length
						? values.every((value) => domainRegex.test(value))
						: true,
				'One of the chosen domains is invalid.'
			),
		dataCenterLocation: z.string(),
		friendlyWorkspaceURL: z.string().optional(),
		incidentReportContacts: z.array(z.string().email()).min(1),
		productKey: z.string().optional(),
		productName: z.string(),
		productPurchaseKey: z.string().optional(),
		workspaceName: z.string().min(3),
		workspaceOwnerEmail: z.string().email(),
	}),
	businessEventActual: z.object({
		businessEvent: z
			.object({
				actualEventDate: requiredDate,
				actualEventTime: requiredTimeInput,
				lastComment: z.string().optional(),
				timeZone: requiredKeyName,
			})
			.passthrough(),
	}),
	dsrLicenseKey: z
		.object({
			...dsrLicenseKeyBaseSchema,
			dataCenterLocation: z.string().min(1),
			workspaceName: z.string().min(3),
			workspaceOwnerEmail: z.string().email(),
		})
		.refine(
			(data) =>
				Boolean(
					data.hostname?.trim() ||
						data.ipAddress?.trim() ||
						data.macAddress?.trim()
				),
			{
				message:
					'Please complete at least one of the following fields to proceed',
				path: ['hostname'],
			}
		),
	dsrLicenseKeyServerOnly: z
		.object({
			...dsrLicenseKeyBaseSchema,
			dataCenterLocation: z.string().optional(),
			workspaceName: z.string().optional(),
			workspaceOwnerEmail: z
				.string()
				.email()
				.optional()
				.or(z.literal('')),
		})
		.refine(
			(data) =>
				Boolean(
					data.hostname?.trim() ||
						data.ipAddress?.trim() ||
						data.macAddress?.trim()
				),
			{
				message:
					'Please complete at least one of the following fields to proceed',
				path: ['hostname'],
			}
		),
	extendSSATrial: z.object({
		duration: z.coerce
			.number()
			.int()
			.min(1, 'Please enter a valid number (1-90)')
			.max(90, 'Please enter a valid number (1-90)'),
		reason: z.string().min(3),
	}),
	generateLicenseKey: z.object({
		description: z
			.string()
			.min(3)
			.max(100, {message: 'Invalid license name'}),
		hostname: z.string().optional().or(z.literal('')),
		ipAddress: z.string().refine((value) => checkRegExp(ipv4Regex, value), {
			message: 'Invalid IP address',
		}),
		macAddress: z
			.string()
			.refine((value) => checkRegExp(macAddressRegex, value), {
				message: 'Invalid MAC address',
			}),
		subscription: z
			.object({
				name: z.string(),
				productPurchasedKey: z.string(),
				productVersion: z.string(),
				skuId: z.number(),
			})
			.optional(),
	}),
	ldpProvisioning: z.object({
		_refAllowedEmailDomains: z.array(z.any()),
		_refIncidentReportContacts: z.array(z.any()),
		acceptTerms: z.boolean().refine((value) => value, {
			message: 'You must agree with the terms',
		}),
		allowedEmailDomains: z
			.array(z.string())
			.optional()
			.default([])
			.refine(
				(values) =>
					values.length
						? values.every((value) => domainRegex.test(value))
						: true,
				'One of the chosen domains is invalid.'
			),
		dataCenterLocation: z.string(),
		friendlyWorkspaceURL: z.string().optional(),
		incidentReportContacts: z.array(z.string().email()).min(1),
		productKey: z.string().optional(),
		productPurchaseKey: z.string().optional(),
		workspaceName: z.string().min(3),
		workspaceOwnerEmail: z.string().email(),
	}),
	ssaInviteUsers: z.object({
		emailAddress: z
			.string()
			.email({message: i18n.translate('please-fill-in-a-valid-email')}),
		roles: z
			.array(z.object({value: z.string()}))
			.nonempty(i18n.translate('at-least-one-role-must-be-provided')),
	}),
	ssaTrialForm: z.object({
		duration: z.coerce
			.number()
			.int()
			.min(1, 'Please enter a valid number (1-90)')
			.max(90, 'Please enter a valid number (1-90)'),
		emailAddress: z
			.array(
				z.object({
					key: z.string(),
					label: z.string(),
					value: z.string(),
				})
			)
			.refine(
				(emails) =>
					emails.every(
						(error) =>
							z.string().email().safeParse(error.value).success
					),
				{message: 'One or more email addresses are invalid'}
			)
			.optional(),
		objective: z.string().refine((val) => val, {
			message: 'Select an Option',
		}),
		projectId: z
			.string()
			.min(3, {message: 'Project ID must have at least 3 characters'})
			.regex(/^[a-zA-Z0-9]+$/, {
				message: 'Only alphanumeric characters are allowed',
			}),
		siteInitializerKey: z.string(),
	}),
};

export default adminSchemas;
