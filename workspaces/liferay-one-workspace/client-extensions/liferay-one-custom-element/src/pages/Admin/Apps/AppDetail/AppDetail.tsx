/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayAlert from '@clayui/alert';
import ClayIcon from '@clayui/icon';
import ClayLabel from '@clayui/label';
import DOMPurify from 'dompurify';
import {ComponentProps} from 'react';
import {useParams} from 'react-router-dom';
import BackLink from '~/components/BackLink/BackLink';
import DetailSection from '~/components/DetailSection/DetailSection';
import {PageRenderer} from '~/components/Page/Page';
import useAdminProduct from '~/hooks/useAdminProduct';
import i18n from '~/i18n';
import {
	ProductSpecificationKey,
	ProductTypeLabels,
	ProductWorkflowDisplayType,
	ProductWorkflowStatusCode,
	ProductWorkflowStatusLabel,
	getProductCategoriesByVocabularyName,
} from '~/utils/productUtils';

import type {Word} from '~/i18n';
import type {Product} from '~/types/product';

const APP_ICON_TAG = 'app-icon';

const PRICE_MODEL_DESCRIPTIONS: Record<string, Word> = {
	free: 'price-model-free-description',
	paid: 'price-model-paid-description',
};

const PRODUCT_TYPE_DESCRIPTIONS: Record<string, Word> = {
	'client-extension': 'product-type-client-extension-description',
	'cloud': 'product-type-cloud-description',
	'composite-app': 'product-type-composite-app-description',
	'dxp': 'product-type-dxp-description',
	'low-code-configuration': 'product-type-low-code-configuration-description',
	'other': 'product-type-other-description',
};

function getSpecificationValue(product: Product, key: string) {
	return (product.productSpecifications ?? []).find(
		({specificationKey}) => specificationKey === key
	)?.value?.en_US;
}

type SupportItem = {
	symbol: string;
	title: string;
	url?: string;
	urlPrefix?: 'mailto' | 'tel';
};

function SupportContent({symbol, title, url, urlPrefix}: SupportItem) {
	return (
		<div className="align-items-center border d-flex mt-3 p-4 rounded-lg">
			<ClayIcon className="mr-3" symbol={symbol} />

			<div className="d-flex flex-column">
				<span className="font-weight-semi-bold">{title}</span>

				{url && (
					<a
						href={urlPrefix ? `${urlPrefix}:${url}` : url}
						rel="noopener noreferrer"
						target="_blank"
					>
						{url}
					</a>
				)}
			</div>
		</div>
	);
}

type TagsSectionProps = {
	labels: string[];
	title: string;
};

function TagsSection({labels, title}: TagsSectionProps) {
	return (
		<DetailSection title={title}>
			<div className="d-flex flex-wrap">
				{labels.map((label, index) => (
					<ClayLabel className="mr-2" key={index}>
						{label}
					</ClayLabel>
				))}
			</div>
		</DetailSection>
	);
}

function AppDetailContent({product}: {product: Product}) {
	const code = product.workflowStatusInfo?.code;

	const appType = getSpecificationValue(
		product,
		ProductSpecificationKey.APP_TYPE
	);

	const appVersion = getSpecificationValue(
		product,
		ProductSpecificationKey.APP_VERSION
	);

	const priceModel = getSpecificationValue(
		product,
		ProductSpecificationKey.APP_PRICING_MODEL
	)?.toLowerCase();

	const areas = getProductCategoriesByVocabularyName(
		product.categories ?? [],
		'marketplace-app-category'
	);

	const category = getProductCategoriesByVocabularyName(
		product.categories ?? [],
		'marketplace-category'
	);

	const tags = getProductCategoriesByVocabularyName(
		product.categories ?? [],
		'marketplace-app-tags'
	);

	const storefrontImages = (product.images ?? []).filter(
		(image) => !image.tags?.includes(APP_ICON_TAG)
	);

	const videoDescription = getSpecificationValue(
		product,
		ProductSpecificationKey.APP_STOREFRONT_VIDEO_DESCRIPTION
	);

	const videoURL = getSpecificationValue(
		product,
		ProductSpecificationKey.APP_STOREFRONT_VIDEO_URL
	);

	const supportItems: SupportItem[] = [
		{
			symbol: 'link',
			title: i18n.translate('support-url'),
			url: getSpecificationValue(
				product,
				ProductSpecificationKey.APP_SUPPORT_URL
			),
		},
		{
			symbol: 'globe',
			title: i18n.translate('publisher-website-url'),
			url: getSpecificationValue(
				product,
				ProductSpecificationKey.APP_SUPPORT_PUBLISHER_WEBSITE_URL
			),
		},
		{
			symbol: 'envelope-open',
			title: i18n.translate('support-email-address'),
			url: getSpecificationValue(
				product,
				ProductSpecificationKey.APP_SUPPORT_EMAIL
			),
			urlPrefix: 'mailto',
		},
		{
			symbol: 'phone',
			title: i18n.translate('support-phone-number'),
			url: getSpecificationValue(
				product,
				ProductSpecificationKey.APP_SUPPORT_PHONE
			),
			urlPrefix: 'tel',
		},
		{
			symbol: 'info-book',
			title: i18n.translate('app-usage-terms-url'),
			url: getSpecificationValue(
				product,
				ProductSpecificationKey.APP_SUPPORT_USAGE_TERMS_URL
			),
		},
		{
			symbol: 'order-form-tag',
			title: i18n.translate('app-documentation-url'),
			url: getSpecificationValue(
				product,
				ProductSpecificationKey.APP_SUPPORT_DOCUMENTATION_URL
			),
		},
		{
			symbol: 'sites',
			title: i18n.translate('app-installation-guide-url'),
			url: getSpecificationValue(
				product,
				ProductSpecificationKey.APP_SUPPORT_INSTALLATION_GUIDE_URL
			),
		},
	];

	return (
		<div className="w-100">
			<div className="mb-4">
				<BackLink path="/mp-apps">
					{i18n.translate('back-to-apps')}
				</BackLink>
			</div>

			{code === ProductWorkflowStatusCode.PENDING && (
				<ClayAlert displayType="info">
					{i18n.translate(
						'this-submission-is-currently-under-review-by-liferay-once-the-process-is-complete-it-will-be-published-on-the-marketplace-in-the-meantime-no-information-or-data-from-this-app-submission-can-be-updated'
					)}
				</ClayAlert>
			)}

			<div className="align-items-center d-flex mb-4">
				<img
					alt={product.name?.en_US}
					className="mr-3"
					src={product.thumbnail}
					style={{height: '4rem', width: '4rem'}}
				/>

				<div>
					<h2
						className="mb-1 text-truncate"
						title={product.name?.en_US}
					>
						{product.name?.en_US}
					</h2>

					<div className="align-items-center d-flex">
						{appVersion && (
							<span className="mr-3 text-secondary">
								{appVersion}
							</span>
						)}

						<ClayLabel
							displayType={
								ProductWorkflowDisplayType[
									code as keyof typeof ProductWorkflowDisplayType
								] as ComponentProps<
									typeof ClayLabel
								>['displayType']
							}
						>
							{
								ProductWorkflowStatusLabel[
									code as keyof typeof ProductWorkflowStatusLabel
								]
							}
						</ClayLabel>
					</div>
				</div>
			</div>

			<div className="border p-5 rounded-lg">
				<DetailSection title={i18n.translate('description')}>
					<div
						dangerouslySetInnerHTML={{
							__html: DOMPurify.sanitize(
								product.description?.en_US ?? ''
							),
						}}
					/>
				</DetailSection>

				{category[0] && (
					<TagsSection
						labels={[category[0]]}
						title={i18n.translate('category')}
					/>
				)}

				{!!areas.length && (
					<TagsSection
						labels={areas}
						title={i18n.translate('areas')}
					/>
				)}

				{!!tags.length && (
					<TagsSection labels={tags} title={i18n.translate('tags')} />
				)}

				{appType &&
					ProductTypeLabels[
						appType as keyof typeof ProductTypeLabels
					] && (
						<DetailSection title={i18n.translate('build')}>
							<div className="border p-4 rounded-lg">
								<span className="d-block font-weight-semi-bold">
									{
										ProductTypeLabels[
											appType as keyof typeof ProductTypeLabels
										]
									}
								</span>

								{PRODUCT_TYPE_DESCRIPTIONS[appType] && (
									<span>
										{i18n.translate(
											PRODUCT_TYPE_DESCRIPTIONS[appType]
										)}
									</span>
								)}
							</div>
						</DetailSection>
					)}

				{priceModel && (
					<DetailSection title={i18n.translate('pricing')}>
						<div className="border p-4 rounded-lg">
							<span className="d-block font-weight-semi-bold">
								{i18n.translate(priceModel as Word)}
							</span>

							{PRICE_MODEL_DESCRIPTIONS[priceModel] && (
								<span>
									{i18n.translate(
										PRICE_MODEL_DESCRIPTIONS[priceModel]
									)}
								</span>
							)}
						</div>
					</DetailSection>
				)}

				<DetailSection title={i18n.translate('storefront')}>
					{!!storefrontImages.length && (
						<>
							<span className="font-weight-semi-bold">
								{i18n.translate('images')}
							</span>

							{storefrontImages.map((image, index) => (
								<div className="d-flex mt-3" key={index}>
									<img
										draggable={false}
										src={image.src}
										style={{maxWidth: '12rem'}}
									/>

									<span className="ml-4">
										{image.title?.en_US}
									</span>
								</div>
							))}
						</>
					)}

					{videoURL && (
						<div className="mt-4">
							<span className="d-block font-weight-semi-bold">
								{i18n.translate('video')}
							</span>

							<a
								href={videoURL}
								rel="noopener noreferrer"
								target="_blank"
							>
								{videoURL}
							</a>

							{videoDescription && (
								<p className="mt-1">{videoDescription}</p>
							)}
						</div>
					)}
				</DetailSection>

				<DetailSection
					isLastSection
					title={i18n.translate('support-and-help')}
				>
					{supportItems.map((supportItem, index) => (
						<SupportContent key={index} {...supportItem} />
					))}
				</DetailSection>
			</div>
		</div>
	);
}

export default function AppDetail() {
	const {productId} = useParams();

	const {data: product, error, isLoading} = useAdminProduct(productId!);

	return (
		<PageRenderer error={error} isLoading={isLoading}>
			{product && <AppDetailContent product={product} />}
		</PageRenderer>
	);
}
