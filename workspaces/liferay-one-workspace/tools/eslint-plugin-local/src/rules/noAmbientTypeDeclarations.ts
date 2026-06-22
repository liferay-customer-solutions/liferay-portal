/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import type {TSESLint, TSESTree} from '@typescript-eslint/experimental-utils';

type MessageId = 'noAmbientInterface' | 'noAmbientTypeAlias';

type NodeWithParent = {
	parent?: NodeWithParent;
	type: string;
};

function isAmbient(node: NodeWithParent): boolean {
	let current: NodeWithParent | undefined = node.parent;

	while (current) {
		if (
			current.type === 'TSModuleDeclaration' ||
			current.type === 'TSGlobalAugmentation'
		) {
			return true;
		}

		current = current.parent;
	}

	return false;
}

const rule: TSESLint.RuleModule<MessageId, []> = {
	create(context) {
		const filename = context.getFilename().replace(/\\/g, '/');

		if (!filename.endsWith('.d.ts')) {
			return {};
		}

		return {
			TSInterfaceDeclaration(node: TSESTree.TSInterfaceDeclaration) {
				if (!isAmbient(node as unknown as NodeWithParent)) {
					context.report({
						messageId: 'noAmbientInterface',
						node,
					});
				}
			},

			TSTypeAliasDeclaration(node: TSESTree.TSTypeAliasDeclaration) {
				if (!isAmbient(node as unknown as NodeWithParent)) {
					context.report({
						messageId: 'noAmbientTypeAlias',
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
				'Disallow ambient type/interface declarations in .d.ts files. Reserve .d.ts files for module augmentation (declare module) and global declarations (declare global).',
			recommended: false,
			url: '',
		},
		messages: {
			noAmbientInterface:
				'Use a plain .ts file with "export interface" instead of an ambient interface declaration in a .d.ts file.',
			noAmbientTypeAlias:
				'Use a plain .ts file with "export type" instead of an ambient type declaration in a .d.ts file.',
		},
		schema: [],
		type: 'suggestion',
	},
};

export = rule;
