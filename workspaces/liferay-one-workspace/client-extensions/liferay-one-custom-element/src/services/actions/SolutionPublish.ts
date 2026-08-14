/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {SolutionInitialState} from '~/context/SolutionContextProvider';
import {
	ProductSpecificationKey,
	ProductTags,
	ProductTypeVocabulary,
	ProductVocabulary,
	ProductWorkflowStatusCode,
} from '~/enums/Product';
import {Liferay} from '~/services/liferay/liferay';
import {base64ToText, fileToBase64} from '~/utils/fileUtils';

import HeadlessCommerceAdminCatalogImpl from '../headless/HeadlessCommerceAdminCatalog';
import BaseAppPublish from './BaseAppPublish';

import type {Product as ProductType, ProductCategories} from '~/types/product';

export type SolutionConfig = {
	editorName: string;
	isDraft: boolean;
};

const LAST_UPDATED_BY_LABEL = 'Last Updated By';

function normalizeCategory(category: {
	name: string;
	value: number | string;
}): Partial<ProductCategories> {
	return {
		id: Number(category.value),
		name: category.name,
	};
}

export default class SolutionPublish extends BaseAppPublish {
	private config: SolutionConfig = {
		editorName: '',
		isDraft: false,
	};

	constructor(private context: SolutionInitialState) {
		super();
	}

	public async sync(config: SolutionConfig) {
		this.config = config;

		const product = await this.syncProfile();

		this.context._product = product;

		await this.deleteRemovedImages();

		const failedSteps: string[] = [];

		for (const sync of [
			this.syncHeader.bind(this),
			this.syncCompanyProfileAndContactUs.bind(this),
			this.syncDetails.bind(this),
			this.syncLastUpdatedBy.bind(this),
		]) {
			try {
				await sync(product);
			}
			catch (error) {
				failedSteps.push(sync.name);

				console.error(`Unable to sync ${sync.name}`, error);
			}
		}

		if (failedSteps.length) {
			throw new Error(
				`Unable to publish the solution because the following steps did not complete ${failedSteps.join(
					', '
				)}`
			);
		}

		return product;
	}

	private async deleteRemovedImages() {
		await BaseAppPublish.deleteReferences(
			this.context.references.imagesToDelete
		);
	}

	private getProductStatus() {
		const productStatus = this.config.isDraft
			? ProductWorkflowStatusCode.DRAFT
			: ProductWorkflowStatusCode.PENDING;

		return {
			productStatus,
			workflowStatusInfo: productStatus,
		};
	}

	private async syncCompanyProfileAndContactUs(product: ProductType) {
		const {
			company: {description, email, phone, website},
			contactUs,
		} = this.context;

		await BaseAppPublish.updateSpecifications(product, [
			{
				key: ProductSpecificationKey.SOLUTION_COMPANY_DESCRIPTION,
				value: description,
			},
			{
				key: ProductSpecificationKey.SOLUTION_COMPANY_EMAIL,
				value: email,
			},
			{
				key: ProductSpecificationKey.SOLUTION_COMPANY_PHONE,
				value: phone,
			},
			{
				key: ProductSpecificationKey.SOLUTION_COMPANY_WEBSITE,
				value: website,
			},
			{
				key: ProductSpecificationKey.SOLUTION_CONTACT_EMAIL,
				value: contactUs,
			},
		]);
	}

	private async syncDetails(product: ProductType) {
		const {details, header} = this.context;

		const headerImageCount =
			header.contentType.type === 'upload-images'
				? header.contentType.content.headerImages.length
				: 0;

		for (const block of details) {
			if (block.type !== 'text-images-block') {
				continue;
			}

			await BaseAppPublish.addOrUpdateImages(
				block.content.files ?? [],
				ProductTags.SOLUTION_DETAILS,
				product,
				headerImageCount
			);
		}

		const blocks = details.map((block) => {
			if (block.type === 'text-images-block') {
				return {
					...block,
					content: {
						...block.content,
						files: (block.content.files ?? []).map(({id}) => id),
					},
				};
			}

			return block;
		});

		await BaseAppPublish.updateSpecification(
			product,
			ProductSpecificationKey.SOLUTION_DETAILS_BLOCKS,
			JSON.stringify(blocks)
		);
	}

	private async syncHeader(product: ProductType) {
		const {
			header: {contentType, description, title},
		} = this.context;

		await BaseAppPublish.updateSpecifications(product, [
			{
				key: ProductSpecificationKey.SOLUTION_HEADER_DESCRIPTION,
				value: description,
			},
			{
				key: ProductSpecificationKey.SOLUTION_HEADER_TITLE,
				value: title,
			},
		]);

		if (contentType.type === 'embed-video-url') {
			await BaseAppPublish.updateSpecifications(product, [
				{
					key: ProductSpecificationKey.SOLUTION_HEADER_VIDEO_DESCRIPTION,
					value: contentType.content.headerVideoDescription ?? '',
				},
				{
					key: ProductSpecificationKey.SOLUTION_HEADER_VIDEO_URL,
					value: contentType.content.headerVideoUrl,
				},
			]);

			return;
		}

		await BaseAppPublish.addOrUpdateImages(
			contentType.content.headerImages,
			ProductTags.SOLUTION_HEADER,
			product,
			0
		);
	}

	private async syncLastUpdatedBy(product: ProductType) {
		await BaseAppPublish.updateSpecification(
			product,
			ProductSpecificationKey.LAST_UPDATED_BY,
			JSON.stringify({
				name: this.config.editorName,
				userId: Liferay.ThemeDisplay.getUserId(),
			}),
			{label: LAST_UPDATED_BY_LABEL, visible: false}
		);
	}

	private async syncProfile() {
		const {
			_product,
			catalogId,
			profile: {categories, description, file, name, tags},
			references: {vocabulariesAndCategories},
		} = this.context;

		const productTypeCategories = (
			vocabulariesAndCategories[ProductVocabulary.PRODUCT_TYPE]
				?.categories ?? []
		).filter(
			({name}: {name: string}) => name === ProductTypeVocabulary.SOLUTION
		);

		const productCategories = [
			...categories,
			...productTypeCategories,
			...tags,
		]
			.filter((category) => category?.value)
			.map(normalizeCategory);

		if (_product) {
			if (file?.file && (!file.uploaded || file.changed)) {
				await this.uploadProfileIcon(
					_product,
					file.fileName,
					file.file
				);
			}

			await HeadlessCommerceAdminCatalogImpl.updateProduct(
				_product.productId as number,
				{
					categories: productCategories,
					description: {en_US: description},
					name: {en_US: name},
					...this.getProductStatus(),
				}
			);

			return _product;
		}

		const product =
			await HeadlessCommerceAdminCatalogImpl.createVirtualProduct({
				catalogId,
				categories: productCategories,
				description,
				name,
				...this.getProductStatus(),
			});

		product.productSpecifications = [];

		if (file?.file) {
			await this.uploadProfileIcon(product, file.fileName, file.file);
		}

		return product;
	}

	private async uploadProfileIcon(
		product: ProductType,
		fileName: string,
		file: File
	) {
		await HeadlessCommerceAdminCatalogImpl.addOrUpdateProductImageByExternalReferenceCode(
			product.externalReferenceCode,
			{
				attachment: base64ToText((await fileToBase64(file)) as string),
				galleryEnabled: false,
				neverExpire: true,
				priority: 0,
				tags: [ProductTags.SOLUTION_PROFILE_APP_ICON],
				title: {
					en_US: fileName,
				},
			}
		);
	}
}
