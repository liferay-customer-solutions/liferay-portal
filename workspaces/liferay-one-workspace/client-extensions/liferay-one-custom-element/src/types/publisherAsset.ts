/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

export type Document = {
	contentUrl?: string;
	fileName?: string;
	id: number;
	link?: {href: string};
	sizeInBytes: number;
	title?: string;
};

export type DocumentFolder = {
	id: number;
	name: string;
};

export type PublisherAssetAttachment = {
	id?: number;
	publisherAssetAttachmentType?: string;
	sourceCode: {
		id: number;
		link: {href: string};
		name: string;
	};
};

export type PublisherAsset = {
	id: number;
	name?: string;
	publisherAssetToAttachment: PublisherAssetAttachment[];
	version: string;
};
