/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {z} from 'zod';
import i18n from '~/i18n';
import {EMAIL_PATTERN} from '~/utils/formValidationUtils';

export const MAX_INVITATIONS_COUNT = 10;

const inviteSchema = z.object({
	emailAddress: z
		.string()
		.trim()
		.regex(
			EMAIL_PATTERN,
			i18n.translate('please-enter-a-valid-email-address')
		),
	familyName: z
		.string()
		.trim()
		.min(1, i18n.translate('please-enter-a-valid-last-name')),
	givenName: z
		.string()
		.trim()
		.min(1, i18n.translate('please-enter-a-valid-first-name')),
	roleNames: z.array(z.string()),
});

export const accountSchemas = {
	inviteMembers: z.object({
		invites: z
			.array(inviteSchema)
			.min(1)
			.max(MAX_INVITATIONS_COUNT)
			.superRefine((invites, context) => {
				const emailAddresses = new Set<string>();

				invites.forEach((invite, index) => {
					const emailAddress = invite.emailAddress.toLowerCase();

					if (emailAddresses.has(emailAddress)) {
						context.addIssue({
							code: z.ZodIssueCode.custom,
							message: i18n.translate(
								'this-email-address-is-duplicated'
							),
							path: [index, 'emailAddress'],
						});
					}

					emailAddresses.add(emailAddress);
				});
			}),
	}),
};

export type InviteMembersForm = z.infer<typeof accountSchemas.inviteMembers>;

export default accountSchemas;
