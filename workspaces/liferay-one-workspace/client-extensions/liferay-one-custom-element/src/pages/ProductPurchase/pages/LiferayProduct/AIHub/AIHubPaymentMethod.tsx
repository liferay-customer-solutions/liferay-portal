/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useSelector} from '@xstate/store/react';
import {useEffect, useMemo, useState} from 'react';
import {useNavigate} from 'react-router-dom';

import ProductPurchase from '~/components/ProductPurchase';
import useAccountAddresses from '~/pages/ProductPurchase/hooks/useAccountAddresses';
import i18n from '~/i18n';
import {commerceSchemas as commerceZodSchema} from '~/schema/commerceSchemas';
import HeadlessAdminUser from '~/services/headless/HeadlessAdminUser';
import HeadlessCommerceDeliveryCart from '~/services/headless/HeadlessCommerceDeliveryCart';
import {useProductPurchaseLayoutContext as useProductPurchaseOutletContext} from '~/pages/ProductPurchase/components/ProductPurchaseLayout/ProductPurchaseLayout';
import ProductPurchaseApp from '~/services/commerce/ProductPurchaseApp';
import {cartStore} from '../../../store';
import {productPurchaseStore} from '../../../store/AppPurchaseStore';
import {PaymentMethodType} from '../../../types';
import BillingAddress from '~/pages/ProductPurchase/PaymentMethod/components/BillingAddress/BillingAddress';
import PaymentTypeSelector from '~/pages/ProductPurchase/PaymentMethod/components/PaymentTypeSelector/PaymentTypeSelector';
import TaxIdDisplay from './TaxIdDisplay';

export default function PaymentMethod() {
	const [loading, setLoading] = useState(false);
	const navigate = useNavigate();

	const payment = useSelector(
		productPurchaseStore,
		(state) => state.context.payment
	);

	const isPrimaryButtonActive = useMemo(() => {
		const isAddressValid = commerceZodSchema.billingAddress.safeParse(
			payment.billingAddress
		);

		const isTaxIdValid = !!payment.taxId;

		return isAddressValid.success && isTaxIdValid;
	}, [payment]);

	const {licenseType} = useSelector(
		productPurchaseStore,
		(state) => state.context
	);

	const {
		actions: {nextStep, previousStep},
		handlePurchase,
		payment: contextPayment,
		product,
		productPurchaseCart,
		selectedAccount,
	} = useProductPurchaseOutletContext();

	useEffect(() => {
		if (contextPayment?.billingAddress) {
			productPurchaseStore.send({
				billingAddress: contextPayment.billingAddress,
				type: 'setBillingAddress',
			});
		}
	}, [contextPayment?.billingAddress]);

	useEffect(() => {
		if (selectedAccount?.taxId && !payment.taxId) {
			productPurchaseStore.send({
				taxId: selectedAccount.taxId,
				type: 'setAccountTaxId',
			});
		}
	}, [selectedAccount?.taxId, payment.taxId]);

	const {data: addressResponse} = useAccountAddresses(selectedAccount?.id);
	const addresses = addressResponse?.items ?? [];

	useEffect(() => {
		if (!licenseType) {

			// Force redirect to checkout homepage

			navigate('/');
		}
	}, [licenseType, navigate]);

	useEffect(() => {
		productPurchaseStore.send({
			paymentMethodType: PaymentMethodType.PAY_NOW,
			type: 'setPaymentMethodType',
		});
	}, []);

	const onClickContinue = async () => {
		setLoading(true);

		try {
			if (licenseType === 'TRIAL') {
				return handlePurchase(
					new ProductPurchaseApp(selectedAccount, product),
					{isTrialSKU: true}
				);
			}

			await productPurchaseCart.updateCart(productPurchaseCart.cart.id, {
				billingAddress: payment.billingAddress,
			});

			if (payment.taxId && !selectedAccount.taxId) {
				await HeadlessAdminUser.updateAccount(selectedAccount.id, {
					taxId: payment.taxId,
				});
			}

			cartStore.send({
				cart: await HeadlessCommerceDeliveryCart.getCart(
					productPurchaseCart.cart.id
				),
				type: 'setCart',
			});

			nextStep();
		}
		finally {
			setLoading(false);
		}
	};

	return (
		<ProductPurchase.Shell
			className="select-payment-step"
			footerProps={{
				backButtonProps: {
					onClick: previousStep,
				},
				continueButtonProps: {
					children: i18n.translate('continue'),
					disabled: !isPrimaryButtonActive || loading,
					onClick: onClickContinue,
				},
			}}
			title={i18n.translate('payment-method')}
		>
			<BillingAddress hideNewAddressButton={!!addresses.length} />

			<TaxIdDisplay />

			<PaymentTypeSelector
				allowedPaymentMethodTypes={[PaymentMethodType.PAY_NOW]}
			/>
		</ProductPurchase.Shell>
	);
}
