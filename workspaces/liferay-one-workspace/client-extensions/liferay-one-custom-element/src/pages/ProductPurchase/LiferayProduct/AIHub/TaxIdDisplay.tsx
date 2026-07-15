/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Input} from '~/components/Input/Input';
import Section from '~/components/Section/Section';
import i18n from '~/i18n';
import {useProductPurchaseLayoutContext as useProductPurchaseOutletContext} from '~/pages/ProductPurchase/components/ProductPurchaseLayout/ProductPurchaseLayout';
import {useAppPurchaseContext} from '~/pages/ProductPurchase/context';

const TaxIdDisplay = () => {
	const {selectedAccount} = useProductPurchaseOutletContext();

	const {payment, setAccountTaxId} = useAppPurchaseContext();

	return (
		<Section label={i18n.translate('tax-vat-id')}>
			<Input
				defaultValue={selectedAccount.taxId}
				disabled={!!selectedAccount?.taxId}
				onChange={({target: {value}}) => setAccountTaxId(value)}
				placeholder={i18n.translate('enter-your-vat-id')}
				required
				value={payment.taxId}
			/>
		</Section>
	);
};

export default TaxIdDisplay;
