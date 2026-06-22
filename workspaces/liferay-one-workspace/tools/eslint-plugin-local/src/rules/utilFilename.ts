/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import type {TSESLint, TSESTree} from '@typescript-eslint/experimental-utils';

const TYPE_ONLY_DECLARATIONS = new Set([
	'TSInterfaceDeclaration',
	'TSTypeAliasDeclaration',
]);

type MessageId =
	| 'defaultExportMustMatchFilename'
	| 'multipleExportsNeedSuffix'
	| 'singleExportNeedsDefault';

const rule: TSESLint.RuleModule<MessageId, []> = {
	create(context) {
		let defaultExportName: string | null = null;
		let namedValueExportCount = 0;

		return {
			'ExportDefaultDeclaration'(
				node: TSESTree.ExportDefaultDeclaration
			) {
				const {declaration} = node;

				if (declaration.type === 'Identifier') {
					defaultExportName = declaration.name;
				}
				else if (
					(declaration.type === 'ClassDeclaration' ||
						declaration.type === 'FunctionDeclaration') &&
					declaration.id
				) {
					defaultExportName = declaration.id.name;
				}
			},

			'ExportNamedDeclaration'(node: TSESTree.ExportNamedDeclaration) {
				if (node.exportKind === 'type') {
					return;
				}

				if (
					node.declaration &&
					TYPE_ONLY_DECLARATIONS.has(node.declaration.type)
				) {
					return;
				}

				namedValueExportCount++;
			},

			'Program:exit'(programNode: TSESTree.Program) {
				const filename = context.getFilename().replace(/\\/g, '/');
				const parts = filename.split('/');
				const basename = parts.pop()!;

				if (!parts.some((part) => part === 'utils')) {
					return;
				}

				if (namedValueExportCount === 0) {
					return;
				}

				const stem = basename.replace(/\.[^.]+$/, '');

				if (stem === 'index' || basename.endsWith('.d.ts')) {
					return;
				}

				const stemLower = stem.toLowerCase();

				if (namedValueExportCount > 1) {
					if (
						!stemLower.endsWith('utils') &&
						!stemLower.endsWith('constants')
					) {
						context.report({
							data: {basename, stem},
							messageId: 'multipleExportsNeedSuffix',
							node: programNode,
						});
					}
				}
				else if (!defaultExportName) {
					context.report({
						data: {basename, stem},
						messageId: 'singleExportNeedsDefault',
						node: programNode,
					});
				}
				else if (defaultExportName !== stem) {
					context.report({
						data: {basename, defaultExportName},
						messageId: 'defaultExportMustMatchFilename',
						node: programNode,
					});
				}
			},
		};
	},
	meta: {
		docs: {
			category: 'Stylistic Issues',
			description:
				'Enforce that utils files either have a single default export matching the filename, or end with "Utils"/"Constants" when they contain multiple exports.',
			recommended: false,
			url: '',
		},
		messages: {
			defaultExportMustMatchFilename:
				'Default export "{{defaultExportName}}" must match the filename. Expected "{{defaultExportName}}.ts" (or similar), got "{{basename}}".',
			multipleExportsNeedSuffix:
				'Files with multiple exports in a utils directory must end with "Utils" or "Constants". Rename "{{basename}}" to "{{stem}}Utils.ts" (or similar).',
			singleExportNeedsDefault:
				'Single-export utils file "{{basename}}" must have a default export. Add "export default {{stem}};" at the bottom.',
		},
		schema: [],
		type: 'suggestion',
	},
};

export = rule;
