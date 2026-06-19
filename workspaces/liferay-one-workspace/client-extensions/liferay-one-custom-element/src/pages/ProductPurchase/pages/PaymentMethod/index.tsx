/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useState} from 'react';
import {Navigate} from 'react-router-dom';

import i18n from '../../../../i18n';
import zodSchema from '../../../../schema/zod';
import CommerceOrder from '../../../../services/oauth/CommerceOrder';
import HeadlessCommerceDeliveryCart from '../../../../services/rest/HeadlessCommerceDeliveryCart';
import {useProductPurchaseOutletContext} from '../../ProductPurchaseOutlet';
import ProductPurchaseShell from '../../components/ProductPurchaseShell';
import BillingAddress from './BillingAddress';
import PaymentTypeSelector from './PaymentTypeSelector';
import TaxIdInput from './TaxIdInput';

const PaymentMethod = () => {
	const [loading, setLoading] = useState(false);

	const {
		actions: {nextStep, previousStep},
		payment,
		productPurchaseCart,
		selectedAccount,
	} = useProductPurchaseOutletContext();

	if (!selectedAccount?.id) {
		return <Navigate replace to="/" />;
	}

	const isBillingAddressValid = zodSchema.billingAddress.safeParse(
		payment.billingAddress
	).success;

	const onContinue = async () => {
		setLoading(true);

		try {
			const cartId = productPurchaseCart.cart?.id;

			if (cartId) {
				await productPurchaseCart.updateCart(cartId, {
					billingAddress: payment.billingAddress,
					shippingAddress: payment.billingAddress,
				});

				await CommerceOrder.taxCalculate(cartId).catch(console.error);

				productPurchaseCart.setCart(
					await HeadlessCommerceDeliveryCart.getCart(cartId)
				);
			}

			nextStep();
		}
		finally {
			setLoading(false);
		}
	};

	return (
		<ProductPurchaseShell
			footerProps={{
				backButtonProps: {onClick: () => previousStep()},
				continueButtonProps: {
					disabled: !isBillingAddressValid || loading,
					onClick: onContinue,
				},
			}}
			title={i18n.translate('payment-method')}
		>
			<BillingAddress />

			<TaxIdInput />

			<PaymentTypeSelector />
		</ProductPurchaseShell>
	);
};

export default PaymentMethod;
