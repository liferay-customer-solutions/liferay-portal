/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import componentFolderStructure = require('./rules/componentFolderStructure');
import cssFilenamePascalCase = require('./rules/cssFilenamePascalCase');
import filenameCamelcase = require('./rules/filenameCamelcase');
import filenameMatchesDefaultExport = require('./rules/filenameMatchesDefaultExport');
import imageFilenameSnakeCase = require('./rules/imageFilenameSnakeCase');
import noAmbientTypeDeclarations = require('./rules/noAmbientTypeDeclarations');
import noComments = require('./rules/noComments');
import pageFolderStructure = require('./rules/pageFolderStructure');
import serviceClassMatchesUrl = require('./rules/serviceClassMatchesUrl');
import utilFilename = require('./rules/utilFilename');

const plugin = {
	configs: {
		recommended: {
			plugins: ['local'],
			rules: {
				'local/component-folder-structure': 'error',
				'local/css-filename-pascal-case': 'error',
				'local/filename-camelcase': 'error',
				'local/filename-matches-default-export': 'error',
				'local/image-filename-snake-case': 'error',
				'local/no-ambient-type-declarations': 'warn',
				'local/no-comments': 'warn',
				'local/page-folder-structure': 'error',
				'local/service-class-matches-url': 'error',
				'local/util-filename': 'error',
			},
		},
	},
	rules: {
		'component-folder-structure': componentFolderStructure,
		'css-filename-pascal-case': cssFilenamePascalCase,
		'filename-camelcase': filenameCamelcase,
		'filename-matches-default-export': filenameMatchesDefaultExport,
		'image-filename-snake-case': imageFilenameSnakeCase,
		'no-ambient-type-declarations': noAmbientTypeDeclarations,
		'no-comments': noComments,
		'page-folder-structure': pageFolderStructure,
		'service-class-matches-url': serviceClassMatchesUrl,
		'util-filename': utilFilename,
	},
};

export = plugin;
