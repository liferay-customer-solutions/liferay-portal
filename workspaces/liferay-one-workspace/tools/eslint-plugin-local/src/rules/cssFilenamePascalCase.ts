/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import type {TSESLint, TSESTree} from '@typescript-eslint/experimental-utils';

function toPascalCase(str: string): string {
	return str
		.split(/[_-]/)
		.map((word) => word.charAt(0).toUpperCase() + word.slice(1))
		.join('');
}

type MessageId = 'mustBePascalCase';

const rule: TSESLint.RuleModule<MessageId, []> = {
	create(context) {
		return {
			ImportDeclaration(node: TSESTree.ImportDeclaration) {
				const source = node.source.value as string;

				if (!source.endsWith('.css')) {
					return;
				}

				const importParts = source.split('/');
				const fileName = importParts[importParts.length - 1];
				const stem = fileName.slice(0, -4);

				if (
					/^[A-Z]/.test(stem) &&
					/[a-z]/.test(stem) &&
					!/[_-]/.test(stem)
				) {
					return;
				}

				const suggested =
					stem === 'index'
						? 'the component name (e.g. "ComponentName.css")'
						: `"${toPascalCase(stem)}.css"`;

				context.report({
					data: {fileName, suggested},
					messageId: 'mustBePascalCase',
					node,
				});
			},
		};
	},
	meta: {
		docs: {
			category: 'Stylistic Issues',
			description: 'Enforce PascalCase filenames for imported CSS files.',
			recommended: false,
			url: '',
		},
		messages: {
			mustBePascalCase:
				'CSS file "{{fileName}}" must be PascalCase. Rename it to {{suggested}}.',
		},
		schema: [],
		type: 'suggestion',
	},
};

export = rule;
