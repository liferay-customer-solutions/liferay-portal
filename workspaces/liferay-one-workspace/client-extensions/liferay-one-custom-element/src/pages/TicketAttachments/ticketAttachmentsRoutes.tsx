/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {lazy} from 'react';
import {Navigate} from 'react-router-dom';
import {AppRoute} from '~/utils/routeUtils';

import TicketAttachmentsLayout from './components/TicketAttachmentsLayout/TicketAttachmentsLayout';

const TicketAttachmentsAdd = lazy(
	() => import('./TicketAttachmentsAdd/TicketAttachmentsAdd')
);
const TicketAttachmentsDownloaderOutlet = lazy(
	() =>
		import(
			'./TicketAttachmentsDownloader/TicketAttachmentsDownloaderOutlet'
		)
);
const TicketAttachmentsList = lazy(
	() => import('./TicketAttachmentsList/TicketAttachmentsList')
);
const TicketAttachmentsUploaderOutlet = lazy(
	() => import('./TicketAttachmentsUploader/TicketAttachmentsUploaderOutlet')
);

export const ticketAttachmentsRoutes: AppRoute[] = [
	{
		children: [
			{element: <TicketAttachmentsList />, index: true},
			{element: <TicketAttachmentsAdd />, path: 'new'},
			{
				element: <TicketAttachmentsUploaderOutlet />,
				path: 'new/:ticketId',
			},
			{
				element: <TicketAttachmentsDownloaderOutlet />,
				path: 'erc/:ticketAttachmentERC',
			},
			{
				element: <TicketAttachmentsDownloaderOutlet />,
				path: 'id/:ticketAttachmentId',
			},
			{element: <TicketAttachmentsUploaderOutlet />, path: ':ticketId'},
			{element: <Navigate replace to="." />, path: '*'},
		],
		element: <TicketAttachmentsLayout />,
	},
];
