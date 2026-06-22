/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import type {TSESLint, TSESTree} from '@typescript-eslint/experimental-utils';

const IMAGE_EXTENSIONS = new Set([
	'.gif',
	'.ico',
	'.jpg',
	'.jpeg',
	'.png',
	'.svg',
	'.webp',
]);

const SNAKE_CASE = /^[a-z0-9]+(_[a-z0-9]+)*$/;

type MessageId = 'mustBeSnakeCase';

const rule: TSESLint.RuleModule<MessageId, []> = {
	create(context) {
		return {
			ImportDeclaration(node: TSESTree.ImportDeclaration) {
				const source = node.source.value;

				if (typeof source !== 'string') {
					return;
				}

				const basename = source.split(/[?#]/)[0].split('/').pop() ?? '';
				const dotIndex = basename.lastIndexOf('.');

				if (dotIndex === -1) {
					return;
				}

				const ext = basename.slice(dotIndex).toLowerCase();

				if (!IMAGE_EXTENSIONS.has(ext)) {
					return;
				}

				const stem = basename.slice(0, dotIndex);

				if (!SNAKE_CASE.test(stem)) {
					context.report({
						data: {basename},
						messageId: 'mustBeSnakeCase',
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
				'Enforce snake_case filenames for image files (.gif, .ico, .jpg, .jpeg, .png, .svg, .webp).',
			recommended: false,
			url: '',
		},
		messages: {
			mustBeSnakeCase:
				'Image filename "{{basename}}" must use snake_case. Rename it to use underscores instead of hyphens or uppercase letters.',
		},
		schema: [],
		type: 'suggestion',
	},
};

export = rule;
