/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import react from '@vitejs/plugin-react';
import {defineConfig} from 'vite';

export default defineConfig(({command}) => ({
	base: command === 'build' ? '/o/liferay-one-custom-element/' : '/',
	build: {
		assetsDir: '.',
		outDir: 'build/static',
		rollupOptions: {
			output: {
				assetFileNames: '[name].[hash][extname]',
				chunkFileNames: '[name].[hash].js',
				entryFileNames: '[name].[hash].js',
				manualChunks: {
					vendor: ['react', 'react-dom', 'react-router-dom'],
				},
			},
		},
	},
	plugins: [react()],
	server: {
		origin: 'http://localhost:5173',
	},
}));
