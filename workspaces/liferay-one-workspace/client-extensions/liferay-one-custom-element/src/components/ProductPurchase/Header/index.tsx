/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ComponentProps, ReactNode} from 'react';
import ClayIcon from '@clayui/icon';
import ClaySticker from '@clayui/sticker';

import i18n from '~/i18n';
import {Liferay} from '~/services/liferay/liferay';
import {normalizeURLProtocol} from '~/utils/stringUtils';
import type {Account} from '~/types/accounts';
import type {DeliveryProduct} from '~/types/product';

type ProductPurchaseHeaderAccountProps = {
	account: Account;
};

type ProductPurchaseHeaderProps = {
	product: DeliveryProduct;
	rightNode?: ReactNode;
};

const AccountEmailInfo: React.FC<{image?: string; name?: string}> = ({image, name}) => (
	<div className="align-items-center d-flex">
		<div className="account-banner-name-text align-items-end d-flex flex-column mx-2">
			<strong>{name}</strong>

			<div className="account-banner-email-text">
				{Liferay.ThemeDisplay.getUserEmailAddress()}
			</div>
		</div>

		<ClaySticker displayType="light" shape="circle" size="sm">
			{image ? (
				<ClaySticker.Image
					alt="placeholder"
					draggable={false}
					height={24}
					src={image}
					width={24}
				/>
			) : (
				<ClayIcon symbol="picture" />
			)}
		</ClaySticker>
	</div>
);

const ProductPurchaseHeaderAccount: React.FC<
	ProductPurchaseHeaderAccountProps
> = ({account}) => {
	if (!account) {
		return null;
	}

	return (
		<>
			<hr className="mx-n5" />

			<div className="d-flex flex-row justify-content-between">
				<strong className="account-banner-title-text align-self-center">
					{i18n.translate('account-selected')}
				</strong>

				<AccountEmailInfo image={account.logoURL} name={account.name} />
			</div>
		</>
	);
};

const ProductPurchaseHeader: React.FC<ProductPurchaseHeaderProps> = ({
	product,
	rightNode,
}) => {
	const HeadingComponent = product.name.length > 30 ? 'h3' : 'h1';

	return (
		<div className="product-banner px-5 py-5">
			<div className="d-flex flex-row justify-content-between">
				<div className="d-flex flex-row">
					<img
						alt="App Icon"
						className="object-fit-cover rounded"
						draggable={false}
						height="64px"
						src={normalizeURLProtocol(product.urlImage)}
						width="64px"
					/>

					<div className="align-items-center ml-4">
						<HeadingComponent className="font-weight-semi-bold product-banner-title">
							{product.name}
						</HeadingComponent>

						<span className="sub-text">{product.catalogName}</span>
					</div>
				</div>

				{rightNode}
			</div>
		</div>
	);
};

export {ProductPurchaseHeaderAccount};

export default ProductPurchaseHeader;
