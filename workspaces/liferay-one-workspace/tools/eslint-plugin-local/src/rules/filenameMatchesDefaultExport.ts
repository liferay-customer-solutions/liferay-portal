/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import type {TSESLint, TSESTree} from '@typescript-eslint/experimental-utils';

const CAMEL_OR_PASCAL = /^[a-zA-Z][a-zA-Z0-9]*$/;

type MessageId =
	| 'defaultExportMustMatchFilename'
	| 'namedExportMustMatchFilename';

const rule: TSESLint.RuleModule<MessageId, []> = {
	create(context) {
		let defaultExportName: string | null = null;
		const namedValueExports = new Set<string>();

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
				const {declaration, specifiers} = node;

				if (declaration) {
					if (
						declaration.type === 'FunctionDeclaration' &&
						declaration.id &&
						CAMEL_OR_PASCAL.test(declaration.id.name)
					) {
						namedValueExports.add(declaration.id.name);
					}
					else if (
						declaration.type === 'ClassDeclaration' &&
						declaration.id &&
						CAMEL_OR_PASCAL.test(declaration.id.name)
					) {
						namedValueExports.add(declaration.id.name);
					}
					else if (declaration.type === 'VariableDeclaration') {
						for (const declarator of declaration.declarations) {
							if (
								declarator.id.type === 'Identifier' &&
								CAMEL_OR_PASCAL.test(declarator.id.name)
							) {
								namedValueExports.add(declarator.id.name);
							}
						}
					}
				}

				for (const specifier of specifiers) {
					if (
						specifier.exported.type === 'Identifier' &&
						CAMEL_OR_PASCAL.test(specifier.exported.name)
					) {
						namedValueExports.add(specifier.exported.name);
					}
				}
			},

			'Program:exit'(node: TSESTree.Program) {
				const filename = context.getFilename().replace(/\\/g, '/');
				const basename = filename.split('/').pop()!;
				const stem = basename.replace(/\.[^.]+$/, '');

				if (
					stem === 'index' ||
					stem === 'types' ||
					stem === 'utils' ||
					/constants$/i.test(stem) ||
					basename.endsWith('.d.ts')
				) {
					return;
				}

				if (defaultExportName) {
					if (defaultExportName !== stem) {
						context.report({
							data: {basename, defaultExportName},
							messageId: 'defaultExportMustMatchFilename',
							node,
						});
					}

					return;
				}

				if (
					namedValueExports.size === 1 &&
					!namedValueExports.has(stem)
				) {
					const [exportName] = namedValueExports;

					context.report({
						data: {basename, exportName},
						messageId: 'namedExportMustMatchFilename',
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
				'Enforce that the filename matches the default export name, or the sole named camelCase/PascalCase export when there is no default export.',
			recommended: false,
			url: '',
		},
		messages: {
			defaultExportMustMatchFilename:
				'Default export "{{defaultExportName}}" must match the filename. Expected "{{defaultExportName}}.tsx" (or similar), got "{{basename}}".',
			namedExportMustMatchFilename:
				'Named export "{{exportName}}" must match the filename. Expected "{{exportName}}.tsx" (or similar), got "{{basename}}".',
		},
		schema: [],
		type: 'suggestion',
	},
};

export = rule;
