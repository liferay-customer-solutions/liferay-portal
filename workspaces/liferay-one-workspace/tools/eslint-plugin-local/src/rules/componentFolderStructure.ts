/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import type {TSESLint, TSESTree} from '@typescript-eslint/experimental-utils';

const ALLOWED_SUBFOLDERS = ['context', 'hooks'];

type MessageId =
	| 'contextNamingConvention'
	| 'hookNamingConvention'
	| 'indexNotAllowed'
	| 'mustBeInContextFolder'
	| 'mustBeInHooksFolder'
	| 'mustBeInOwnFolder'
	| 'mustMatchFolderName'
	| 'nestedTooDeep'
	| 'notAllowedSubfolder';

const rule: TSESLint.RuleModule<MessageId, []> = {
	create(context) {
		return {
			Program(node: TSESTree.Program) {
				const filename = context.getFilename().replace(/\\/g, '/');
				const match = filename.match(/src\/components\/(.+)$/);

				if (!match) {
					return;
				}

				const parts = match[1].split('/');
				const fileName = parts[parts.length - 1];
				const baseName = fileName.replace(/\.[^.]+$/, '');

				if (parts.length === 1) {
					context.report({
						data: {baseName, fileName},
						messageId: 'mustBeInOwnFolder',
						node,
					});

					return;
				}

				const folderName = parts[0];

				if (parts.length === 3) {
					const subFolder = parts[1];

					if (!ALLOWED_SUBFOLDERS.includes(subFolder)) {
						context.report({
							data: {subFolder},
							messageId: 'notAllowedSubfolder',
							node,
						});

						return;
					}

					if (subFolder === 'hooks' && !baseName.startsWith('use')) {
						context.report({
							data: {fileName},
							messageId: 'hookNamingConvention',
							node,
						});
					}

					if (
						subFolder === 'context' &&
						!baseName.includes('Context') &&
						!baseName.includes('Provider')
					) {
						context.report({
							data: {fileName},
							messageId: 'contextNamingConvention',
							node,
						});
					}

					return;
				}

				if (parts.length > 3) {
					context.report({
						data: {fileName},
						messageId: 'nestedTooDeep',
						node,
					});

					return;
				}

				if (baseName === 'index') {
					const ext = fileName.slice(fileName.indexOf('.'));

					context.report({
						data: {ext, fileName, folderName},
						messageId: 'indexNotAllowed',
						node,
					});

					return;
				}

				if (baseName.startsWith('use')) {
					context.report({
						data: {fileName, folderName},
						messageId: 'mustBeInHooksFolder',
						node,
					});

					return;
				}

				if (
					baseName.includes('Context') ||
					baseName.includes('Provider')
				) {
					context.report({
						data: {fileName, folderName},
						messageId: 'mustBeInContextFolder',
						node,
					});

					return;
				}

				if (fileName.endsWith('.tsx') && baseName !== folderName) {
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
				'Enforce component folder structure: one folder per component, hooks in hooks/ subfolder, context providers in context/ subfolder, no other sub-folders.',
			recommended: false,
			url: '',
		},
		messages: {
			contextNamingConvention:
				'"{{fileName}}" is in context/ but doesn\'t follow the context naming convention. Context files must include "Context" or "Provider" in their name.',
			hookNamingConvention:
				'"{{fileName}}" is in hooks/ but doesn\'t follow the hook naming convention. Hook files must start with "use".',
			indexNotAllowed:
				'"{{fileName}}" is not allowed. Name the file after its folder: "{{folderName}}{{ext}}".',
			mustBeInContextFolder:
				'"{{fileName}}" must be in a context/ subfolder: src/components/{{folderName}}/context/{{fileName}}',
			mustBeInHooksFolder:
				'"{{fileName}}" must be in a hooks/ subfolder: src/components/{{folderName}}/hooks/{{fileName}}',
			mustBeInOwnFolder:
				'"{{fileName}}" must be in its own folder: src/components/{{baseName}}/{{fileName}}',
			mustMatchFolderName:
				'"{{fileName}}" must match its folder name. Expected "{{folderName}}.tsx".',
			nestedTooDeep:
				'"{{fileName}}" is nested too deep. Component sub-folders (context/, hooks/) must not contain further sub-folders.',
			notAllowedSubfolder:
				'"{{subFolder}}/" is not an allowed subfolder. Only "context/" and "hooks/" are permitted inside component folders.',
		},
		schema: [],
		type: 'suggestion',
	},
};

export = rule;
