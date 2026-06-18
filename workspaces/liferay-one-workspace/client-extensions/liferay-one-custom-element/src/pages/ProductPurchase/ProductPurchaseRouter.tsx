/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Suspense, lazy, useEffect} from 'react';
import {HashRouter, Route, Routes} from 'react-router-dom';

import EmptyState from '../../components/EmptyState';
import Loading from '../../components/Loading';
import {useDeliveryProduct} from '../../hooks/data/useProduct';
import i18n from '../../i18n';
import {Liferay} from '../../liferay/liferay';
import {getProductPriceModel} from '../../utils/productUtils';
import ProductPurchaseOutlet from './ProductPurchaseOutlet';
import {getProductPurchaseRoutes} from './productPurchaseRoutes';

import './product_purchase.scss';

const PurchaseCompleted = lazy(() => import('./pages/PurchaseCompleted'));

const ProductPurchaseRouter = () => {
	const searchParams = new URLSearchParams(window.location.search);

	const productId = searchParams.get('productId') ?? '';

	const isSignedIn = Liferay.ThemeDisplay.isSignedIn();

	const {data: product, isLoading} = useDeliveryProduct(productId);

	const {isPaidApp} = getProductPriceModel(product as DeliveryProduct);

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

	const routes = getProductPurchaseRoutes(isPaidApp);

	return (
		<HashRouter>
			<div className="my-7 product-purchase">
				<Suspense fallback={null}>
					<Routes>
						<Route
							element={
								<ProductPurchaseOutlet
									product={product}
									routes={routes}
								/>
							}
						>
							{routes.map((route, index) =>
								route.index ? (
									<Route
										element={route.element}
										index
										key={index}
									/>
								) : (
									<Route
										element={route.element}
										key={index}
										path={route.path}
									/>
								)
							)}
						</Route>

						<Route
							element={<PurchaseCompleted product={product} />}
							path="purchase-completed"
						/>
					</Routes>
				</Suspense>
			</div>
		</HashRouter>
	);
};

export default ProductPurchaseRouter;
