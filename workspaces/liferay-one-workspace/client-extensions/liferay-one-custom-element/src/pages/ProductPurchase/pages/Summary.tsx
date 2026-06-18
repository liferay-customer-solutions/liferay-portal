/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import classNames from 'classnames';
import {useState} from 'react';
import {Navigate} from 'react-router-dom';

import i18n from '../../../i18n';
import {Liferay} from '../../../liferay/liferay';
import {formatCurrency} from '../../../utils/currencies';
import {useProductPurchaseOutletContext} from '../ProductPurchaseOutlet';
import LicenseTermsCheckbox from '../components/LicenseTermsCheckbox';
import ProductPurchaseShell from '../components/ProductPurchaseShell';

const Summary = () => {
	const [eulaAgreement, setEulaAgreement] = useState(false);

	const {
		actions: {previousStep},
		handlePurchase,
		isSingleAccount,
		isSubmitting,
		product,
		selectedAccount,
	} = useProductPurchaseOutletContext();

	if (!selectedAccount?.id) {
		return <Navigate replace to="/" />;
	}

	const freePrice = formatCurrency(
		0,
		Liferay.CommerceContext.currency.currencyCode
	);

	const summaryRows = [
		{label: i18n.translate('net-price'), value: freePrice},
		{label: i18n.translate('vat'), value: freePrice},
		{label: i18n.translate('total'), value: freePrice},
	];

	return (
		<ProductPurchaseShell
			footerProps={{
				backButtonProps: {
					className: classNames({'d-none': isSingleAccount}),
					onClick: () => previousStep(),
				},
				continueButtonProps: {
					children: i18n.translate('get-app'),
					disabled: !eulaAgreement || isSubmitting,
					onClick: () => handlePurchase(),
				},
			}}
			title={i18n.translate('summary')}
		>
			<h5 className="mb-2">{i18n.translate('order-summary')}</h5>

			<hr className="mt-0" />

			{summaryRows.map(({label, value}) => (
				<div className="align-items-center d-flex mb-1" key={label}>
					<span className="product-purchase-summary-label text-right">
						{label}:
					</span>

					<strong className="ml-2">{value}</strong>
				</div>
			))}

			<LicenseTermsCheckbox
				checked={eulaAgreement}
				onChange={() => setEulaAgreement(!eulaAgreement)}
				product={product}
			/>
		</ProductPurchaseShell>
	);
};

export default Summary;
