/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayIcon from '@clayui/icon';
import RadioCard from '~/components/RadioCard/RadioCard';
import Section from '~/components/Section/Section';
import {useOneContext} from '~/context/OneContextProvider';
import i18n from '~/i18n';
import {useProductPurchaseLayoutContext} from '~/pages/ProductPurchase/components/ProductPurchaseLayout/ProductPurchaseLayout';
import {PaymentMethodType} from '~/pages/ProductPurchase/types';

interface PaymentTypeSelectorProps {
	allowedPaymentMethodTypes?: PaymentMethodType[];
}

const PaymentTypeSelector = ({
	allowedPaymentMethodTypes,
}: PaymentTypeSelectorProps) => {
	const {myUserAccount} = useOneContext();
	const {payment, productPurchaseCart, setPayment} =
		useProductPurchaseLayoutContext();

	const paymentModes = [
		{
			onSelect: () =>
				setPayment((previousPayment) => ({
					...previousPayment,
					type: PaymentMethodType.PAY_NOW,
				})),
			subtitle: i18n.translate('online-payments-with-paypal'),
			symbol: 'credit-card',
			title: i18n.translate('pay-with-card'),
			type: PaymentMethodType.PAY_NOW,
		},
		{
			onSelect: () =>
				setPayment((previousPayment) => ({
					...previousPayment,
					invoice: {
						email: myUserAccount?.emailAddress || '',
						purchaseOrderNumber: String(
							productPurchaseCart.cart?.id || ''
						),
					},
					type: PaymentMethodType.INVOICE,
				})),
			subtitle: i18n.translate('offline-payments-using-the-invoice'),
			symbol: 'document-text',
			title: i18n.translate('pay-with-bank-transfer'),
			type: PaymentMethodType.INVOICE,
		},
	];

	const filteredPaymentModes = paymentModes.filter((paymentMode) => {
		if (!allowedPaymentMethodTypes) {
			return true;
		}

		return allowedPaymentMethodTypes.includes(paymentMode.type);
	});

	return (
		<Section label={i18n.translate('payment-method')} required>
			{filteredPaymentModes.map((paymentMode) => (
				<RadioCard
					className="mb-3"
					content={
						<div className="align-items-center d-flex">
							<ClayIcon
								className="mr-3 text-primary"
								symbol={paymentMode.symbol}
							/>

							<div>
								<p className="mb-0">{paymentMode.title}</p>

								<small className="text-muted">
									{paymentMode.subtitle}
								</small>
							</div>
						</div>
					}
					key={paymentMode.type}
					onChange={paymentMode.onSelect}
					selected={payment.type === paymentMode.type}
				/>
			))}
		</Section>
	);
};

export default PaymentTypeSelector;
