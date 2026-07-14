/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayIcon from '@clayui/icon';
import {useEffect, useMemo, useState} from 'react';

import ProductPurchase from '~/components/ProductPurchase';
import RadioCardList from '~/components/RadioCardList/RadioCardList';
import SearchBuilder from '~/utils/SearchBuilder';
import type {CartItem, OrderTypes} from '~/types/orders';
import type {DeliverySKU} from '~/types/product';
import i18n from '~/i18n';
import {Liferay} from '~/services/liferay/liferay';
import HeadlessCommerceDeliveryCart from '~/services/headless/HeadlessCommerceDeliveryCart';
import HeadlessCommerceDeliveryOrder from '~/services/headless/HeadlessCommerceDeliveryOrder';
import {getAiHubTokenSKUs} from '~/utils/productUtils';
import {getSiteURL} from '~/utils/siteUtils';
import {useProductPurchaseLayoutContext as useProductPurchaseOutletContext} from '~/pages/ProductPurchase/components/ProductPurchaseLayout/ProductPurchaseLayout';
import {cartStore, productPurchaseStore} from '../../../store';

import './AIHubForm.scss';

import '../index.scss';

const AIHubTokenSelection = () => {
	const {
		actions: {nextStep},
		product,
		productPurchaseCart,
		selectedAccount,
	} = useProductPurchaseOutletContext();

	useEffect(() => {
		productPurchaseStore.send({
			licenseType: 'PAID',
			type: 'setLicenseType',
		});
	}, []);

	const aiHubTokens = useMemo(() => getAiHubTokenSKUs(product), [product]);

	const [isCartLoading, setIsCartLoading] = useState(true);
	const [selectedSkuId, setSelectedSkuId] = useState<number | undefined>();

	const searchParams = useMemo(
		() => new URLSearchParams(window.location.search),
		[]
	);
	const [orderId, setOrderId] = useState<string | null>(
		searchParams.get('orderId')
	);

	const onClickCancel = () => {
		if (productPurchaseCart.cart.id) {
			productPurchaseCart.removeCart(productPurchaseCart.cart.id);
		}

		if (orderId) {
			return Liferay.Util.navigate(
				`${getSiteURL()}/customer-dashboard#/products/${orderId}`
			);
		}

		Liferay.Util.navigate(`${getSiteURL()}/customer-dashboard`);
	};

	useEffect(() => {
		if (orderId || !selectedAccount?.id) {
			return;
		}

		HeadlessCommerceDeliveryOrder.getPlacedOrders(
			Liferay.CommerceContext.commerceChannelId,
			selectedAccount.id,
			new URLSearchParams({
				filter: SearchBuilder.eq(
					'orderTypeExternalReferenceCode',
					'AI_HUB'
				),
				pageSize: '1',
			})
		)
			.then(({items}) => {
				if (items.length) {
					setOrderId(String(items[0].id));
				}
			})
			.catch(console.error);
	}, [orderId, selectedAccount?.id]);

	useEffect(() => {
		if (!selectedAccount?.id) {
			return;
		}

		setSelectedSkuId(undefined);

		cartStore.send({type: 'reset'});

		const getOrdersItems = async () => {
			try {
				const response =
					await HeadlessCommerceDeliveryCart.getAccountCarts(
						selectedAccount.id,
						Liferay.CommerceContext.commerceChannelId
					);

				const cart = response.items.find(
					(item) =>
						item.orderTypeExternalReferenceCode ===
							'AI_HUB_TOKEN' &&
						item.orderStatusInfo?.label === 'open' &&
						(!item.author ||
							item.author === Liferay.ThemeDisplay.getUserName())
				);

				if (cart) {
					cartStore.send({cart, type: 'setCart'});
					cartStore.send({
						cartItems: cart.cartItems,
						type: 'setCartItems',
					});
				}
			}
			catch (error) {
				console.error(error);
			}
			finally {
				setIsCartLoading(false);
			}
		};

		getOrdersItems();
	}, [selectedAccount?.id]);

	const [hasCleanedCart, setHasCleanedCart] = useState(false);

	useEffect(() => {
		if (isCartLoading) {
			return;
		}

		if (!productPurchaseCart.cartItems.length) {
			return;
		}

		const cartSkuIds = productPurchaseCart.cartItems.map(
			(item) => item.skuId
		);

		const tokensInCart = aiHubTokens.filter((token: DeliverySKU) =>
			cartSkuIds.includes(token.id)
		);

		if (tokensInCart.length) {
			const activeToken = tokensInCart[0];

			setSelectedSkuId(activeToken.id);

			if (tokensInCart.length > 1 && !hasCleanedCart) {
				const tokenSkuIds = aiHubTokens.map((token: any) => token.id);
				const filteredItems = productPurchaseCart.cartItems.filter(
					(item) =>
						!tokenSkuIds.includes(item.skuId) ||
						item.skuId === activeToken.id
				);

				cartStore.send({
					cartItems: filteredItems,
					type: 'setCartItems',
				});

				setHasCleanedCart(true);
			}
		}
	}, [
		aiHubTokens,
		isCartLoading,
		product.id,
		productPurchaseCart,
		selectedSkuId,
		hasCleanedCart,
	]);

	const handleSelectToken = async (token: any) => {
		const tokenValue = token?.value;
		const newSkuId = tokenValue?.id;

		if (!newSkuId || selectedSkuId === newSkuId) {
			return;
		}

		let activeCart = productPurchaseCart.cart;

		if (!activeCart.id) {
			const createdCart = await productPurchaseCart.addCart(
				product.productId ?? product.id,
				newSkuId
			);

			if (createdCart) {
				activeCart = createdCart as any;

				cartStore.send({cart: createdCart as any, type: 'setCart'});
			}
		}

		const tokenSkuIds = aiHubTokens.map((token: any) => token.id);
		const filteredItems = (
			activeCart.cartItems || productPurchaseCart.cartItems
		).filter((item) => !tokenSkuIds.includes(item.skuId));

		const newCartItems = [
			...filteredItems,
			{
				productId: product.productId ?? product.id,
				quantity: 1,
				skuId: newSkuId,
			} as CartItem,
		];

		cartStore.send({cartItems: newCartItems, type: 'setCartItems'});

		setSelectedSkuId(newSkuId);
	};

	const onSubmit = () => {
		nextStep();
	};

	return (
		<ProductPurchase.Shell
			className="liferay-ai-hub-form"
			footerProps={{
				backButtonProps: {className: 'd-none'},
				cancelButtonProps: {
					onClick: onClickCancel,
				},
				continueButtonProps: {
					children: i18n.translate('continue'),
					disabled: !selectedSkuId,
					onClick: onSubmit,
				},
			}}
			title={i18n.translate('select-desired-amount-of-tokens')}
		>
			<p className="mb-6 text-black-50">
				{i18n.translate(
					'pick-one-of-the-following-three-options-to-immediately-obtain-extra-tokens-to-foster-your-ai-hub-capabilities'
				)}
			</p>

			<div>
				{aiHubTokens.length ? (
					<RadioCardList
						contentList={aiHubTokens.map((token: any) => ({
							...token,
							imageURL: token.customFields?.find(
								(field: any) => field.name === 'icon-url'
							)?.customValue.data,
							selected: selectedSkuId === token?.id,
							title: (
								<div className="align-items-center d-flex justify-content-between pt-2">
									<div>
										<p className="liferay-ai-hub-form-token-name mb-1">
											{
												token.skuOptions[0]
													.skuOptionValueNames[0]
											}
										</p>

										<p className="liferay-ai-hub-form-token-description mb-0 text-black-50">
											{
												token.customFields?.find(
													(field: any) =>
														field.name ===
														'description'
												)?.customValue.data
											}
										</p>
									</div>

									<p className="liferay-ai-hub-form-token-price mb-0">
										{token.price.priceFormatted}
									</p>
								</div>
							),
							value: token,
						}))}
						leftRadio
						onSelect={handleSelectToken}
						showImage
					/>
				) : (
					<p className="font-weight-bold my-5">No tokens available</p>
				)}

				<ClayIcon
					className="liferay-ai-hub-form-info mr-1 text-black-50"
					symbol="lock"
				/>

				<span className="liferay-ai-hub-form-info text-black-50">
					The per-token price is locked when you purchase. Future rate
					changes won&apos;t affect tokens you&apos;ve already bought.
				</span>
			</div>
		</ProductPurchase.Shell>
	);
};

export default AIHubTokenSelection;
