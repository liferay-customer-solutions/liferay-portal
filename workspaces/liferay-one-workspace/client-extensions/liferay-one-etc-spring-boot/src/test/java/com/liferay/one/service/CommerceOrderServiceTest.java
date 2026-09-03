/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.commerce.admin.order.client.dto.v1_0.Order;
import com.liferay.one.constants.CommerceOrderConstants;
import com.liferay.one.util.KeyedLock;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Ricardo Mariz
 */
public class CommerceOrderServiceTest {

	@BeforeEach
	public void setUp() throws Exception {
		_commerceOrderService = Mockito.spy(new CommerceOrderService());

		ReflectionTestUtils.setField(
			_commerceOrderService, "_keyedLock", new KeyedLock());
		ReflectionTestUtils.setField(
			_commerceOrderService, "_settledPaymentRetryDelays", new long[0]);

		Mockito.doNothing(
		).when(
			_commerceOrderService
		).completeOrder(
			ArgumentMatchers.anyLong(), ArgumentMatchers.anyInt()
		);
	}

	@Test
	public void testCompleteSettledOrderCompletesPaidOrder() throws Exception {
		_whenFetchCommerceOrder(
			_createOrder(
				CommerceOrderConstants.ORDER_STATUS_PENDING, "DXP_APP",
				CommerceOrderConstants.ORDER_PAYMENT_STATUS_COMPLETED));

		_commerceOrderService.completeSettledOrder(_ORDER_ID);

		Mockito.verify(
			_commerceOrderService
		).completeOrder(
			_ORDER_ID, CommerceOrderConstants.ORDER_PAYMENT_STATUS_COMPLETED
		);
	}

	@Test
	public void testCompleteSettledOrderCompletesPaymentNotRequiredOrder()
		throws Exception {

		_whenFetchCommerceOrder(
			_createOrder(
				CommerceOrderConstants.ORDER_STATUS_PENDING, "CLOUD_APP",
				CommerceOrderConstants.ORDER_PAYMENT_STATUS_NOT_REQUIRED));

		_commerceOrderService.completeSettledOrder(_ORDER_ID);

		Mockito.verify(
			_commerceOrderService
		).completeOrder(
			_ORDER_ID, CommerceOrderConstants.ORDER_PAYMENT_STATUS_NOT_REQUIRED
		);
	}

	@Test
	public void testCompleteSettledOrderRetriesUntilPaymentSettles()
		throws Exception {

		ReflectionTestUtils.setField(
			_commerceOrderService, "_settledPaymentRetryDelays",
			new long[] {0, 0});

		Mockito.doReturn(
			_createOrder(
				CommerceOrderConstants.ORDER_STATUS_PENDING, "DXP_APP",
				_PAYMENT_STATUS_PENDING)
		).doReturn(
			_createOrder(
				CommerceOrderConstants.ORDER_STATUS_PENDING, "DXP_APP",
				CommerceOrderConstants.ORDER_PAYMENT_STATUS_COMPLETED)
		).when(
			_commerceOrderService
		).fetchCommerceOrder(
			_ORDER_ID
		);

		_commerceOrderService.completeSettledOrder(_ORDER_ID);

		Mockito.verify(
			_commerceOrderService
		).completeOrder(
			_ORDER_ID, CommerceOrderConstants.ORDER_PAYMENT_STATUS_COMPLETED
		);
	}

	@Test
	public void testCompleteSettledOrderSkipsCanceledOrder() throws Exception {
		_whenFetchCommerceOrder(
			_createOrder(
				CommerceOrderConstants.ORDER_STATUS_CANCELLED, "DXP_APP",
				CommerceOrderConstants.ORDER_PAYMENT_STATUS_COMPLETED));

		_commerceOrderService.completeSettledOrder(_ORDER_ID);

		_verifyNeverCompleted();
	}

	@Test
	public void testCompleteSettledOrderSkipsCompletedOrder() throws Exception {
		_whenFetchCommerceOrder(
			_createOrder(
				CommerceOrderConstants.ORDER_STATUS_COMPLETED, "DXP_APP",
				CommerceOrderConstants.ORDER_PAYMENT_STATUS_COMPLETED));

		_commerceOrderService.completeSettledOrder(_ORDER_ID);

		_verifyNeverCompleted();
	}

	@Test
	public void testCompleteSettledOrderSkipsMissingOrder() throws Exception {
		_whenFetchCommerceOrder(null);

		_commerceOrderService.completeSettledOrder(_ORDER_ID);

		_verifyNeverCompleted();
	}

	@Test
	public void testCompleteSettledOrderSkipsOrderCanceledDuringRetry()
		throws Exception {

		ReflectionTestUtils.setField(
			_commerceOrderService, "_settledPaymentRetryDelays",
			new long[] {0, 0});

		Mockito.doReturn(
			_createOrder(
				CommerceOrderConstants.ORDER_STATUS_PENDING, "DXP_APP",
				_PAYMENT_STATUS_PENDING)
		).doReturn(
			_createOrder(
				CommerceOrderConstants.ORDER_STATUS_CANCELLED, "DXP_APP",
				_PAYMENT_STATUS_PENDING)
		).when(
			_commerceOrderService
		).fetchCommerceOrder(
			_ORDER_ID
		);

		_commerceOrderService.completeSettledOrder(_ORDER_ID);

		_verifyNeverCompleted();
	}

	@Test
	public void testCompleteSettledOrderSkipsOrderWithoutOrderType()
		throws Exception {

		_whenFetchCommerceOrder(
			_createOrder(
				CommerceOrderConstants.ORDER_STATUS_PENDING, null,
				CommerceOrderConstants.ORDER_PAYMENT_STATUS_COMPLETED));

		_commerceOrderService.completeSettledOrder(_ORDER_ID);

		_verifyNeverCompleted();
	}

	@Test
	public void testCompleteSettledOrderSkipsPendingPayment() throws Exception {
		_whenFetchCommerceOrder(
			_createOrder(
				CommerceOrderConstants.ORDER_STATUS_PENDING, "DXP_APP",
				_PAYMENT_STATUS_PENDING));

		_commerceOrderService.completeSettledOrder(_ORDER_ID);

		_verifyNeverCompleted();
	}

	@Test
	public void testCompleteSettledOrderSkipsUnrelatedOrderType()
		throws Exception {

		_whenFetchCommerceOrder(
			_createOrder(
				CommerceOrderConstants.ORDER_STATUS_PENDING, "AI_HUB",
				CommerceOrderConstants.ORDER_PAYMENT_STATUS_COMPLETED));

		_commerceOrderService.completeSettledOrder(_ORDER_ID);

		_verifyNeverCompleted();
	}

	@Test
	public void testCompleteSettledOrdersSweepsEveryPendingSettledOrder()
		throws Exception {

		Order order1 = _createOrder(
			CommerceOrderConstants.ORDER_STATUS_PENDING, "DXP_APP",
			CommerceOrderConstants.ORDER_PAYMENT_STATUS_COMPLETED);

		order1.setId(_ORDER_ID);

		Order order2 = _createOrder(
			CommerceOrderConstants.ORDER_STATUS_PENDING, "CLOUD_APP",
			CommerceOrderConstants.ORDER_PAYMENT_STATUS_NOT_REQUIRED);

		order2.setId(_ORDER_ID + 1);

		Mockito.doReturn(
			List.of(order1, order2)
		).when(
			_commerceOrderService
		).getOrders(
			ArgumentMatchers.anyString()
		);

		Mockito.doNothing(
		).when(
			_commerceOrderService
		).completeSettledOrder(
			ArgumentMatchers.anyLong()
		);

		_commerceOrderService.completeSettledOrders();

		Mockito.verify(
			_commerceOrderService
		).completeSettledOrder(
			_ORDER_ID
		);

		Mockito.verify(
			_commerceOrderService
		).completeSettledOrder(
			_ORDER_ID + 1
		);
	}

	@Test
	public void testOnApplicationReadyCompletesSettledOrders()
		throws Exception {

		Mockito.doThrow(
			new Exception()
		).when(
			_commerceOrderService
		).completeSettledOrders();

		_commerceOrderService.onApplicationReady();

		Mockito.verify(
			_commerceOrderService
		).completeSettledOrders();
	}

	private Order _createOrder(
		int orderStatus, String orderTypeExternalReferenceCode,
		int paymentStatus) {

		Order order = new Order();

		order.setOrderStatus(orderStatus);
		order.setOrderTypeExternalReferenceCode(orderTypeExternalReferenceCode);
		order.setPaymentStatus(paymentStatus);

		return order;
	}

	private void _verifyNeverCompleted() throws Exception {
		Mockito.verify(
			_commerceOrderService, Mockito.never()
		).completeOrder(
			ArgumentMatchers.anyLong(), ArgumentMatchers.anyInt()
		);
	}

	private void _whenFetchCommerceOrder(Order order) throws Exception {
		Mockito.doReturn(
			order
		).when(
			_commerceOrderService
		).fetchCommerceOrder(
			_ORDER_ID
		);
	}

	private static final long _ORDER_ID = 1000L;

	private static final int _PAYMENT_STATUS_PENDING = 1;

	private CommerceOrderService _commerceOrderService;

}