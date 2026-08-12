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
	ProductCategories,
	ProductImageFallbackCategories,
	ProductLicense,
	ProductLicenseTier,
	ProductOfferingTypes,
	ProductType,
	SKU,
	SkuOptions,
} from '~/types/product';

export {
	ProductLicense,
	ProductLicenseFriendlyName,
	ProductLicenseTier,
	ProductLicenseType,
	ProductOfferingTypes,
	ProductPriceModel,
	ProductSpecificationKey,
	ProductTags,
	ProductType,
	ProductTypeLabels,
	ProductTypeLicenseOptions,
	ProductTypeVocabulary,
	ProductUploadType,
	ProductVocabulary,
	ProductWorkflowDisplayType,
	ProductWorkflowStatusCode,
	ProductWorkflowStatusLabel,
	SkuOptions,
} from '~/enums/Product';

export function getProductCategoriesByVocabularyName(
	categories: ProductCategories[],
	vocabulary: string
) {
	return categories
		.filter((category) =>
			category.vocabulary
				.replaceAll(' ', '-')
				.toLowerCase()
				.includes(vocabulary.toLowerCase())
		)
		.map(({name}) => name);
}



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

export function isLDPProduct(product: DeliveryProduct) {
	return (
		getProductSpecificationValue(
			ProductSpecificationKey.SOLUTION_TYPE,
			product
		) === 'liferay-data-platform'
	);
}

export function isDXPFreeTierProduct(product: DeliveryProduct) {
	const {isFreeApp} = getProductPriceModel(product);
	const {isDXP} = getProductType(product);

	return isFreeApp && isDXP;
}

export function getAiHubTokenSKUs(product: DeliveryProduct) {
	return (product.skus ?? [])
		.filter(
			({purchasable, skuOptions}) =>
				purchasable &&
				skuOptions &&
				skuOptions.some((skuOption) =>
					skuOption.skuOptionValueKey.includes('tokens')
				)
		)
		.sort(
			(a, b) =>
				parseInt(a?.sku?.replace(/[^\d]/g, ''), 10) -
				parseInt(b?.sku?.replace(/[^\d]/g, ''), 10)
		);
}
