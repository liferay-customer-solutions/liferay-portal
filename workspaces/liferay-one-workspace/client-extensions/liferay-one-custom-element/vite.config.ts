/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import react from '@vitejs/plugin-react';
import path from 'path';
import {defineConfig} from 'vite';

export default defineConfig(({command}) => ({
	build: {
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
