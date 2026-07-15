/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {
	createContext,
	useCallback,
	useContext,
	useMemo,
	useReducer,
	useRef,
} from 'react';
import HeadlessCommerceDeliveryCart from '~/services/headless/HeadlessCommerceDeliveryCart';

import type {Cart, CartItem} from '~/types/orders';

type CartState = {
	cart: Cart;
	cartItems: CartItem[];
};

type CartAction =
	| {type: 'reset'}
	| {cart: Cart; type: 'setCart'}
	| {cartItems: CartItem[]; type: 'setCartItems'};

const initialState: CartState = {
	cart: {} as Cart,
	cartItems: [] as CartItem[],
};

function cartReducer(state: CartState, action: CartAction): CartState {
	switch (action.type) {
		case 'reset':
			return initialState;
		case 'setCart':
			return {...state, cart: action.cart};
		case 'setCartItems':
			return {...state, cartItems: action.cartItems};
		default:
			return state;
	}
}

type CartContextValue = CartState & {
	reset: () => void;
	setCart: (cart: Cart) => void;
	setCartItems: (cartItems: CartItem[]) => void;
};

const CartContext = createContext<CartContextValue | null>(null);

export function CartProvider({children}: {children: React.ReactNode}) {
	const [state, dispatch] = useReducer(cartReducer, initialState);

	const cartIdRef = useRef<number>(0);

	const reset = useCallback(() => {
		cartIdRef.current = 0;
		dispatch({type: 'reset'});
	}, []);

	const setCart = useCallback((cart: Cart) => {
		cartIdRef.current = cart.id;
		dispatch({cart, type: 'setCart'});
	}, []);

	const setCartItems = useCallback((cartItems: CartItem[]) => {
		dispatch({cartItems, type: 'setCartItems'});

		HeadlessCommerceDeliveryCart.updateCart(cartIdRef.current, {cartItems})
			.then((cart) => dispatch({cart, type: 'setCart'}))
			.catch((error) => console.error('Unable to update cart', error));
	}, []);

	const value = useMemo<CartContextValue>(
		() => ({...state, reset, setCart, setCartItems}),
		[reset, setCart, setCartItems, state]
	);

	return (
		<CartContext.Provider value={value}>{children}</CartContext.Provider>
	);
}

export function useCartContext() {
	const context = useContext(CartContext);

	if (!context) {
		throw new Error('useCartContext must be used within CartProvider');
	}

	return context;
}
