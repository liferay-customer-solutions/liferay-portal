/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

export interface IUpload {
	attachmentName?: string;
	errorCode?: string;
	errorMessage?: string;
	gcsSessionURL?: string;
	projectKey?: string;
	ticketAttachmentId?: string;
	ticketId?: string;
	uploadProjectKey?: string;
}
