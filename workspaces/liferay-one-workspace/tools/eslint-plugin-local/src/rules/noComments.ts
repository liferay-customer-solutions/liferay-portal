/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import type {TSESLint, TSESTree} from '@typescript-eslint/experimental-utils';

type MessageId = 'unexpectedComment';

function isCopyrightComment(comment: TSESTree.Comment): boolean {
	return (
		comment.type === 'Block' &&
		comment.value.includes('SPDX-FileCopyrightText')
	);
}

function isESLintDirective(comment: TSESTree.Comment): boolean {
	const value = comment.value.trim();

	return (
		value.startsWith('eslint-') ||
		value.startsWith('exported ') ||
		value.startsWith('global ') ||
		value.startsWith('globals ')
	);
}

function removeComment(
	fixer: TSESLint.RuleFixer,
	comment: TSESTree.Comment,
	sourceCode: Readonly<TSESLint.SourceCode>
): TSESLint.RuleFix {
	const fullSource = sourceCode.getText();
	const [start, end] = comment.range;

	const lineStart = fullSource.lastIndexOf('\n', start - 1) + 1;
	const beforeComment = fullSource.slice(lineStart, start);
	const isAloneOnLine = /^\s*$/.test(beforeComment);

	if (isAloneOnLine) {
		const trailingNewline = fullSource.slice(end).match(/^\r?\n/);
		const removeEnd =
			fullSource[end] === '\n'
				? end + 1
				: trailingNewline
					? end + trailingNewline[0].length
					: end;

		return fixer.removeRange([lineStart, removeEnd]);
	}
	else {
		const trailingSpaceEnd = fullSource[end] === ' ' ? end + 1 : end;

		return fixer.removeRange([start, trailingSpaceEnd]);
	}
}

const rule: TSESLint.RuleModule<MessageId, []> = {
	create(context) {
		const sourceCode = context.getSourceCode();

		return {
			Program() {
				const comments = sourceCode.getAllComments();

				comments.forEach((comment) => {
					if (
						isCopyrightComment(comment) ||
						isESLintDirective(comment)
					) {
						return;
					}

					context.report({
						fix(fixer) {
							return removeComment(fixer, comment, sourceCode);
						},
						loc: comment.loc,
						messageId: 'unexpectedComment',
					});
				});
			},
		};
	},
	meta: {
		docs: {
			category: 'Stylistic Issues',
			description:
				'Disallow comments (except the SPDX copyright header).',
			recommended: false,
			url: '',
		},
		fixable: 'code',
		messages: {
			unexpectedComment: 'Unexpected comment.',
		},
		schema: [],
		type: 'suggestion',
	},
};

export = rule;
