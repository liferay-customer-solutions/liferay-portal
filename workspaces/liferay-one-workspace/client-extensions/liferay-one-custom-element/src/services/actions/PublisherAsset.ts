/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import type {Product} from '~/types/product';
import {Liferay} from '~/services/liferay/liferay';
import SearchBuilder from '~/utils/SearchBuilder';

import {UploadedFile} from '../../components/FileList/FileList';
import {DOCUMENT_FOLDER_PERMISSIONS} from '../../enums/File';
import {Properties} from '~/utils/attributeUtils';
import HeadlessDelivery from '../headless/HeadlessDelivery';
import HeadlessPublisherAssetses from '../headless/HeadlessPublisherAsset';
import HeadlessPublisherAssetAttachment from '../headless/HeadlessPublisherAssetAttachment';

const DOCUMENTS_ROOT_FOLDER = 0;
const PICK_LIST_ASSET_TYPE = 'package';
const PUBLISHER_ASSETS_FOLDER = 'publisher_assets';

export default class PublisherAsset {
	constructor(
		protected file: UploadedFile[],
		protected id: string,
		protected product: Product,
		protected properties: Properties,
		protected versions: string
	) {}

	private async createPublisherAssetsFolderId(): Promise<number> {
		const response = await HeadlessDelivery.createDocumentFolder(
			PUBLISHER_ASSETS_FOLDER,
			DOCUMENTS_ROOT_FOLDER
		);

		return response.id;
	}

	private async getAppFolderId(publisherFolderId: number): Promise<number> {
		const folderName = `app_${this.product.productId}`;

		const {items: appFolders} =
			await HeadlessDelivery.getDocumentFolderDocuments(
				publisherFolderId,
				new URLSearchParams({
					filter: SearchBuilder.contains('name', folderName),
				})
			);

		const appFolder = appFolders.find(
			(document: any) => document.name === folderName
		);

		let appFolderId = appFolder?.id;

		if (!appFolderId) {
			const packageFolder = await HeadlessDelivery.createDocumentFolder(
				folderName,
				publisherFolderId,
				DOCUMENT_FOLDER_PERMISSIONS.SITE_MEMBERS
			);

			appFolderId = packageFolder.id;
		}

		return appFolderId;
	}

	private async getPackageFolderId(appFolderId: number): Promise<number> {
		const folderName = `app_${this.product.productId}_package_${this.id}`;

		const {items: packageFolders} =
			await HeadlessDelivery.getDocumentFolderDocuments(
				appFolderId,
				new URLSearchParams({
					filter: SearchBuilder.contains('name', folderName),
				})
			);

		const packageFolder = packageFolders.find(
			(document: any) => document.name === folderName
		);

		let packageFolderId = packageFolder?.id;

		if (!packageFolderId) {
			const packageFolder = await HeadlessDelivery.createDocumentFolder(
				folderName,
				appFolderId,
				DOCUMENT_FOLDER_PERMISSIONS.SITE_MEMBERS
			);

			packageFolderId = packageFolder.id;
		}

		return packageFolderId;
	}

	private async getPublisherAssetDocumentId(
		appFolderId: number,
		file: UploadedFile
	): Promise<number> {
		const formData = new FormData();
		formData.append('file', file.file, file.fileName);

		const sourceDocument =
			await HeadlessDelivery.createDocumentFolderDocument(
				formData,
				appFolderId
			);

		return sourceDocument.id;
	}

	private async getPublisherFolderId(): Promise<number> {
		let publisherFolderId;

		const publisherAssetsFolder = await HeadlessDelivery.getDocumentFolders(
			Liferay.ThemeDisplay.getScopeGroupId(),
			new URLSearchParams({
				filter: SearchBuilder.contains('name', PUBLISHER_ASSETS_FOLDER),
			})
		);

		if (publisherAssetsFolder.items.length) {
			publisherFolderId = publisherAssetsFolder.items[0].id;
		}

		if (!publisherFolderId) {
			publisherFolderId = await this.createPublisherAssetsFolderId();
		}

		return publisherFolderId;
	}

	public async process() {
		try {
			const publisherFolderId = await this.getPublisherFolderId();

			const appFolderId = await this.getAppFolderId(publisherFolderId);

			const packageFolderId = await this.getPackageFolderId(appFolderId);

			const productRelationshipName =
				this.properties?.featurePreview?.includes(
					'product-versioning-new-primary-key'
				)
					? 'r_productEntryToPublisherAsset_CProductId'
					: 'r_productEntryToPublisherAsset_CPDefinitionId';

			const publisherAsset =
				await HeadlessPublisherAssetses.createPublisherAsset({
					[productRelationshipName]: this.product
						.id as unknown as string,
					r_accountEntryToPublisherAsset_accountEntryId:
						Liferay.CommerceContext.account?.accountId,
					version: this.versions,
				});

			for (const file of this.file) {
				const sourceCode = await this.getPublisherAssetDocumentId(
					packageFolderId,
					file
				);

				await HeadlessPublisherAssetAttachment.createPublisherAssetAttachment(
					{
						name: file.fileName,
						publisherAssetAttachmentType: PICK_LIST_ASSET_TYPE,
						r_publisherAssetToAttachment_c_publisherAssetId:
							publisherAsset.id,
						sourceCode,
					}
				);
			}
		}
		catch (error) {
			Liferay.Util.openToast({
				message:
					'Something went wrong when trying to upload a new package',
				type: 'danger',
			});

			throw error;
		}
	}
}
