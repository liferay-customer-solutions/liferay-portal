/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Navigate} from 'react-router-dom';

import i18n from '../../../../i18n';
import {isTrialSKU} from '../../../../utils/productUtils';
import {useProductPurchaseOutletContext} from '../../ProductPurchaseOutlet';
import ProductPurchaseShell from '../../components/ProductPurchaseShell';
import LicenseCard from './LicenseCard';

const License = () => {
	const {
		actions: {nextStep, previousStep},
		isSingleAccount,
		product,
		productPurchaseCart,
		selectedAccount,
	} = useProductPurchaseOutletContext();

	if (!selectedAccount?.id) {
		return <Navigate replace to="/" />;
	}

	const purchasableSkus = (product.skus || []).filter(
		(sku) =>
			sku?.price?.price &&
			sku.purchasable &&
			!isTrialSKU(sku as unknown as SKU)
	);

	const hasCartItems = productPurchaseCart.cartItems.some(
		(cartItem) => cartItem.quantity > 0
	);

	return (
		<ProductPurchaseShell
			footerProps={{
				backButtonProps: {
					className: isSingleAccount ? 'd-none' : undefined,
					onClick: () => previousStep(),
				},
				continueButtonProps: {
					disabled: !hasCartItems,
					onClick: () => nextStep(),
				},
			}}
			title={i18n.translate('license-selection')}
		>
			<p className="text-muted">
				{i18n.translate(
					'select-the-license-type-and-the-number-of-licenses-you-want-to-purchase'
				)}
			</p>

			{purchasableSkus.length ? (
				purchasableSkus.map((sku) => (
					<LicenseCard key={sku.id} sku={sku} />
				))
			) : (
				<p className="font-weight-bold my-5">
					{i18n.translate('no-licenses-available')}
				</p>
			)}
		</ProductPurchaseShell>
	);
};

export default License;
