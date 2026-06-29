/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';
import {useLocation} from 'react-router-dom';
import purchaseInvoiceIconUrl from '~/assets/icons/purchase_invoice.svg';
import EmptyState from '~/components/EmptyState/EmptyState';
import i18n from '~/i18n';
import ProductPurchaseHeaderCards from '~/pages/ProductPurchase/components/ProductPurchaseHeaderCards/ProductPurchaseHeaderCards';
import {Liferay} from '~/services/liferay/liferay';
import {getSiteURL} from '~/utils/siteUtils';

import type {Account} from '~/types/accounts';
import type {DeliveryProduct} from '~/types/product';

type BankTransferCompletedProps = {
	product: DeliveryProduct;
};

const BankTransferCompleted = ({product}: BankTransferCompletedProps) => {
	const {search, state} = useLocation();

	const urlSearchParams = new URLSearchParams(
		search || window.location.search
	);

	const orderId = urlSearchParams.get('orderId') ?? '';

	const account = (state as {account?: Account} | null)?.account;

	if (!orderId) {
		return (
			<EmptyState
				title={i18n.translate('no-results-found')}
				type="NOT_FOUND"
			/>
		);
	}

	return (
		<div className="product-purchase-completed">
			<ProductPurchaseHeaderCards account={account} product={product} />

			<div className="d-flex justify-content-center mt-5">
				<img
					alt=""
					height="64px"
					src={purchaseInvoiceIconUrl}
					width="74px"
				/>
			</div>

			<h1 className="mt-4 product-purchase-shell-title text-center">
				{i18n.translate('order-confirmation')}
			</h1>

			<p className="mt-3 text-center text-muted">
				{i18n.translate(
					'you-will-receive-an-invoice-via-email-with-the-instructions-to-complete-your-bank-transfer-payment'
				)}
			</p>

			<p className="mt-4 text-center">
				{i18n.translate('your-order-id-is')}{' '}
				<strong className="text-primary">{orderId}</strong>
			</p>

			<hr className="my-4" />

			<div className="d-flex justify-content-center">
				<ClayButton
					displayType="secondary"
					onClick={() =>
						Liferay.Util.navigate(
							`${getSiteURL()}/my-account#/project/applications`
						)
					}
				>
					{i18n.translate('go-to-my-apps')}
				</ClayButton>

				<ClayButton
					className="ml-3"
					onClick={() =>
						Liferay.Util.navigate(`${getSiteURL()}/marketplace`)
					}
				>
					{i18n.translate('go-to-the-catalog')}
				</ClayButton>
			</div>
		</div>
	);
};

export default BankTransferCompleted;
