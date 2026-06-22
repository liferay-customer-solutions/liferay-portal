/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import type {TSESLint, TSESTree} from '@typescript-eslint/experimental-utils';

type MessageId = 'urlSegmentMustMatchClass';

function getUrlString(node: TSESTree.CallExpressionArgument): string | null {
	if (node.type === 'Literal') {
		const literal = node as TSESTree.StringLiteral;
		if (typeof literal.value === 'string') {
			return literal.value;
		}
	}

	if (node.type === 'TemplateLiteral') {
		return node.quasis[0].value.cooked ?? '';
	}

	return null;
}

function isFetcherCallee(callee: TSESTree.Expression): boolean {
	if (callee.type === 'Identifier') {
		return callee.name === 'fetcher';
	}

	if (callee.type === 'MemberExpression') {
		const {object} = callee;

		if (object.type === 'Identifier') {
			return object.name === 'fetcher';
		}
	}

	return false;
}

const rule: TSESLint.RuleModule<MessageId, []> = {
	create(context) {
		const filename = context.getFilename().replace(/\\/g, '/');

		if (!filename.includes('/services/objects/')) {
			return {};
		}

		let className: string | null = null;
		const fetcherCalls: Array<{
			node: TSESTree.CallExpressionArgument;
			url: string;
		}> = [];

		return {
			'CallExpression'(node: TSESTree.CallExpression) {
				const {callee} = node;

				if (!isFetcherCallee(callee) || !node.arguments.length) {
					return;
				}

				const url = getUrlString(node.arguments[0]);

				if (url) {
					fetcherCalls.push({node: node.arguments[0], url});
				}
			},

			'ExportDefaultDeclaration'(
				node: TSESTree.ExportDefaultDeclaration
			) {
				const {declaration} = node;

				if (
					(declaration.type === 'ClassDeclaration' ||
						declaration.type === 'ClassExpression') &&
					declaration.id
				) {
					className = declaration.id.name;
				}
			},

			'Program:exit'() {
				if (!className || !fetcherCalls.length) {
					return;
				}

				const classLower = className.toLowerCase();

				for (const {node, url} of fetcherCalls) {
					const match = url.match(/\/?o\/c\/([^/?#\s]+)/);

					if (!match) {
						continue;
					}

					const actualSegment = match[1].toLowerCase();

					if (actualSegment !== classLower) {
						context.report({
							data: {
								classLower,
								className,
								urlSegment: match[1],
							},
							messageId: 'urlSegmentMustMatchClass',
							node,
						});
					}
				}
			},
		};
	},
	meta: {
		docs: {
			category: 'Stylistic Issues',
			description:
				'Enforce that the class name (lowercased) in services/objects/ exactly matches the /o/c/ URL segment it calls.',
			recommended: false,
			url: '',
		},
		messages: {
			urlSegmentMustMatchClass:
				'URL segment "{{urlSegment}}" does not match service class "{{className}}". Expected "/o/c/{{classLower}}".',
		},
		schema: [],
		type: 'suggestion',
	},
};

export = rule;
