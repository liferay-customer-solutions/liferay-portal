/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

const path = require('path');

const config = {
	env: {
		browser: true,
		es2021: true,
		node: true,
	},
	extends: ['plugin:@liferay/portal', 'plugin:local/recommended'],
	globals: {
		Liferay: true,
		configuration: true,
		fragmentElement: true,
		fragmentNamespace: true,
		layoutMode: true,
		themeDisplay: true,
	},
	ignorePatterns: ['!*', '**/build/**', 'tools/eslint-plugin-local/dist/**'],
	parserOptions: {
		ecmaFeatures: {
			jsx: true,
		},
		ecmaVersion: 2023,
	},
	plugins: ['@liferay', 'local'],
	overrides: [
		{
			files: ['client-extensions/liferay-one-custom-element/@vite/**/*.ts'],
			rules: {
				'@liferay/no-absolute-import': 'off',
			},
		},
		{
			files: ['*.json'],
			parser: 'jsonc-eslint-parser',
			plugins: ['jsonc'],
			rules: {
				'@typescript-eslint/no-explicit-any': 'off',
				'jsonc/no-dupe-keys': 'error',
				'jsonc/sort-keys': 'error',
				'notice/notice': 'off',
			},
		},
	],
	root: true,
	rules: {
		'@typescript-eslint/no-explicit-any': 'error',
		'@liferay/empty-line-between-elements': 'off',
		'@liferay/import-extensions': 'off',
		'@liferay/portal/deprecation': 'off',
		'@liferay/portal/no-document-cookie': 'off',
		'@liferay/portal/no-explicit-extend': 'off',
		'@liferay/portal/no-global-fetch': 'off',
		'@liferay/portal/no-global-storage': 'off',
		'@liferay/portal/no-loader-import-specifier': 'off',
		'@liferay/portal/no-localhost-reference': 'off',
		'@liferay/portal/no-react-dom-create-portal': 'off',
		'@liferay/portal/no-react-dom-render': 'off',
		'@liferay/portal/no-side-navigation': 'off',
		'@liferay/portal/unexecuted-ismounted': 'off',
		'no-empty': ['error', {allowEmptyCatch: true}],
		'notice/notice': [
			'error',
			{
				nonMatchingTolerance: 0.7,
				onNonMatchingHeader: 'replace',
				templateFile: path.join(__dirname, 'copyright.js'),
			},
		],
	},
};

module.exports = config;
