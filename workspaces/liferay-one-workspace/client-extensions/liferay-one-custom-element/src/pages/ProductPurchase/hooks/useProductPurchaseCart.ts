/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useCallback, useEffect, useState} from 'react';

import {Liferay} from '../../../liferay/liferay';
import HeadlessCommerceDeliveryCart from '../../../services/rest/HeadlessCommerceDeliveryCart';

const useProductPurchaseCart = (
	accountId?: number,
	product?: DeliveryProduct,
	orderTypeExternalReferenceCode?: string
) => {
	const channelId = Liferay.CommerceContext.commerceChannelId;

	const [cart, setCart] = useState<Cart>({} as Cart);
	const [cartItems, setCartItems] = useState<CartItem[]>([]);

	const cartId = cart?.id;

	const syncCartItems = useCallback(
		async (id: number, items: CartItem[]) => {
			const updatedCart = await HeadlessCommerceDeliveryCart.updateCart(
				id,
				{cartItems: items}
			);

			setCart(updatedCart);
		},
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

		await syncCartItems(currentCart.id, newCartItems);
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

		if (cartId) {
			await syncCartItems(cartId, newCartItems);
		}
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

			const [openCart] = carts ?? [];

			if (openCart?.orderStatusInfo?.label !== 'open') {
				return;
			}

			const {items: openCartItems} =
				await HeadlessCommerceDeliveryCart.getCartItems(openCart.id);

			const hasProduct = openCartItems.some(
				(cartItem) => cartItem.productId === product.productId
			);

			if (!hasProduct) {
				return removeCart(openCart.id);
			}

			setCart(openCart);
			setCartItems(openCartItems);
		})();
	}, [accountId, channelId, product, removeCart]);

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
