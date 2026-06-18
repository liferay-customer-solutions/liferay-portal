/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import orderArrowRightIconUrl from '../../../assets/icons/order_arrow_right.svg';
import AccountAvatar from '../../../components/AccountAvatar';
import {ProductImageFallbackCategories} from '../../../enums/Product';
import i18n from '../../../i18n';
import {getProductImageFallback} from '../../../utils/productUtils';
import {normalizeURLProtocol} from '../../../utils/string';

type ProductPurchaseHeaderCardsProps = {
	account?: Account;
	product: DeliveryProduct;
};

const ProductPurchaseHeaderCards = ({
	account,
	product,
}: ProductPurchaseHeaderCardsProps) => {
	return (
		<div className="align-items-center d-flex justify-content-center">
			<div className="flex-grow-1 product-purchase-banner px-4 py-3">
				<div className="align-items-center d-flex">
					<img
						alt={product.name}
						className="object-fit-cover rounded"
						draggable={false}
						height="56px"
						src={
							normalizeURLProtocol(product.urlImage) ||
							getProductImageFallback(
								ProductImageFallbackCategories.PRODUCT_ICON
							)
						}
						width="56px"
					/>

					<div className="ml-3">
						<small className="d-block text-muted">
							{i18n.translate('application')}
						</small>

						<strong>{product.name}</strong>
					</div>
				</div>
			</div>

			{account?.id && (
				<>
					<img
						alt=""
						className="mx-3"
						height="16px"
						src={orderArrowRightIconUrl}
						width="16px"
					/>

					<div className="flex-grow-1 product-purchase-banner px-4 py-3">
						<div className="align-items-center d-flex">
							<AccountAvatar
								logoURL={account.logoURL}
								size={56}
								type={account.type}
							/>

							<div className="ml-3">
								<small className="d-block text-muted">
									{i18n.translate('account')}
								</small>

								<strong>{account.name}</strong>
							</div>
						</div>
					</div>
				</>
			)}
		</div>
	);
};

export default ProductPurchaseHeaderCards;
