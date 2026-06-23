/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Input} from '~/components/Input/Input';
import Section from '~/components/Section/Section';
import i18n from '~/i18n';
import {useProductPurchaseLayoutContext} from '~/pages/ProductPurchase/components/ProductPurchaseLayout/ProductPurchaseLayout';

const TaxIdInput = () => {
	const {payment, setPayment} = useProductPurchaseLayoutContext();

	return (
		<Section label={i18n.translate('tax-vat-id')}>
			<Input
				onChange={({target: {value}}) =>
					setPayment((previousPayment) => ({
						...previousPayment,
						taxId: value,
					}))
				}
				placeholder={i18n.translate('enter-your-vat-id')}
				value={payment.taxId}
			/>
		</Section>
	);
};

export default TaxIdInput;
