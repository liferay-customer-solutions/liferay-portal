/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import type {TSESLint, TSESTree} from '@typescript-eslint/experimental-utils';

const LOCALE_PATTERN = /^[a-z]{2}_[A-Z]{2}$/;

type MessageId = 'mustBeCamelCase';

const rule: TSESLint.RuleModule<MessageId, []> = {
	create(context) {
		return {
			Program(node: TSESTree.Program) {
				const filename = context.getFilename().replace(/\\/g, '/');
				const parts = filename.split('/');
				const basename = parts[parts.length - 1];

				if (!/(?:\.d)?\.tsx?$/.test(basename)) {
					return;
				}

				const stem = basename.replace(/(?:\.d)?\.tsx?$/, '');

				if (stem === 'index') {
					return;
				}

				if (parts.includes('i18n') && LOCALE_PATTERN.test(stem)) {
					return;
				}

				if (stem.includes('-') || stem.includes('_')) {
					context.report({
						data: {basename},
						messageId: 'mustBeCamelCase',
						node,
					});
				}
			},
		};
	},
	meta: {
		docs: {
			category: 'Stylistic Issues',
			description:
				'Enforce camelCase filenames for TypeScript files (no hyphens or underscores), except for index files and i18n locale files.',
			recommended: false,
			url: '',
		},
		messages: {
			mustBeCamelCase:
				'Filename "{{basename}}" must be camelCase. Rename it to remove hyphens and underscores.',
		},
		schema: [],
		type: 'suggestion',
	},
};

export = rule;
