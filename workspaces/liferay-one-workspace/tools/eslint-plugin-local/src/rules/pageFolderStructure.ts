/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import type {TSESLint, TSESTree} from '@typescript-eslint/experimental-utils';

type MessageId = 'mustMatchFolderName';

const rule: TSESLint.RuleModule<MessageId, []> = {
	create(context) {
		return {
			Program(node: TSESTree.Program) {
				const filename = context.getFilename().replace(/\\/g, '/');

				if (!filename.endsWith('.tsx')) {
					return;
				}

				const match = filename.match(/src\/pages\/([^/]+)\/([^/]+)$/);

				if (!match) {
					return;
				}

				const folderName = match[1];
				const fileName = match[2];
				const stem = fileName.slice(0, -4);

				if (
					stem.toLowerCase() === folderName.toLowerCase() &&
					stem !== folderName
				) {
					context.report({
						data: {fileName, folderName},
						messageId: 'mustMatchFolderName',
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
				'Enforce that the main TSX file in a page folder matches the folder name exactly (PascalCase).',
			recommended: false,
			url: '',
		},
		messages: {
			mustMatchFolderName:
				'"{{fileName}}" must match its folder name exactly. Expected "{{folderName}}.tsx".',
		},
		schema: [],
		type: 'suggestion',
	},
};

export = rule;
