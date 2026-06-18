/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useState} from 'react';
import {
	Outlet,
	useLocation,
	useNavigate,
	useOutletContext,
} from 'react-router-dom';

import AccountAvatar from '../../components/AccountAvatar';
import Loading from '../../components/Loading';
import i18n from '../../i18n';
import {Liferay} from '../../liferay/liferay';
import {getProductPriceModel} from '../../utils/productUtils';
import ProductPurchaseHeader from './components/ProductPurchaseHeader';
import ProductPurchaseSteps from './components/ProductPurchaseSteps';
import useAccounts from './hooks/useAccounts';
import useProductPurchaseCart from './hooks/useProductPurchaseCart';
import {ProductPurchaseRoute} from './productPurchaseRoutes';
import ProductPurchaseApp from './services/ProductPurchaseApp';

type ProductPurchaseOutletProps = {
	product: DeliveryProduct;
	routes: ProductPurchaseRoute[];
};

export type ProductPurchaseOutletContext = {
	accounts: Account[];
	actions: {
		nextStep: () => void;
		previousStep: () => void;
	};
	handlePurchase: () => Promise<void>;
	isLoadingAccounts: boolean;
	isSingleAccount: boolean;
	isSubmitting: boolean;
	product: DeliveryProduct;
	productPurchaseCart: ReturnType<typeof useProductPurchaseCart>;
	selectedAccount: Account;
	setSelectedAccount: React.Dispatch<React.SetStateAction<Account>>;
};

const ProductPurchaseOutlet = ({
	product,
	routes,
}: ProductPurchaseOutletProps) => {
	const [isSubmitting, setSubmitting] = useState(false);
	const {accounts, isLoading, selectedAccount, setSelectedAccount} =
		useAccounts();

	const productPurchaseCart = useProductPurchaseCart(
		selectedAccount?.id,
		product,
		ProductPurchaseApp.getOrderTypeExternalReferenceCode(product)
	);

	const {isFreeApp} = getProductPriceModel(product);

	const priceLabel = isFreeApp
		? i18n.translate('free')
		: productPurchaseCart.cart?.summary?.totalFormatted ||
			product.skus?.find((sku) => sku?.price?.priceFormatted)?.price
				?.priceFormatted ||
			i18n.translate('free');

	const {pathname} = useLocation();
	const navigate = useNavigate();

	const steps = routes.map((route) => {
		const key = route.index ? '/' : `/${route.path}`;

		return {
			active: pathname === key,
			key,
			title: route.title,
		};
	});

	const activeStepIndex = steps.findIndex(({active}) => active);

	const stepNavigate = (stepNumber: number) => {
		const step = steps[activeStepIndex + stepNumber];

		if (step) {
			navigate(step.key);
		}
	};

	const handlePurchase = async () => {
		setSubmitting(true);

		try {
			const productPurchase = new ProductPurchaseApp(
				selectedAccount,
				product
			);

			const order = await productPurchase.createOrder();

			navigate(await productPurchase.getNextStepsLink(order), {
				state: {account: selectedAccount},
			});
		}
		catch (error) {
			console.error(error);

			Liferay.Util.openToast({
				message: i18n.translate('an-unexpected-error-occurred'),
				type: 'danger',
			});
		}

		setSubmitting(false);
	};

	const context: ProductPurchaseOutletContext = {
		accounts,
		actions: {
			nextStep: () => stepNavigate(1),
			previousStep: () => stepNavigate(-1),
		},
		handlePurchase,
		isLoadingAccounts: isLoading,
		isSingleAccount: accounts.length === 1,
		isSubmitting,
		product,
		productPurchaseCart,
		selectedAccount,
		setSelectedAccount,
	};

	return (
		<>
			{isSubmitting && (
				<Loading.FullScreen>
					{i18n.translate(
						'hang-tight-your-purchase-is-being-processed'
					)}
				</Loading.FullScreen>
			)}

			<ProductPurchaseHeader
				product={product}
				rightNode={
					<div className="text-right">
						<small className="d-block text-muted">
							{i18n.translate('price')}
						</small>

						<span className="font-weight-semi-bold">
							{priceLabel}
						</span>
					</div>
				}
			>
				{pathname !== '/' && selectedAccount?.id && (
					<>
						<hr className="mx-n4 my-4" />

						<div className="align-items-center d-flex justify-content-between">
							<span className="font-weight-semi-bold text-muted">
								{i18n.translate('account-selected')}
							</span>

							<div className="align-items-center d-flex">
								<div className="mr-3 text-right">
									<strong className="d-block">
										{selectedAccount.name}
									</strong>

									<small className="text-muted">
										{Liferay.ThemeDisplay.getUserEmailAddress()}
									</small>
								</div>

								<AccountAvatar
									logoURL={selectedAccount.logoURL}
									type={selectedAccount.type}
								/>
							</div>
						</div>
					</>
				)}
			</ProductPurchaseHeader>

			<div className="bg-white border d-flex flex-column mt-4 p-5 rounded">
				<ProductPurchaseSteps className="mb-4" steps={steps} />

				<Outlet context={context} />
			</div>
		</>
	);
};

const useProductPurchaseOutletContext = () =>
	useOutletContext<ProductPurchaseOutletContext>();

export {useProductPurchaseOutletContext};

export default ProductPurchaseOutlet;
