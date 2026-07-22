/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayAlert from '@clayui/alert';
import ClayButton from '@clayui/button';
import ClayIcon from '@clayui/icon';
import ClayLabel from '@clayui/label';
import DOMPurify from 'dompurify';
import {ComponentProps, ReactNode} from 'react';
import {useNavigate, useParams} from 'react-router-dom';
import {PageRenderer} from '~/components/Page/Page';
import useAdminApp from '~/hooks/useAdminApp';
import i18n from '~/i18n';
import {
	ProductSpecificationKey,
	ProductWorkflowDisplayType,
	ProductWorkflowStatusCode,
	ProductWorkflowStatusLabel,
	getProductCategoriesByVocabularyName,
} from '~/utils/productUtils';

import type {Product} from '~/types/product';

const APP_ICON_TAG = 'app-icon';

function getSpecificationValue(product: Product, key: string) {
	return product.productSpecifications.find(
		({specificationKey}) => specificationKey === key
	)?.value?.en_US;
}

type SectionProps = {
	children: ReactNode;
	isLastSection?: boolean;
	title: string;
};

function Section({children, isLastSection = false, title}: SectionProps) {
	return (
		<>
			<div className="mb-4">
				<h3 className="mb-3">{title}</h3>

				{children}
			</div>

			{!isLastSection && <hr />}
		</>
	);
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
					<a href={urlPrefix ? `${urlPrefix}:${url}` : url}>{url}</a>
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
		<Section title={title}>
			<div className="d-flex flex-wrap">
				{labels.map((label, index) => (
					<ClayLabel className="mr-2" key={index}>
						{label}
					</ClayLabel>
				))}
			</div>
		</Section>
	);
}

function AppDetailContent({product}: {product: Product}) {
	const navigate = useNavigate();

	const {code} = product.workflowStatusInfo;

	const appVersion = getSpecificationValue(
		product,
		ProductSpecificationKey.APP_VERSION
	);

	const areas = getProductCategoriesByVocabularyName(
		product.categories,
		'marketplace-app-category'
	);

	const category = getProductCategoriesByVocabularyName(
		product.categories,
		'marketplace-category'
	);

	const tags = getProductCategoriesByVocabularyName(
		product.categories,
		'marketplace-app-tags'
	);

	const storefrontImages = product.images.filter(
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
			<ClayButton
				className="align-items-center d-flex mb-4"
				displayType="unstyled"
				onClick={() => navigate('/mp-apps')}
			>
				<ClayIcon className="mr-2" symbol="order-arrow-left" />

				<span className="h5 mb-0">
					{i18n.translate('back-to-apps')}
				</span>
			</ClayButton>

			{code === ProductWorkflowStatusCode.DRAFT && (
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
				<Section title={i18n.translate('description')}>
					<div
						dangerouslySetInnerHTML={{
							__html: DOMPurify.sanitize(
								product.description?.en_US ?? ''
							),
						}}
					/>
				</Section>

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

				<Section title={i18n.translate('storefront')}>
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
				</Section>

				<Section
					isLastSection
					title={i18n.translate('support-and-help')}
				>
					{supportItems.map((supportItem, index) => (
						<SupportContent key={index} {...supportItem} />
					))}
				</Section>
			</div>
		</div>
	);
}

export default function AppDetail() {
	const {productId} = useParams();

	const {data: product, error, isLoading} = useAdminApp(productId!);

	return (
		<PageRenderer error={error} isLoading={isLoading}>
			{product && <AppDetailContent product={product} />}
		</PageRenderer>
	);
}
