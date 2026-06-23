/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {z} from 'zod';
import i18n from '~/i18n';
import {billingAddress, rootProjectPlanUsage} from '~/utils/schemaUtils';

export const commerceSchemas = {
	accountCreator: z.object({
		accounts: z.any().array().optional(),
		companyName: z
			.string()
			.min(1, {message: 'Please enter a company name to continue'}),
		country: z
			.string()
			.min(2, {message: 'Please select the country to continue'}),
		emailAddress: z
			.string()
			.email(i18n.translate('this-field-is-required')),
		extension: z.string().optional(),
		familyName: z
			.string()
			.min(3, {message: i18n.translate('this-field-is-required')}),
		givenName: z.string(),
		phone: z.object({
			code: z.string(),
			flag: z.string(),
		}),
		phoneNumber: z
			.string()
			.min(1, {message: 'Please enter a phone number to continue.'}),
	}),
	accountForm: z.object({
		accountImage: z.any(),
		accountName: z
			.string()
			.min(1, {message: 'Please enter a company name to continue'}),
		accountType: z.string().min(1),
		billingAddress,
		emailAddress: z.string().email('Please fill in valid email'),
		taxNumber: z
			.string()
			.min(1, {message: 'Please enter a Tax/VAT number to continue'}),
	}),
	billingAddress,
	contactSales: z.object({
		accountName: z
			.string()
			.min(3, i18n.sub('x-is-required', 'account-name')),
		additionalAppsRequested: z.string(),
		comments: z.string(),
		email: z.string().email(i18n.translate('please-fill-in-a-valid-email')),
		name: z.string().min(3, i18n.sub('x-is-required', 'name')),
	}),
	installProductSchema: z.object({
		environment: z.object({
			isExtensionEnvironment: z.boolean(),
			projectId: z.string(),
		}),
		project: z.object({
			availabilityToProduct: z.boolean(),
			environments: z.array(
				z.object({
					isExtensionEnvironment: z.boolean(),
					projectId: z.string(),
				})
			),
			rootProjectId: z.string(),
			rootProjectPlanUsage,
		}),
	}),
	invitedNewMember: z.object({
		emailAddress: z
			.string()
			.min(5, 'Please enter an email')
			.email('Invalid email address'),
		firstName: z.string().min(3, 'Please enter member name'),
		lastName: z.string().min(3, 'Last name is required'),
		roles: z.string().array().min(5, 'Please select at least one role'),
	}),
	productFeedback: z.object({
		companyName: z.string().optional(),
		emailAddress: z
			.string()
			.email('Invalid email address')
			.min(1, 'Email is required'),
		fullName: z.string().min(1, 'Full Name is required'),
		jobTitle: z.string().optional(),
		notify: z.boolean().optional(),
		ratingEaseOfUse: z.number().min(0).max(5).optional(),
		ratingSatisfaction: z.number().min(0).max(5).optional(),
		ratingUsefulness: z.number().min(0).max(5).optional(),
		suggestionFeatures: z.string().optional(),
		suggestionImprovements: z.string().optional(),
		suggestionSatisfaction: z.string().optional(),
	}),
	trialForm: z.object({
		accountId: z.string().optional(),
		consoleInviteEmailAddresses: z.array(z.string().email()),
		product: z
			.any()
			.refine((value) => !!value, {message: 'Product is required'}),
		sendNotificationEmail: z.boolean(),
	}),
};

export default commerceSchemas;
