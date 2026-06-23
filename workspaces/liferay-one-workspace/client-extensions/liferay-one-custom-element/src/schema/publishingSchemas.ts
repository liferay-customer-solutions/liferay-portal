/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {z} from 'zod';
import i18n from '~/i18n';
import {
	blocksContentSchemas,
	contentMediaTypeImage,
	contentMediaTypeVideo,
	freeApp,
	paidApp,
} from '~/utils/schemaUtils';
import {removeHTMLTags} from '~/utils/stringUtils';

export const publishingSchemas = {
	appPublishing: {
		build: z.object({
			appType: z.string(),
			liferayPackages: z
				.array(
					z.object({
						file: z.array(z.any()).nonempty(),
						versions: z.array(z.string()).min(1),
					})
				)
				.min(1),
		}),
		profile: z.object({
			areas: z.array(z.any()).nonempty(),
			categories: z.object({label: z.string(), value: z.string().min(1)}),
			description: z.string().min(3),
			name: z.string().min(3),
			tags: z.array(z.any()).nonempty(),
		}),
		storefront: z.object({images: z.array(z.any()).min(1).max(10)}),
		support: {
			supportForFreeApp: freeApp,
			supportForPaidApp: paidApp,
		},
		termsAndConditions: z.boolean().refine((data) => data === true),
		version: z.object({
			notes: z.string().optional(),
			version: z.string().min(1),
		}),
	},
	becomePublisherForm: z.object({
		emailAddress: z.string().email('Please fill in valid email'),
		extension: z.string().optional(),
		firstName: z.string().min(3, 'First name is required'),
		lastName: z.string().min(3, 'Last name is required'),
		phone: z
			.object({
				code: z.string(),
				flag: z.string(),
			})
			.optional(),
		phoneNumber: z
			.string()
			.min(1, {message: i18n.translate('this-field-is-required')}),
		publisherType: z.array(z.string()).min(1),
		requestDescription: z
			.string()
			.min(3, {message: 'Request Description is required'}),
	}),
	solutionPublishing: {
		company: z
			.object({
				description: z.string().min(1),
				email: z.string().email().min(1),
				phone: z.string().min(1),
				website: z.string().min(1),
			})
			.refine((data) => !!removeHTMLTags(data.description)),
		contactUs: z.string().email().min(1),
		details: z
			.array(
				z.object({
					content: z.lazy(() =>
						z.union([
							blocksContentSchemas.textBlock,
							blocksContentSchemas.textImages,
							blocksContentSchemas.textVideo,
						])
					),
					type: z.enum([
						'text-block',
						'text-images-block',
						'text-video-block',
					]),
				})
			)
			.min(2),
		header: z
			.object({
				contentType: z.object({
					content: z.lazy(() =>
						z.union([contentMediaTypeImage, contentMediaTypeVideo])
					),
					type: z.enum(['embed-video-url', 'upload-images']),
				}),
				description: z.string().min(1),
				title: z.string().min(1),
			})
			.refine((data) => !!removeHTMLTags(data.description)),
		profile: z.object({
			categories: z.array(z.any()).nonempty(),
			description: z.string().min(3),
			name: z.string().min(3),
			tags: z.array(z.any()).nonempty(),
		}),
		termsAndConditions: z.boolean().refine((data) => data === true),
	},
};

export default publishingSchemas;
