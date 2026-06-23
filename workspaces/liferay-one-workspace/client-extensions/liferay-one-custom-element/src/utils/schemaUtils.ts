/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {z} from 'zod';
import i18n from '~/i18n';
import {removeHTMLTags} from '~/utils/stringUtils';

export const domainRegex = /^(?!:\/\/)([a-zA-Z0-9-_]+?\.)+[a-zA-Z]{2,}$/;

export const ipv4Regex =
	/^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/;

export const macAddressRegex = /^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$/;

export function checkRegExp(regex: RegExp, values: string) {
	if (!values) {
		return true;
	}

	return values.split('\n').every((value) => {
		if (!value.trim()) {
			return true;
		}

		return regex.test(value.trim());
	});
}

const baseAppSchema = {
	appUsageTermsURL: z.string().url().or(z.literal('')),
	documentationURL: z.string().url().or(z.literal('')),
	installationGuideURL: z.string().url().or(z.literal('')),
	url: z.string().url().or(z.literal('')),
};

const baseContentSchema = z.object({
	description: z.string().min(1).refine(removeHTMLTags),
	title: z.string().min(1),
});

const resources = z.object({
	free: z.number(),
	limit: z.number(),
	used: z.number(),
});

export const billingAddress = z.object({
	city: z.string().min(1),
	country: z.string().min(1),
	countryISOCode: z.string().optional(),
	name: z.string().min(1),
	phoneNumber: z.string().min(1),
	regionISOCode: z.string().optional(),
	street1: z.string().min(1),
	street2: z.string().optional(),
	zip: z.string().min(1),
});

export const blocksContentSchemas = {
	textBlock: baseContentSchema,
	textImages: baseContentSchema.extend({
		files: z.array(z.any()).min(1),
	}),
	textVideo: baseContentSchema.extend({
		videoUrl: z.string().url().min(1),
	}),
};

export const contentMediaTypeImage = z.object({
	headerImages: z.array(z.any()).min(1),
});

export const contentMediaTypeVideo = z.object({
	headerVideoDescription: z.string().optional(),
	headerVideoUrl: z.string().url().min(1),
});

export const dsrLicenseKeyBaseSchema = {
	acceptTermsAndConditions: z.boolean().refine((value) => value, {
		message: 'You must agree with the terms and conditions',
	}),
	hostname: z.string().optional().or(z.literal('')),
	ipAddress: z
		.string()
		.optional()
		.refine((value) => checkRegExp(ipv4Regex, value ?? ''), {
			message: 'Invalid IP address',
		}),
	macAddress: z
		.string()
		.optional()
		.refine((value) => checkRegExp(macAddressRegex, value ?? ''), {
			message: 'Invalid MAC address',
		}),
};

export const freeApp = z.object({
	...baseAppSchema,
	email: z.string().email().or(z.literal('')),
	phone: z.string().min(8).or(z.literal('')),
	publisherWebsiteURL: z.string().url().or(z.literal('')),
});

export const paidApp = z.object({
	...baseAppSchema,
	email: z.string().email(i18n.translate('please-fill-in-a-valid-email')),
	phone: z.string().min(8, {
		message: i18n.translate('please-fill-in-a-valid-phone-number'),
	}),
	publisherWebsiteURL: z
		.string()
		.url({message: i18n.translate('please-fill-in-a-valid-url')})
		.transform((url) => (url.startsWith('http') ? url : `https://${url}`)),
});

export const personalInformationSchema = {
	businessEmailAddress: z.string().email('Please fill in valid email'),
	companyName: z
		.string()
		.min(3, 'Company name is required')
		.optional()
		.or(z.literal('')),
	country: z.string().min(2, 'Please select the country to continue'),
	extension: z.string().optional(),
	fullName: z.string().min(3, 'Full name is required'),
	intlCode: z.object({code: z.string(), flag: z.string()}),
	jobTitle: z
		.string()
		.min(3, 'Job title is required')
		.optional()
		.or(z.literal('')),
	phoneNumber: z.string(),
};

export const rootProjectPlanUsage = z.object({
	cpu: resources,
	instance: resources,
	memory: resources,
});

export const requiredKeyName = z.object({
	key: z.string().min(1, i18n.translate('this-field-is-required')),
	name: z.string().optional(),
});

export const requiredDate = z
	.string()
	.min(1, i18n.translate('this-field-is-required'))
	.refine(
		(value) =>
			new Date(value.replace(/-/g, '/')).toString() !== 'Invalid Date',
		{message: i18n.translate('please-insert-a-valid-date')}
	);

export const requiredTimeInput = z
	.object({
		hours: z.string(),
		minutes: z.string(),
	})
	.refine((value) => value.hours !== '--' && value.minutes !== '--', {
		message: i18n.translate('this-field-is-required'),
	});
