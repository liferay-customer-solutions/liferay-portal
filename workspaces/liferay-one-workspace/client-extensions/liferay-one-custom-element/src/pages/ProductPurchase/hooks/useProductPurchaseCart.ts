/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useSelector} from '@xstate/store/react';
import {useCallback, useEffect} from 'react';
import HeadlessCommerceDeliveryCart from '~/services/headless/HeadlessCommerceDeliveryCart';
import {Liferay} from '~/services/liferay/liferay';
import {cartStore} from '../store';

import type {Cart, CartItem} from '~/types/orders';
import type {DeliveryProduct} from '~/types/product';

const useProductPurchaseCart = (
	accountId?: number,
	product?: DeliveryProduct,
	orderTypeExternalReferenceCode?: string
) => {
	const channelId = Liferay.CommerceContext.commerceChannelId;

	const {cart, cartItems} = useSelector(cartStore, (state) => state.context);

	const cartId = cart?.id;

	const setCart = useCallback(
		(cart: Cart) => cartStore.send({cart, type: 'setCart'}),
		[]
	);

	const setCartItems = useCallback(
		(cartItems: CartItem[]) =>
			cartStore.send({cartItems, type: 'setCartItems'}),
		[]
	);

	const addCart = async (productId: number, skuId: number) => {
		let currentCart = cart;

		if (!cartId) {
			currentCart = await HeadlessCommerceDeliveryCart.createCart(
				channelId,
				{
					accountId,
					currencyCode: Liferay.CommerceContext.currency.currencyCode,
					orderTypeExternalReferenceCode,
				}
			);

			setCart(currentCart);
		}

		const existingItem = cartItems.find((item) => item.skuId === skuId);

		const newCartItems = existingItem
			? cartItems.map((item) =>
					item.skuId === skuId
						? {...item, quantity: item.quantity + 1}
						: item
				)
			: [...cartItems, {productId, quantity: 1, skuId} as CartItem];

		setCartItems(newCartItems);

		return {
			...currentCart,
			cartItems: newCartItems,
		};
	};

	const removeFromCart = async (skuId: number) => {
		const newCartItems = cartItems
			.map((item) =>
				item.skuId === skuId
					? {...item, quantity: item.quantity - 1}
					: item
			)
			.filter((item) => item.quantity > 0);

		setCartItems(newCartItems);
	};

	const removeCart = useCallback(
		(id: number) =>
			HeadlessCommerceDeliveryCart.deleteCart(id)
				.then(() => {
					setCart({} as Cart);
					setCartItems([]);
				})
				.catch(console.error),
		[]
	);

	useEffect(() => {
		(async () => {
			if (!accountId || !product) {
				return;
			}

			const {items: carts} =
				await HeadlessCommerceDeliveryCart.getAccountCarts(
					accountId,
					channelId
				);

			const openCart = carts?.find(
				(cart) =>
					cart.orderTypeExternalReferenceCode ===
						orderTypeExternalReferenceCode &&
					(!cart.author ||
						cart.author === Liferay.ThemeDisplay.getUserName())
			);

			if (openCart?.orderStatusInfo?.label !== 'open') {
				return;
			}

			const {items: openCartItems} =
				await HeadlessCommerceDeliveryCart.getCartItems(openCart.id);

			const hasOtherProduct = openCartItems.some(
				(cartItem) =>
					cartItem.productId !== (product.productId ?? product.id)
			);

			if (hasOtherProduct) {
				return removeCart(openCart.id);
			}

			setCart(openCart);
			setCartItems(openCartItems);
		})();
	}, [
		accountId,
		channelId,
		orderTypeExternalReferenceCode,
		product,
		removeCart,
		setCart,
		setCartItems,
	]);

	return {
		addCart,
		cart,
		cartItems,
		removeCart,
		removeFromCart,
		setCart,
		updateCart: HeadlessCommerceDeliveryCart.updateCart.bind(
			HeadlessCommerceDeliveryCart
		),
	};
};

export default useProductPurchaseCart;
