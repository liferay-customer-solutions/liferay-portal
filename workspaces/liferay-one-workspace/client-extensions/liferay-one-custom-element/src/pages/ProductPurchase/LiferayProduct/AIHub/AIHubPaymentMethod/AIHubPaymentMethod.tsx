/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useEffect, useMemo, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import ProductPurchase from '~/components/ProductPurchase/ProductPurchase';
import i18n from '~/i18n';
import BillingAddress from '~/pages/ProductPurchase/PaymentMethod/components/BillingAddress/BillingAddress';
import PaymentTypeSelector from '~/pages/ProductPurchase/PaymentMethod/components/PaymentTypeSelector/PaymentTypeSelector';
import {useProductPurchaseLayoutContext as useProductPurchaseOutletContext} from '~/pages/ProductPurchase/components/ProductPurchaseLayout/ProductPurchaseLayout';
import {useAppPurchaseContext} from '~/pages/ProductPurchase/context/AppPurchaseContext';
import {useCartContext} from '~/pages/ProductPurchase/context/CartContext';
import useAccountAddresses from '~/pages/ProductPurchase/hooks/useAccountAddresses';
import {PaymentMethodType} from '~/pages/ProductPurchase/types';
import {commerceSchemas as commerceZodSchema} from '~/schema/commerceSchemas';
import ProductPurchaseApp from '~/services/commerce/ProductPurchaseApp';
import HeadlessAdminUser from '~/services/headless/HeadlessAdminUser';
import HeadlessCommerceDeliveryCart from '~/services/headless/HeadlessCommerceDeliveryCart';

import TaxIdDisplay from '../TaxIdDisplay';

export default function AIHubPaymentMethod() {
	const [loading, setLoading] = useState(false);
	const navigate = useNavigate();

	const {
		licenseType,
		payment,
		salesforceProject,
		setAccountTaxId,
		setBillingAddress,
		setPaymentMethodType,
	} = useAppPurchaseContext();

	const {setCart} = useCartContext();

	const isPrimaryButtonActive = useMemo(() => {
		const isAddressValid = commerceZodSchema.billingAddress.safeParse(
			payment.billingAddress
		);

		const isTaxIdValid = !!payment.taxId;

		return isAddressValid.success && isTaxIdValid;
	}, [payment]);

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
			setBillingAddress(contextPayment.billingAddress);
		}
	}, [contextPayment?.billingAddress, setBillingAddress]);

	useEffect(() => {
		if (selectedAccount?.taxId && !payment.taxId) {
			setAccountTaxId(selectedAccount.taxId);
		}
	}, [selectedAccount?.taxId, payment.taxId, setAccountTaxId]);

	const {data: addressResponse} = useAccountAddresses(selectedAccount?.id);
	const addresses = addressResponse?.items ?? [];

	useEffect(() => {
		if (!licenseType) {
			navigate('/');
		}
	}, [licenseType, navigate]);

	useEffect(() => {
		setPaymentMethodType(PaymentMethodType.PAY_NOW);
	}, [setPaymentMethodType]);

	const onClickContinue = async () => {
		setLoading(true);

		try {
			if (licenseType === 'TRIAL') {
				return handlePurchase(
					new ProductPurchaseApp(
						selectedAccount,
						product,
						salesforceProject
					),
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

			setCart(
				await HeadlessCommerceDeliveryCart.getCart(
					productPurchaseCart.cart.id
				)
			);

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
