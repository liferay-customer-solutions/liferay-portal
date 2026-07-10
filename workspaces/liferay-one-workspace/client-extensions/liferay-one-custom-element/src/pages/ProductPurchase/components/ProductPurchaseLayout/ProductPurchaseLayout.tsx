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
import AccountAvatar from '~/components/AccountAvatar/AccountAvatar';
import Loading from '~/components/Loading/Loading';
import i18n from '~/i18n';
import ProductPurchaseApp from '~/services/commerce/ProductPurchaseApp';
import ProductPurchaseLDP, {
	LDPSettings,
} from '~/services/commerce/ProductPurchaseLDP';
import HeadlessCommerceDeliveryCart from '~/services/headless/HeadlessCommerceDeliveryCart';
import {Liferay} from '~/services/liferay/liferay';
import {getProductPriceModel, isLDPProduct} from '~/utils/productUtils';

import useAccounts from '../../hooks/useAccounts';
import useProductPurchaseCart from '../../hooks/useProductPurchaseCart';
import {ProductPurchaseStepItem} from '../../productPurchaseRoutes';
import {PaymentMethodType, ProductPurchasePayment} from '../../types';
import ProductPurchaseHeader from '../ProductPurchaseHeader/ProductPurchaseHeader';
import ProductPurchaseSteps from '../ProductPurchaseSteps/ProductPurchaseSteps';

import type {Account} from '~/types/accounts';
import type {BillingAddress} from '~/types/orders';
import type {DeliveryProduct} from '~/types/product';

type ProductPurchaseLayoutProps = {
	product: DeliveryProduct;
	steps: ProductPurchaseStepItem[];
};

export type ProductPurchaseLayoutContext = {
	accounts: Account[];
	actions: {
		nextStep: () => void;
		previousStep: () => void;
	};
	handlePurchase: () => Promise<void>;
	isLoadingAccounts: boolean;
	isSingleAccount: boolean;
	isSubmitting: boolean;
	payment: ProductPurchasePayment;
	product: DeliveryProduct;
	productPurchaseCart: ReturnType<typeof useProductPurchaseCart>;
	selectedAccount: Account;
	setLDPSettings: React.Dispatch<React.SetStateAction<LDPSettings | null>>;
	setPayment: React.Dispatch<React.SetStateAction<ProductPurchasePayment>>;
	setSelectedAccount: React.Dispatch<React.SetStateAction<Account>>;
};

const ProductPurchaseLayout = ({
	product,
	steps: stepItems,
}: ProductPurchaseLayoutProps) => {
	const [isSubmitting, setSubmitting] = useState(false);
	const [ldpSettings, setLDPSettings] = useState<LDPSettings | null>(null);
	const [payment, setPayment] = useState<ProductPurchasePayment>({
		billingAddress: {} as BillingAddress,
		invoice: {email: '', purchaseOrderNumber: ''},
		taxId: '',
		type: PaymentMethodType.PAY_NOW,
	});

	const {accounts, isLoading, selectedAccount, setSelectedAccount} =
		useAccounts();

	const productPurchaseCart = useProductPurchaseCart(
		selectedAccount?.id,
		product,
		ProductPurchaseApp.getOrderTypeExternalReferenceCode(product)
	);

	const {isFreeApp, isPaidApp} = getProductPriceModel(product);

	const priceLabel = isFreeApp
		? i18n.translate('free')
		: productPurchaseCart.cart?.summary?.totalFormatted ||
			product.skus?.find((sku) => sku?.price?.priceFormatted)?.price
				?.priceFormatted ||
			i18n.translate('free');

	const {pathname} = useLocation();
	const navigate = useNavigate();

	const steps = stepItems.map((stepItem) => ({
		active: pathname === stepItem.key,
		key: stepItem.key,
		title: stepItem.title,
	}));

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
			const productPurchase =
				isLDPProduct(product) && ldpSettings
					? new ProductPurchaseLDP(
							selectedAccount,
							product,
							ldpSettings
						)
					: new ProductPurchaseApp(selectedAccount, product);

			if (isPaidApp) {
				const cart = await productPurchase.createOrder({
					...productPurchaseCart.cart,
					billingAddress: payment.billingAddress,
					cartItems: productPurchaseCart.cartItems,
					paymentMethod:
						payment.type === PaymentMethodType.PAY_NOW
							? 'paypal-integration'
							: 'money-order',
					shippingAddress: payment.billingAddress,
				});

				if (payment.type === PaymentMethodType.PAY_NOW) {
					window.location.href =
						await HeadlessCommerceDeliveryCart.getPaymentMethodURL(
							cart.id,
							`${
								window.location.href.split('#')[0]
							}#/purchase-completed?orderId=${cart.id}`
						);

					return;
				}

				navigate(`/bank-transfer-completed?orderId=${cart.id}`, {
					state: {account: selectedAccount},
				});

				return;
			}

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

	const context: ProductPurchaseLayoutContext = {
		accounts,
		actions: {
			nextStep: () => stepNavigate(1),
			previousStep: () => stepNavigate(-1),
		},
		handlePurchase,
		isLoadingAccounts: isLoading,
		isSingleAccount: accounts.length === 1,
		isSubmitting,
		payment,
		product,
		productPurchaseCart,
		selectedAccount,
		setLDPSettings,
		setPayment,
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

const useProductPurchaseLayoutContext = () =>
	useOutletContext<ProductPurchaseLayoutContext>();

export {useProductPurchaseLayoutContext};

export default ProductPurchaseLayout;
