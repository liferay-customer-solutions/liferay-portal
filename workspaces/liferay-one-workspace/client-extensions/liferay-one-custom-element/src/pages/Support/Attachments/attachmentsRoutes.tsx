/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {lazy} from 'react';

import {AppRoute} from '../../../utils/routes';
import Layout from './components/Layout';

const AttachmentDownloaderOutlet = lazy(
	() => import('./pages/AttachmentDownloader/AttachmentDownloaderOutlet')
);
const AttachmentUploaderOutlet = lazy(
	() => import('./pages/AttachmentUploader/AttachmentUploaderOutlet')
);

export const attachmentsRoutes: AppRoute[] = [
	{
		children: [
			{element: <AttachmentUploaderOutlet />, path: ':ticketId'},
			{
				element: <AttachmentDownloaderOutlet />,
				path: 'erc/:ticketAttachmentERC',
			},
			{
				element: <AttachmentDownloaderOutlet />,
				path: 'id/:ticketAttachmentId',
			},
		],
		element: <Layout />,
	},
];
