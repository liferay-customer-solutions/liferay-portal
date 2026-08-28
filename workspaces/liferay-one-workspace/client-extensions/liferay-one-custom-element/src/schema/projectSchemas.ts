/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {z} from 'zod';
import i18n from '~/i18n';
import {EMAIL_PATTERN} from '~/utils/formValidationUtils';

const FRIENDLY_URL_PATTERN = /^\/[^. "]+[0-9a-z]+[^A-Z]$/;

const PROJECT_ID_PATTERN = /^[0-9a-z]+$/;

const emailAddress = z
	.string()
	.trim()
	.regex(EMAIL_PATTERN, i18n.translate('please-enter-a-valid-email-address'));

const projectId = z
	.string()
	.trim()
	.min(1, i18n.translate('this-field-is-required'))
	.regex(
		PROJECT_ID_PATTERN,
		i18n.translate('lowercase-letters-and-numbers-only')
	);

const region = z.string().min(1, i18n.translate('this-field-is-required'));

export const requiredSelectSchema = region;

const requiredText = z
	.string()
	.trim()
	.min(1, i18n.translate('this-field-is-required'));

const paasAdmin = z.object({
	emailAddress,
	firstName: requiredText,
	githubUsername: requiredText,
	lastName: requiredText,
});

const saasAdmin = z.object({
	emailAddress,
	name: requiredText,
});

export const projectSchemas = {
	cloudActivationAnalyticsCloud: z.object({
		allowedEmailDomains: z.string().optional(),
		disasterRecoveryRegion: z.string().optional(),
		friendlyURL: z
			.string()
			.optional()
			.refine(
				(value) => !value || value.startsWith('/'),
				i18n.translate('the-workspace-url-should-start-with-/')
			)
			.refine(
				(value) => !value || !value.includes(' '),
				i18n.translate('the-workspace-url-must-not-have-spaces')
			)
			.refine(
				(value) => !value || FRIENDLY_URL_PATTERN.test(value),
				i18n.translate('lowercase-letters-numbers-and-dashes-only')
			),
		ownerEmailAddress: emailAddress,
		region,
		timeZone: z.string().optional(),
		workspaceName: z
			.string()
			.trim()
			.min(1, i18n.translate('this-field-is-required'))
			.max(255, i18n.sub('this-field-exceeded-x-characters', ['255'])),
	}),
	cloudActivationPaaS: z.object({
		admins: z
			.array(paasAdmin)
			.min(1, i18n.translate('this-field-is-required')),
		disasterRecoveryRegion: z.string().optional(),
		dxpVersion: requiredSelectSchema,
		projectId,
		region,
	}),
	cloudActivationSaaS: z.object({
		admins: z
			.array(saasAdmin)
			.min(1, i18n.translate('this-field-is-required')),
		analyticsCloudOwnerEmailAddress: emailAddress,
		projectId,
		region,
	}),
};

export default projectSchemas;
