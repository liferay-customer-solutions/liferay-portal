/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Suspense, lazy, useEffect} from 'react';
import {HashRouter, useRoutes} from 'react-router-dom';
import EmptyState from '~/components/EmptyState/EmptyState';
import Loading from '~/components/Loading/Loading';
import {useDeliveryProduct} from '~/hooks/useDeliveryProduct';
import i18n from '~/i18n';
import {Liferay} from '~/services/liferay/liferay';
import {getProductPriceModel} from '~/utils/productUtils';
import {AppRoute, toRouteObjects} from '~/utils/routeUtils';

import ProductPurchaseLayout from './components/ProductPurchaseLayout/ProductPurchaseLayout';
import {
	getProductPurchaseSteps,
	toStepItems,
	toStepRoutes,
} from './productPurchaseRoutes';

import './ProductPurchase.css';

import type {DeliveryProduct} from '~/types/product';

const BankTransferCompleted = lazy(
	() => import('./BankTransferCompleted/BankTransferCompleted')
);
const PurchaseCompleted = lazy(
	() => import('./PurchaseCompleted/PurchaseCompleted')
);

const ProductPurchaseRoutes = ({product}: {product: DeliveryProduct}) => {
	const {isPaidApp} = getProductPriceModel(product);

	const steps = getProductPurchaseSteps(isPaidApp);

	const routes: AppRoute[] = [
		{
			children: toStepRoutes(steps),
			element: (
				<ProductPurchaseLayout
					product={product}
					steps={toStepItems(steps)}
				/>
			),
		},
		{
			element: <BankTransferCompleted product={product} />,
			path: 'bank-transfer-completed',
		},
		{
			element: <PurchaseCompleted product={product} />,
			path: 'purchase-completed',
		},
	];

	return useRoutes(toRouteObjects(routes));
};

const ProductPurchaseRouter = () => {
	const searchParams = new URLSearchParams(window.location.search);

	const productId = searchParams.get('productId') ?? '';

	const isSignedIn = Liferay.ThemeDisplay.isSignedIn();

	const {data: product, isLoading} = useDeliveryProduct(productId);

	useEffect(() => {
		if (!isSignedIn) {
			Liferay.Util.navigate(
				`/c/portal/login?redirect=${encodeURIComponent(
					window.location.pathname + window.location.search
				)}`
			);
		}
	}, [isSignedIn]);

	if (!isSignedIn) {
		return null;
	}

	if (isLoading) {
		return (
			<div className="d-flex justify-content-center my-7">
				<Loading />
			</div>
		);
	}

	if (!productId || !product?.productId) {
		return (
			<EmptyState
				description={i18n.translate(
					'this-product-is-no-longer-available'
				)}
				title={i18n.translate('product-unavailable')}
				type="NOT_FOUND"
			/>
		);
	}

	return (
		<HashRouter>
			<div className="my-7 product-purchase">
				<Suspense fallback={null}>
					<ProductPurchaseRoutes product={product} />
				</Suspense>
			</div>
		</HashRouter>
	);
};

export default ProductPurchaseRouter;
