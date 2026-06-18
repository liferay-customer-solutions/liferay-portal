/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ReactNode} from 'react';

import {
	ProductImageFallbackCategories,
	ProductSpecificationKey,
} from '../../../enums/Product';
import i18n from '../../../i18n';
import {
	getProductImageFallback,
	getProductSpecificationValue,
} from '../../../utils/productUtils';
import {normalizeURLProtocol} from '../../../utils/string';

type ProductPurchaseHeaderProps = {
	children?: ReactNode;
	product: DeliveryProduct;
	rightNode?: ReactNode;
};

const ProductPurchaseHeader = ({
	children,
	product,
	rightNode,
}: ProductPurchaseHeaderProps) => {
	const developerName = getProductSpecificationValue(
		ProductSpecificationKey.APP_DEVELOPER_NAME,
		product,
		product.catalogName
	);

	const version = getProductSpecificationValue(
		ProductSpecificationKey.APP_VERSION,
		product
	);

	return (
		<div className="product-purchase-header-banner p-4">
			<div className="d-flex flex-row justify-content-between">
				<div className="d-flex flex-row">
					<img
						alt={product.name}
						className="object-fit-cover rounded"
						draggable={false}
						height="64px"
						src={
							normalizeURLProtocol(product.urlImage) ||
							getProductImageFallback(
								ProductImageFallbackCategories.PRODUCT_ICON
							)
						}
						width="64px"
					/>

					<div className="align-items-center ml-4">
						<h1 className="font-weight-semi-bold product-purchase-banner-title">
							{product.name}
						</h1>

						<span className="text-muted">
							{version && `${version} `}
							{i18n.translate('by')} {developerName}
						</span>
					</div>
				</div>

				{rightNode}
			</div>

			{children}
		</div>
	);
};

export default ProductPurchaseHeader;
