/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';
import {useLocation} from 'react-router-dom';

import purchaseSuccessIconUrl from '../../../assets/icons/purchase_success.svg';
import EmptyState from '../../../components/EmptyState';
import i18n from '../../../i18n';
import {Liferay} from '../../../liferay/liferay';
import {getSiteURL} from '../../../utils/site';
import ProductPurchaseHeaderCards from '../components/ProductPurchaseHeaderCards';

type PurchaseCompletedProps = {
	product: DeliveryProduct;
};

const PurchaseCompleted = ({product}: PurchaseCompletedProps) => {
	const {search, state} = useLocation();

	const urlSearchParams = new URLSearchParams(
		search || window.location.search
	);

	const orderId = urlSearchParams.get('orderId') ?? '';

	// The selected account is passed via navigation state when the purchase
	// completes, so we don't refetch the placed order here: right after checkout
	// the order is still OPEN and the delivery API returns 404 for it. Only a
	// missing orderId is a real error.

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
					src={purchaseSuccessIconUrl}
					width="74px"
				/>
			</div>

			<h1 className="mt-4 product-purchase-shell-title text-center">
				{i18n.translate('purchase-completed')}
			</h1>

			<p className="mt-3 text-center text-muted">
				{i18n.translate('thank-you-for-choosing')}{' '}
				<strong>{product.name}</strong>.{' '}
				{i18n.translate(
					'your-purchase-has-been-successfully-processed-to-continue-please-click-the-button-below-to-download-or-install-the-app'
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
						Liferay.Util.navigate(
							`${getSiteURL()}/my-account#/project/applications/${orderId}?tab=download`
						)
					}
				>
					{i18n.translate('continue-to-install')}
				</ClayButton>
			</div>
		</div>
	);
};

export default PurchaseCompleted;
