/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.commerce.admin.order.client.dto.v1_0.Order;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.OrderItem;
import com.liferay.one.salesforce.model.SalesforceModelTestUtil;
import com.liferay.one.salesforce.model.SalesforceOpportunityLineItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Felipe Franca
 */
public class ProvisioningOrderServiceTest {

	@BeforeEach
	public void setUp() {
		_provisioningOrderService = new ProvisioningOrderService();

		_commerceOrderItemService = Mockito.mock(
			CommerceOrderItemService.class);
		_commerceOrderService = Mockito.mock(CommerceOrderService.class);
		_entitlementService = Mockito.mock(EntitlementService.class);

		ReflectionTestUtils.setField(
			_provisioningOrderService, "_commerceOrderItemService",
			_commerceOrderItemService);
		ReflectionTestUtils.setField(
			_provisioningOrderService, "_commerceOrderService",
			_commerceOrderService);
		ReflectionTestUtils.setField(
			_provisioningOrderService, "_entitlementService",
			_entitlementService);
	}

	@Test
	public void testTrimRealignedOrderItemsExcludesTheCurrentOrder()
		throws Exception {

		OrderItem orderItem = SalesforceModelTestUtil.createOrderItem(
			"Approved", null, "2025-01-01T00:00:00Z", "ORDER-ITEM-1", 5001L,
			_PRODUCT_2_ID, "2024-01-01T00:00:00Z");

		Order currentOrder = new Order();

		currentOrder.setCustomFields(
			Map.of("parentOpportunityId", _PARENT_OPPORTUNITY_ID));
		currentOrder.setExternalReferenceCode(_OPPORTUNITY_ID);
		currentOrder.setOrderItems(new OrderItem[] {orderItem});

		Mockito.when(
			_commerceOrderService.getAccountOrders(_ACCOUNT_ID)
		).thenReturn(
			List.of(currentOrder)
		);

		List<String> warningMessages = new ArrayList<>();

		_provisioningOrderService.trimRealignedOrderItems(
			_ACCOUNT_ID, _OPPORTUNITY_ID, _PARENT_OPPORTUNITY_ID,
			List.of(_createRealignmentLineItem("2026-01-01")), warningMessages);

		Mockito.verifyNoInteractions(_commerceOrderItemService);
		Mockito.verifyNoInteractions(_entitlementService);
		Assertions.assertEquals(1, warningMessages.size());
		Assertions.assertTrue(
			warningMessages.get(
				0
			).contains(
				"Unable to find an order item for amended line"
			));
	}

	@Test
	public void testTrimRealignedOrderItemsMatchesFamilyOrderViaCustomFields()
		throws Exception {

		OrderItem orderItem = SalesforceModelTestUtil.createOrderItem(
			"Approved", null, "2025-01-01T00:00:00Z", "ORDER-ITEM-1", 5001L,
			_PRODUCT_2_ID, "2024-01-01T00:00:00Z");

		Order order = new Order();

		order.setCustomFields(
			Map.of("parentOpportunityId", _PARENT_OPPORTUNITY_ID));
		order.setExternalReferenceCode("OPP-FAMILY-CHILD");
		order.setOrderItems(new OrderItem[] {orderItem});

		Mockito.when(
			_commerceOrderService.getAccountOrders(_ACCOUNT_ID)
		).thenReturn(
			List.of(order)
		);

		List<String> warningMessages = new ArrayList<>();

		_provisioningOrderService.trimRealignedOrderItems(
			_ACCOUNT_ID, _OPPORTUNITY_ID, _PARENT_OPPORTUNITY_ID,
			List.of(_createRealignmentLineItem("2026-01-01")), warningMessages);

		Mockito.verify(
			_commerceOrderItemService
		).patchOrderItemCustomFields(
			5001L, Map.of("effectiveEndDate", "2026-01-01T00:00:00Z")
		);

		Mockito.verify(
			_entitlementService
		).trimEntitlements(
			5001L, "2026-01-01T00:00:00Z"
		);
	}

	@Test
	public void testTrimRealignedOrderItemsPatchesApprovedOrderItem()
		throws Exception {

		OrderItem orderItem = SalesforceModelTestUtil.createOrderItem(
			"Approved", null, "2025-01-01T00:00:00Z", "ORDER-ITEM-1", 5001L,
			_PRODUCT_2_ID, "2024-01-01T00:00:00Z");

		_stubFamilyOrder(orderItem);

		List<String> warningMessages = new ArrayList<>();

		_provisioningOrderService.trimRealignedOrderItems(
			_ACCOUNT_ID, _OPPORTUNITY_ID, _PARENT_OPPORTUNITY_ID,
			List.of(_createRealignmentLineItem("2026-01-01")), warningMessages);

		Mockito.verify(
			_commerceOrderItemService
		).patchOrderItemCustomFields(
			5001L, Map.of("effectiveEndDate", "2026-01-01T00:00:00Z")
		);

		Mockito.verify(
			_entitlementService
		).trimEntitlements(
			5001L, "2026-01-01T00:00:00Z"
		);

		boolean hasMismatchWarning = false;

		for (String warningMessage : warningMessages) {
			if (warningMessage.contains("End date mismatch")) {
				hasMismatchWarning = true;
			}
		}

		Assertions.assertTrue(hasMismatchWarning);
	}

	@Test
	public void testTrimRealignedOrderItemsPatchesWhenExistingEffectiveEndDateIsAfterAmendedDate()
		throws Exception {

		OrderItem orderItem = SalesforceModelTestUtil.createOrderItem(
			"Approved", "2027-01-01T00:00:00Z", "2025-01-01T00:00:00Z",
			"ORDER-ITEM-1", 5001L, _PRODUCT_2_ID, "2024-01-01T00:00:00Z");

		_stubFamilyOrder(orderItem);

		List<String> warningMessages = new ArrayList<>();

		_provisioningOrderService.trimRealignedOrderItems(
			_ACCOUNT_ID, _OPPORTUNITY_ID, _PARENT_OPPORTUNITY_ID,
			List.of(_createRealignmentLineItem("2026-01-01")), warningMessages);

		Mockito.verify(
			_commerceOrderItemService
		).patchOrderItemCustomFields(
			5001L, Map.of("effectiveEndDate", "2026-01-01T00:00:00Z")
		);

		Mockito.verify(
			_entitlementService
		).trimEntitlements(
			5001L, "2026-01-01T00:00:00Z"
		);
	}

	@Test
	public void testTrimRealignedOrderItemsSkipsMatchingDates()
		throws Exception {

		OrderItem orderItem = SalesforceModelTestUtil.createOrderItem(
			"Approved", null, "2025-01-01T00:00:00Z", "ORDER-ITEM-1", 5001L,
			_PRODUCT_2_ID, "2024-06-01T00:00:00Z");

		_stubFamilyOrder(orderItem);

		List<String> warningMessages = new ArrayList<>();

		_provisioningOrderService.trimRealignedOrderItems(
			_ACCOUNT_ID, _OPPORTUNITY_ID, _PARENT_OPPORTUNITY_ID,
			List.of(_createRealignmentLineItem("2025-01-01")), warningMessages);

		Mockito.verifyNoInteractions(_commerceOrderItemService);
		Mockito.verifyNoInteractions(_entitlementService);
		Assertions.assertEquals(0, warningMessages.size());
	}

	@Test
	public void testTrimRealignedOrderItemsSkipsOrderOutsideFamily()
		throws Exception {

		OrderItem orderItem = SalesforceModelTestUtil.createOrderItem(
			"Approved", null, "2025-01-01T00:00:00Z", "ORDER-ITEM-1", 5001L,
			_PRODUCT_2_ID, "2024-01-01T00:00:00Z");

		Order otherOrder = new Order();

		otherOrder.setCustomFields(
			Map.of("parentOpportunityId", "OPP-UNRELATED"));
		otherOrder.setExternalReferenceCode("OPP-OTHER");
		otherOrder.setOrderItems(new OrderItem[] {orderItem});

		Mockito.when(
			_commerceOrderService.getAccountOrders(_ACCOUNT_ID)
		).thenReturn(
			List.of(otherOrder)
		);

		List<String> warningMessages = new ArrayList<>();

		_provisioningOrderService.trimRealignedOrderItems(
			_ACCOUNT_ID, _OPPORTUNITY_ID, _PARENT_OPPORTUNITY_ID,
			List.of(_createRealignmentLineItem("2026-01-01")), warningMessages);

		Mockito.verifyNoInteractions(_commerceOrderItemService);
		Mockito.verifyNoInteractions(_entitlementService);
		Assertions.assertEquals(1, warningMessages.size());
		Assertions.assertTrue(
			warningMessages.get(
				0
			).contains(
				"Unable to find an order item for amended line"
			));
	}

	@Test
	public void testTrimRealignedOrderItemsSkipsPatchWhenExistingEffectiveEndDateIsNotAfterAmendedDate()
		throws Exception {

		OrderItem orderItem = SalesforceModelTestUtil.createOrderItem(
			"Approved", "2025-06-01T00:00:00Z", "2025-01-01T00:00:00Z",
			"ORDER-ITEM-1", 5001L, _PRODUCT_2_ID, "2024-01-01T00:00:00Z");

		_stubFamilyOrder(orderItem);

		List<String> warningMessages = new ArrayList<>();

		_provisioningOrderService.trimRealignedOrderItems(
			_ACCOUNT_ID, _OPPORTUNITY_ID, _PARENT_OPPORTUNITY_ID,
			List.of(_createRealignmentLineItem("2026-01-01")), warningMessages);

		Mockito.verifyNoInteractions(_commerceOrderItemService);

		Mockito.verify(
			_entitlementService
		).trimEntitlements(
			5001L, "2026-01-01T00:00:00Z"
		);

		Assertions.assertEquals(0, warningMessages.size());
	}

	@Test
	public void testTrimRealignedOrderItemsSkipsUnapprovedOrderItem()
		throws Exception {

		OrderItem orderItem = SalesforceModelTestUtil.createOrderItem(
			"Pending", null, "2025-01-01T00:00:00Z", "ORDER-ITEM-1", 5001L,
			_PRODUCT_2_ID, "2024-01-01T00:00:00Z");

		_stubFamilyOrder(orderItem);

		List<String> warningMessages = new ArrayList<>();

		_provisioningOrderService.trimRealignedOrderItems(
			_ACCOUNT_ID, _OPPORTUNITY_ID, _PARENT_OPPORTUNITY_ID,
			List.of(_createRealignmentLineItem("2026-01-01")), warningMessages);

		Mockito.verifyNoInteractions(_commerceOrderItemService);
		Mockito.verifyNoInteractions(_entitlementService);
		Assertions.assertEquals(0, warningMessages.size());
	}

	@Test
	public void testTrimRealignedOrderItemsWarnsWhenNoOrderItemMatches()
		throws Exception {

		OrderItem orderItem = SalesforceModelTestUtil.createOrderItem(
			"Approved", null, "2025-01-01T00:00:00Z", "ORDER-ITEM-1", 5001L,
			"OTHER-SKU", "2024-01-01T00:00:00Z");

		_stubFamilyOrder(orderItem);

		List<String> warningMessages = new ArrayList<>();

		_provisioningOrderService.trimRealignedOrderItems(
			_ACCOUNT_ID, _OPPORTUNITY_ID, _PARENT_OPPORTUNITY_ID,
			List.of(_createRealignmentLineItem("2026-01-01")), warningMessages);

		Mockito.verifyNoInteractions(_commerceOrderItemService);
		Mockito.verifyNoInteractions(_entitlementService);
		Assertions.assertEquals(1, warningMessages.size());
		Assertions.assertTrue(
			warningMessages.get(
				0
			).contains(
				"Unable to find an order item for amended line"
			));
	}

	@Test
	public void testTrimRenewedOrderItemsPatchesEffectiveEndDate()
		throws Exception {

		OrderItem orderItem = SalesforceModelTestUtil.createOrderItem(
			"Approved", "2025-06-01T00:00:00Z", "2025-01-01T00:00:00Z",
			"ORDER-ITEM-1", 5001L, _PRODUCT_2_ID, null);

		_stubFamilyOrder(orderItem);

		List<String> warningMessages = new ArrayList<>();

		_provisioningOrderService.trimRenewedOrderItems(
			_ACCOUNT_ID, _OPPORTUNITY_ID,
			List.of(_createRenewalLineItem("2025-02-01")), warningMessages);

		Mockito.verify(
			_commerceOrderItemService
		).patchOrderItemCustomFields(
			5001L, Map.of("effectiveEndDate", "2025-02-01T00:00:00Z")
		);

		Assertions.assertEquals(0, warningMessages.size());
	}

	@Test
	public void testTrimRenewedOrderItemsSkipsWhenEffectiveEndDateIsAbsent()
		throws Exception {

		OrderItem orderItem = SalesforceModelTestUtil.createOrderItem(
			"Approved", null, "2025-01-01T00:00:00Z", "ORDER-ITEM-1", 5001L,
			_PRODUCT_2_ID, null);

		_stubFamilyOrder(orderItem);

		List<String> warningMessages = new ArrayList<>();

		_provisioningOrderService.trimRenewedOrderItems(
			_ACCOUNT_ID, _OPPORTUNITY_ID,
			List.of(_createRenewalLineItem("2025-02-01")), warningMessages);

		Mockito.verifyNoInteractions(_commerceOrderItemService);
		Assertions.assertEquals(0, warningMessages.size());
	}

	@Test
	public void testTrimRenewedOrderItemsWarnsWhenStartIsBeforeEndDate()
		throws Exception {

		OrderItem orderItem = SalesforceModelTestUtil.createOrderItem(
			"Approved", "2025-06-01T00:00:00Z", "2025-01-01T00:00:00Z",
			"ORDER-ITEM-1", 5001L, _PRODUCT_2_ID, null);

		_stubFamilyOrder(orderItem);

		List<String> warningMessages = new ArrayList<>();

		_provisioningOrderService.trimRenewedOrderItems(
			_ACCOUNT_ID, _OPPORTUNITY_ID,
			List.of(_createRenewalLineItem("2024-12-01")), warningMessages);

		Mockito.verifyNoInteractions(_commerceOrderItemService);
		Assertions.assertEquals(1, warningMessages.size());
		Assertions.assertTrue(
			warningMessages.get(
				0
			).contains(
				"is before the end date"
			));
	}

	private SalesforceOpportunityLineItem _createRealignmentLineItem(
		String endDate) {

		return new SalesforceOpportunityLineItem(
			SalesforceModelTestUtil.createOpportunityLineItemJSONObject(
				"USD", endDate, "LINE-1", _PRODUCT_2_ID, "Widget",
				"Subscription", 0, "2024-06-01"));
	}

	private SalesforceOpportunityLineItem _createRenewalLineItem(
		String serviceDate) {

		return new SalesforceOpportunityLineItem(
			SalesforceModelTestUtil.createOpportunityLineItemJSONObject(
				"USD", null, "LINE-1", _PRODUCT_2_ID, "Widget", "Subscription",
				5, serviceDate));
	}

	private void _stubFamilyOrder(OrderItem orderItem) throws Exception {
		Order order = new Order();

		order.setExternalReferenceCode(_PARENT_OPPORTUNITY_ID);
		order.setOrderItems(new OrderItem[] {orderItem});

		Mockito.when(
			_commerceOrderService.getAccountOrders(_ACCOUNT_ID)
		).thenReturn(
			List.of(order)
		);
	}

	private static final long _ACCOUNT_ID = 1000L;

	private static final String _OPPORTUNITY_ID = "OPP-1";

	private static final String _PARENT_OPPORTUNITY_ID = "OPP-PARENT";

	private static final String _PRODUCT_2_ID = "PROD-1";

	private CommerceOrderItemService _commerceOrderItemService;
	private CommerceOrderService _commerceOrderService;
	private EntitlementService _entitlementService;
	private ProvisioningOrderService _provisioningOrderService;

}