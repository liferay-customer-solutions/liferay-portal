/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {LiferayPackage} from '~/context/NewAppContextProvider';
import {base64ToText, fileToBase64} from '~/utils/fileUtils';

import {UploadedFile} from '../../components/FileList/FileList';
import {ProductSpecificationKey} from '../../enums/Product';
import HeadlessCommerceAdminCatalogImpl from '../headless/HeadlessCommerceAdminCatalog';
import HeadlessDelivery from '../headless/HeadlessDelivery';
import HeadlessPublisherAsset from '../headless/HeadlessPublisherAsset';

import type {Product} from '~/types/product';

export default class BaseAppPublish {
	public static addOrUpdateImages = async (
		images: UploadedFile[],
		tag: string | null,
		product: Product,
		priorityInitialValue: number
	) => {
		let priority = priorityInitialValue;

		for (const image of images) {
			priority++;

			if (!image.changed && image.uploaded) {
				continue;
			}

			const uploadedProductImage = product?.images?.find(
				(uploadedImage) =>
					uploadedImage.externalReferenceCode === image.id
			);

			const imageMetadata = {
				...(uploadedProductImage && {
					fileEntryId: uploadedProductImage.fileEntryId,
					id: uploadedProductImage.id,
				}),
				...(image?.file && {
					attachment: base64ToText(
						(await fileToBase64(image.file)) as string
					),
				}),
				externalReferenceCode: image.id,
				galleryEnabled: true,
				neverExpire: true,
				priority,
				tags: tag ? [tag] : [],
				title: {
					en_US: image.imageDescription || image.file.name,
				},
			};

			await HeadlessCommerceAdminCatalogImpl.addOrUpdateProductImageByExternalReferenceCode(
				product?.externalReferenceCode,
				imageMetadata
			);

			image.changed = false;
			image.progress = 100;
			image.uploaded = true;
		}
	};

	public static async deleteLiferayPackages(
		liferayPackages: LiferayPackage[]
	) {
		for (const liferayPackage of liferayPackages) {
			try {
				for (const file of liferayPackage.file ?? []) {
					await HeadlessDelivery.deleteDocument(file.id);
				}

				await HeadlessPublisherAsset.deletePublisherAsset(
					liferayPackage.id
				);
			}
			catch (error) {
				console.error(
					`Unable to delete Liferay package ${liferayPackage.id}`,
					error
				);
			}
		}
	}

	public static async deleteReferences(externalReferenceCodes: string[]) {
		for (const externalReferenceCode of externalReferenceCodes) {
			try {
				await HeadlessCommerceAdminCatalogImpl.deleteAttachmentByExternalReferenceCode(
					externalReferenceCode
				);
			}
			catch (error) {
				console.error(
					`Unable to delete attachment ${externalReferenceCode}`,
					error
				);
			}
		}
	}

	public static updateSpecification = async (
		product: Product,
		specificationKey: ProductSpecificationKey,
		value: string,
		options: {exactMatch?: boolean} = {exactMatch: false}
	) => {
		const {productId, productSpecifications = []} = product;

		const specification = productSpecifications.find(
			(productSpecification) =>
				productSpecification.specificationKey === specificationKey &&
				(options?.exactMatch
					? productSpecification.value.en_US === value
					: true)
		);

		if (
			!value?.trim() ||
			(specification && specification.value.en_US === value)
		) {
			return;
		}

		const fn = specification
			? HeadlessCommerceAdminCatalogImpl.updateProductSpecification
			: HeadlessCommerceAdminCatalogImpl.createProductSpecification;

		const result = await fn(
			(specification ? specification.id : productId) as number,
			{
				specificationKey,
				value: {en_US: value},
			}
		);

		if (specification) {
			specification.value.en_US = value;

			return;
		}

		productSpecifications.push(result);
	};

	public static updateSpecifications = (
		product: Product,
		specifications: {key: ProductSpecificationKey; value: string}[],
		options: {exactMatch?: boolean} = {exactMatch: false}
	) =>
		Promise.allSettled(
			specifications.map((specification) =>
				this.updateSpecification(
					product,
					specification.key,
					specification.value,
					options
				)
			)
		);
}
