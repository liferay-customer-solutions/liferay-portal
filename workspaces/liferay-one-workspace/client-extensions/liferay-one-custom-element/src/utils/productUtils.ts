/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import productIconFallback from '~/assets/icons/purchased_app_icon.svg';
import productImageFallback from '~/assets/images/app_placeholder.png';
import i18n from '~/i18n';

import {getValueFromDeliverySpecifications} from './getValueFromDeliverySpecifications';

import type {
	DeliveryProduct,
	DeliverySKUOption,
	ProductImageFallbackCategories,
	ProductLicense,
	ProductLicenseTier,
	ProductOfferingTypes,
	ProductType,
	SKU,
	SkuOptions,
} from '~/types/product';

export const ProductSpecificationKey = {
	APP_BETA: 'app-beta',
	APP_BUILD_NUMBER_OF_CPUS: 'cpu',
	APP_BUILD_RAM_IN_GBS: 'ram',
	APP_DEVELOPER_NAME: 'developer-name',
	APP_ENTRY_UUID: 'app-entry-uuid',
	APP_LICENSING_TYPE: 'license-type',
	APP_PRICING_MODEL: 'price-model',
	APP_SETTINGS: 'app-settings',
	APP_STOREFRONT_VIDEO_DESCRIPTION: 'app-storefront-video-description',
	APP_STOREFRONT_VIDEO_URL: 'app-storefront-video-url',
	APP_SUPPORT_DOCUMENTATION_URL: 'appdocumentationurl',
	APP_SUPPORT_EMAIL: 'supportemailaddress',
	APP_SUPPORT_INSTALLATION_GUIDE_URL: 'appinstallationguideurl',
	APP_SUPPORT_PHONE: 'supportphone',
	APP_SUPPORT_PUBLISHER_WEBSITE_URL: 'publisherwebsiteurl',
	APP_SUPPORT_URL: 'supporturl',
	APP_SUPPORT_USAGE_TERMS_URL: 'appusagetermsurl',
	APP_TYPE: 'type',
	APP_VERSION: 'latest-version',
	APP_VERSION_NOTES: 'product-notes',
	LIFERAY_PRODUCT_TYPE: 'liferay-product-type',
	LIFERAY_VERSION: 'liferay-version',
	SOLUTION_COMPANY_DESCRIPTION: 'solution-company-description',
	SOLUTION_COMPANY_EMAIL: 'solution-company-email',
	SOLUTION_COMPANY_PHONE: 'solution-company-phone',
	SOLUTION_COMPANY_WEBSITE: 'solution-company-website',
	SOLUTION_CONTACT_EMAIL: 'solution-contact-email',
	SOLUTION_DETAILS_BLOCKS: 'solution-details-blocks',
	SOLUTION_HEADER_DESCRIPTION: 'solution-header-description',
	SOLUTION_HEADER_TITLE: 'solution-header-title',
	SOLUTION_HEADER_VIDEO_DESCRIPTION: 'solution-header-video-description',
	SOLUTION_HEADER_VIDEO_URL: 'solution-header-video-url',
	SOLUTION_TYPE: 'solution-type',
} as const;

export type ProductSpecificationKey =
	(typeof ProductSpecificationKey)[keyof typeof ProductSpecificationKey];

export const ProductWorkflowStatusCode = {
	APPROVED: 0,
	DRAFT: 2,
	PENDING: 1,
};

export type ProductWorkflowStatusCode =
	(typeof ProductWorkflowStatusCode)[keyof typeof ProductWorkflowStatusCode];

const ALL_OFFERINGS: ProductOfferingTypes[] = [
	'Liferay PaaS',
	'Liferay SaaS',
	'Liferay Self-Hosted',
];

const offeringTypes: Record<string, ProductOfferingTypes[]> = {
	'client-extension': ALL_OFFERINGS,
	'cloud': ['Liferay SaaS'],
	'composite-app': ['Liferay Self-Hosted'],
	'dxp': ['Liferay PaaS', 'Liferay Self-Hosted'],
	'low-code-configuration': ALL_OFFERINGS,
	'other': ALL_OFFERINGS,
};

export const ProductTypeLabels = {
	'client-extension': 'Client Extension',
	'cloud': 'Cloud',
	'composite-app': 'Composite App',
	'dxp': 'DXP',
	'low-code-configuration': 'Low-Code Configuration',
	'other': 'Other',
	'ssa-saas': 'SSA SaaS',
} as const;

export const ProductTypeLicenseOptions: Record<string, ProductLicenseTier[]> = {
	'client-extension': ['standard'],
	'cloud': ['standard'],
	'composite-app': ['standard'],
	'dxp': ['standard', 'developer', 'trial'],
	'low-code-configuration': ['standard'],
	'other': ['standard'],
	'ssa-saas': ['standard'],
};

export const ProductWorkflowDisplayType = {
	[ProductWorkflowStatusCode.APPROVED]: 'success',
	[ProductWorkflowStatusCode.DRAFT]: 'secondary',
	[ProductWorkflowStatusCode.PENDING]: 'warning',
};

export const ProductWorkflowStatusLabel = {
	[ProductWorkflowStatusCode.APPROVED]: i18n.translate('approved'),
	[ProductWorkflowStatusCode.DRAFT]: i18n.translate('draft'),
	[ProductWorkflowStatusCode.PENDING]: i18n.translate('under-review'),
};

export function getOfferingTypes(type: ProductType) {
	return offeringTypes[type];
}

export function getProductFallback(): DeliveryProduct {
	return {
		attachments: [],
		catalogName: '',
		categories: [],
		createDate: '',
		description: i18n.translate('this-product-is-no-longer-available'),
		externalReferenceCode: '--',
		id: 0,
		images: [],
		modifiedDate: '',
		name: i18n.translate('product-unavailable'),
		productId: 0,
		productSpecifications: [],
		productType: i18n.translate('product-unavailable'),
		shortDescription: i18n.translate('this-product-is-no-longer-available'),
		skus: [],
		urlImage: '',
		urls: {en_US: ''},
	};
}

export function getProductImageFallback(type: ProductImageFallbackCategories) {
	const productImagesFallback = {
		productIcon: productIconFallback,
		productImage: productImageFallback,
	};

	return productImagesFallback[type] || '';
}

export function getProductSpecification(
	key: ProductSpecificationKey,
	product: DeliveryProduct
) {
	return product?.productSpecifications?.find(
		({specificationKey}) => specificationKey === key
	);
}

export function getProductSpecificationValue<T = string>(
	key: ProductSpecificationKey,
	product: DeliveryProduct,
	value?: T
) {
	return getProductSpecification(key, product)?.value || (value as T);
}

export function isTrialSKU(sku: SKU) {
	const skuName = sku.sku.toLowerCase();
	const skuOptions = getNormalizedSKUOptions(sku) || [];

	return (
		skuName.endsWith('ts') ||
		skuName === 'trial' ||
		['trial', 'yes'].some(
			(optionValue) =>
				skuOptions[0]?.value?.toLowerCase() ===
				optionValue.toLowerCase()
		)
	);
}

export function getNormalizedSKUOptions(sku: SKU) {
	return (sku.skuOptions || []).map((skuOption) => {
		if ((skuOption as unknown as DeliverySKUOption).skuOptionKey) {
			return {
				key: (skuOption as unknown as DeliverySKUOption).skuOptionKey,
				value: (skuOption as unknown as DeliverySKUOption)
					.skuOptionValueKey,
			};
		}

		return skuOption;
	});
}

export function getSkuByOptionValueKey(
	product: DeliveryProduct,
	skuOptionValueKey: SkuOptions
) {
	return product.skus.find(
		({purchasable, skuOptions}) =>
			purchasable &&
			skuOptions?.find(
				(skuOption) =>
					[
						'cloud-license-usage-type',
						'dxp-license-usage-type',
					].includes(skuOption.skuOptionKey as ProductLicense) &&
					skuOption.skuOptionValueKey === skuOptionValueKey
			)
	);
}

export function getProductType(product: DeliveryProduct) {
	const specification = getProductSpecificationValue(
		ProductSpecificationKey.APP_TYPE,
		product
	);

	return {
		isCloud: specification === 'cloud',
		isDXP: specification === 'dxp',
	};
}

export function getLicenseTagText(product: DeliveryProduct) {
	const licenseTypeSpecification = getValueFromDeliverySpecifications(
		product.productSpecifications,
		ProductSpecificationKey.APP_LICENSING_TYPE
	).toLowerCase();

	return licenseTypeSpecification === 'Perpetual' ? 'One-Time' : 'Annually';
}

export function getProductPriceModel(product: DeliveryProduct) {
	const priceModel = getProductSpecificationValue(
		ProductSpecificationKey.APP_PRICING_MODEL,
		product
	)?.toLowerCase();

	return {
		isFreeApp: priceModel === 'free',
		isPaidApp: priceModel === 'paid',
		priceModel,
	};
}
