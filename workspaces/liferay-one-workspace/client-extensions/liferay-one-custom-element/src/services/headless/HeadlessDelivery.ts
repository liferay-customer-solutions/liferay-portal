/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import fetcher from '~/services/fetcher/fetcher';
import type {APIResponse} from '~/types/api';
import type {Document, DocumentFolder} from '~/types/publisherAsset';
import {Liferay} from '~/services/liferay/liferay';

export default class HeadlessDelivery {
	static async createDocumentFolder(
		name: string,
		parentDocumentFolderId: number,
		viewableBy: string = 'Anyone'
	) {
		const url =
			parentDocumentFolderId !== 0
				? `o/headless-delivery/v1.0/document-folders/${parentDocumentFolderId}/document-folders`
				: `o/headless-delivery/v1.0/sites/${Liferay.ThemeDisplay.getScopeGroupId()}/document-folders`;

		return fetcher.post<DocumentFolder>(url, {
			name,
			parentDocumentFolderId,
			viewableBy,
		});
	}

	static async createDocumentFolderDocument(
		body: FormData | Record<string, unknown>,
		documentFolderId: string | number
	) {
		return fetcher.post<Document>(
			`o/headless-delivery/v1.0/document-folders/${documentFolderId}/documents`,
			body
		);
	}

	static async deleteDocument(documentId: number | string) {
		return fetcher.delete(
			`o/headless-delivery/v1.0/documents/${documentId}`
		);
	}

	static async getDocument(documentId: number | string) {
		return fetcher<Document>(
			`o/headless-delivery/v1.0/documents/${documentId}`
		);
	}

	static async getDocumentFolders(
		siteId: number | string,
		searchParams: URLSearchParams = new URLSearchParams()
	) {
		return fetcher<APIResponse<DocumentFolder>>(
			`o/headless-delivery/v1.0/sites/${siteId}/document-folders?${searchParams.toString()}`
		);
	}

	static async getDocumentFolderDocument(
		folderId: number | string,
		searchParams: URLSearchParams = new URLSearchParams()
	) {
		return fetcher<APIResponse<Document>>(
			`o/headless-delivery/v1.0/document-folders/${folderId}/documents?${searchParams.toString()}`
		);
	}

	static async getDocumentFolderDocuments(
		folderId: number | string,
		searchParams: URLSearchParams = new URLSearchParams()
	) {
		return fetcher<APIResponse<DocumentFolder>>(
			`o/headless-delivery/v1.0/document-folders/${folderId}/document-folders?${searchParams.toString()}`
		);
	}
}
