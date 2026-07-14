/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {createStore} from '@xstate/store';

import HeadlessCommerceDeliveryCart from '~/services/headless/HeadlessCommerceDeliveryCart';
import type {Cart, CartItem} from '~/types/orders';

type CartContext = {
	cart: Cart;
	cartItems: CartItem[];
};

const context: CartContext = {
	cart: {} as Cart,
	cartItems: [] as CartItem[],
};

export const cartStore = createStore({
	context,
	on: {
		reset: () => context,

		setCart: (context: CartContext, event: {cart: Cart}) => {
			return {
				...context,
				cart: event.cart,
			};
		},

		setCartItems: (
			context: CartContext,
			event: {cartItems: CartItem[]},
			{emit}: any
		) => {
			emit({
				cartId: context.cart.id,
				cartItems: event.cartItems,
				type: 'update:cart-items',
			});

			return {
				...context,
				cartItems: event.cartItems,
			};
		},
	},
	types: {
		emitted: {} as {
			cartId: number;
			cartItems: CartItem[];
			type: 'update:cart-items';
		},
	},
});

cartStore.on('update:cart-items', async ({cartId, cartItems}: any) => {
	const cart = await HeadlessCommerceDeliveryCart.updateCart(cartId, {
		cartItems,
	});

	cartStore.send({cart, type: 'setCart'});
});
