/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ClayButtonWithIcon} from '@clayui/button';
import ClayIcon from '@clayui/icon';
import i18n from '~/i18n';
import {useProductPurchaseLayoutContext} from '~/pages/ProductPurchase/components/ProductPurchaseLayout/ProductPurchaseLayout';

import LicenseTier from '../LicenseTier/LicenseTier';

import type {DeliverySKU, ProductLicense} from '~/types/product';

const MAX_QUANTITY = 99;
const MIN_QUANTITY = 0;

const licenseTypeDescriptions: Record<string, string> = {
	developer:
		'Limited to 5 unique addresses and should not be used for full scale production deployments.',
	standard:
		'Covers the following DXP environments: production, non-production (UAT) and backup (DR) for both standalone and virtual cluster servers.',
};

type LicenseCardProps = {
	sku: DeliverySKU;
};

const LicenseCard = ({sku}: LicenseCardProps) => {
	const {product, productPurchaseCart} = useProductPurchaseLayoutContext();

	const {addCart, cartItems, removeFromCart} = productPurchaseCart;

	const quantity =
		cartItems.find((item) => item.skuId === sku.id)?.quantity ||
		MIN_QUANTITY;

	const skuOption = sku.skuOptions.find((skuOption) =>
		[
			'base-license-usage-type',
			'cloud-license-usage-type',
			'dxp-license-usage-type',
		].includes(skuOption.skuOptionKey as ProductLicense)
	);

	const licenseType = skuOption?.skuOptionValueKey?.toLocaleLowerCase() ?? '';

	const licenseDescription = licenseTypeDescriptions[licenseType];

	return (
		<div className="border mb-4 p-4 product-purchase-license-card rounded">
			<div className="align-items-start d-flex justify-content-between">
				<div className="mr-4">
					<span className="font-weight-bold text-capitalize">
						{`${licenseType} ${i18n.translate('license')}`}

						<ClayIcon
							className="ml-2 product-purchase-license-card-icon"
							symbol="code"
						/>
					</span>

					{licenseDescription && (
						<p className="mb-0 mt-2 text-muted">
							{licenseDescription}
						</p>
					)}
				</div>

				<div className="align-items-center border d-flex justify-content-between p-1 product-purchase-license-card-stepper rounded-pill">
					<ClayButtonWithIcon
						aria-label={i18n.translate('remove')}
						disabled={quantity === MIN_QUANTITY}
						displayType="primary"
						onClick={() => removeFromCart(sku.id)}
						size="sm"
						symbol="hr"
					/>

					<span className="px-3">{quantity}</span>

					<ClayButtonWithIcon
						aria-label={i18n.translate('add')}
						disabled={quantity === MAX_QUANTITY}
						displayType="primary"
						onClick={() =>
							addCart(
								Number(product.productId ?? product.id),
								sku.id
							)
						}
						size="sm"
						symbol="plus"
					/>
				</div>
			</div>

			<div className="d-flex flex-column mt-4 p-4 product-purchase-license-card-tier rounded">
				<span className="font-weight-bold mb-1">
					{i18n.translate('license-prices')}
				</span>

				<LicenseTier sku={sku} />
			</div>
		</div>
	);
};

export default LicenseCard;
