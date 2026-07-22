/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import react from '@vitejs/plugin-react';
import path from 'path';
import {defineConfig} from 'vite';

export default defineConfig(({command}) => ({
	build: {
		// Keep SVG spritemaps as emitted files instead of inlined data URIs.
		// ClayIcon renders a custom spritemap with
		// <use href="${spritemap}#${symbol}">, and Chrome and Safari do not
		// resolve <use> against a data: URI, so an inlined spritemap renders
		// nothing. Serving it as a same-origin file makes <use> resolve
		// everywhere. This targets only spritemap SVGs (those with a <symbol>);
		// other SVGs, rendered via <img>, keep the default inlining.
		assetsInlineLimit: (filePath: string, content: Buffer) =>
			filePath.endsWith('.svg') && content.toString().includes('<symbol')
				? false
				: undefined,
		chunkSizeWarningLimit: 2000,
		cssCodeSplit: false,
		outDir: 'build/vite',
		rolldownOptions: {
			external: ['@liferay/oauth2-provider-web/client'],
			onwarn(warning, warn) {
				if (
					warning.code === 'MODULE_LEVEL_DIRECTIVE' &&
					warning.message.includes('use client')
				) {
					return;
				}

				warn(warning);
			},
			output: {
				assetFileNames: (assetInfo) => {
					const name = assetInfo.name ?? '';

					return name.endsWith('.css')
						? 'index.[hash][extname]'
						: 'assets/[name].[hash][extname]';
				},
				chunkFileNames: '[name].[hash].js',
				codeSplitting: false,
				entryFileNames: 'index.[hash].js',
			},
		},
	},
	experimental: {
		renderBuiltUrl(filename: string) {
			return `/o/liferay-one-custom-element/${filename}`;
		},
	},
	optimizeDeps: {
		exclude: ['@liferay/oauth2-provider-web/client'],
		rolldownOptions: {
			transform: {
				define: {
					global: 'globalThis',
				},
			},
		},
	},
	plugins: [react()],
	resolve: {
		alias:
			command === 'serve'
				? {
						'@liferay/oauth2-provider-web/client': path.resolve(
							__dirname,
							'./dev/oauth2ProviderStub.ts'
						),
						'~': path.resolve(__dirname, './src/'),
					}
				: {
						'~': path.resolve(__dirname, './src/'),
					},
	},
	server: {
		origin: 'http://localhost:5173',
	},
}));
