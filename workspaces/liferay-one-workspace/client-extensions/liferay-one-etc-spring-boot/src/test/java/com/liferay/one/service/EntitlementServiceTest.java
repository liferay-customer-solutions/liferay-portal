/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.commerce.admin.order.client.custom.field.CustomField;
import com.liferay.headless.commerce.admin.order.client.custom.field.CustomValue;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.Order;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.OrderItem;
import com.liferay.one.constants.CommerceOrderItemConstants;
import com.liferay.one.model.Entitlement;
import com.liferay.one.model.EntitlementDefinition;

import java.math.BigDecimal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Felipe Veloso
 */
public class EntitlementServiceTest {

	@BeforeEach
	public void setUp() {
		_entitlementService = Mockito.spy(new EntitlementService());

		ReflectionTestUtils.setField(
			_entitlementService, "_commerceOrderItemService",
			_commerceOrderItemService);
		ReflectionTestUtils.setField(
			_entitlementService, "_commerceOrderService",
			_commerceOrderService);
		ReflectionTestUtils.setField(
			_entitlementService, "_entitlementDefinitionService",
			_entitlementDefinitionService);
	}

	@Test
	public void testGenerateEntitlementsMatchesDefinitionsBySku()
		throws Exception {

		_setUpOrderItem(_createOrderItem());

		Mockito.when(
			_entitlementDefinitionService.getEntitlementDefinitions(
				Mockito.eq(
					"(skuExternalReferenceCode eq '" +
						_SKU_EXTERNAL_REFERENCE_CODE +
							"') and (active eq true)"),
				Mockito.anyMap())
		).thenReturn(
			List.of(_createEntitlementDefinition(100.0, 1, "storage"))
		);

		Order order = new Order();

		order.setAccountId(_ACCOUNT_ID);
		order.setCustomFields(
			Map.of(
				"contractId", _CONTRACT_ID, "salesforceProjectId",
				_PROJECT_EXTERNAL_REFERENCE_CODE));

		Mockito.when(
			_commerceOrderService.fetchCommerceOrder(_ORDER_ID)
		).thenReturn(
			order
		);

		Mockito.doReturn(
			_createEntitlement(1, "storage")
		).when(
			_entitlementService
		).addEntitlement(
			Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(),
			Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.any(),
			Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()
		);

		_entitlementService.generateEntitlements(_ORDER_ITEM_ID);

		Mockito.verify(
			_entitlementService
		).addEntitlement(
			Mockito.eq(_ACCOUNT_ID), Mockito.eq(_ORDER_ITEM_ID),
			Mockito.eq(_CONTRACT_ID), Mockito.eq(1L), Mockito.isNull(),
			Mockito.eq("fixed"), Mockito.isNull(), Mockito.eq("storage"),
			Mockito.eq(_PROJECT_EXTERNAL_REFERENCE_CODE), Mockito.eq(200.0),
			Mockito.isNull()
		);
	}

	@Test
	public void testGenerateEntitlementsSkipsCanceledOrderItem()
		throws Exception {

		OrderItem orderItem = _createOrderItem();

		CustomValue customValue = new CustomValue();

		customValue.setData(CommerceOrderItemConstants.STATUS_CANCELED);

		CustomField customField = new CustomField();

		customField.setCustomValue(customValue);
		customField.setName("customStatus");

		orderItem.setCustomFields(new CustomField[] {customField});

		_setUpOrderItem(orderItem);

		_entitlementService.generateEntitlements(_ORDER_ITEM_ID);

		Mockito.verifyNoInteractions(_entitlementDefinitionService);
		Mockito.verifyNoInteractions(_commerceOrderService);
	}

	@Test
	public void testGenerateEntitlementsSkipsOrderItemWithoutDefinitions()
		throws Exception {

		_setUpOrderItem(_createOrderItem());

		Mockito.when(
			_entitlementDefinitionService.getEntitlementDefinitions(
				Mockito.anyString(), Mockito.anyMap())
		).thenReturn(
			new ArrayList<>()
		);

		_entitlementService.generateEntitlements(_ORDER_ITEM_ID);

		Mockito.verifyNoInteractions(_commerceOrderService);
		Mockito.verify(
			_entitlementService, Mockito.never()
		).addEntitlement(
			Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(),
			Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.any(),
			Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()
		);
	}

	@Test
	public void testGetActiveEntitlementDefinitionsDedupesSharedDefinition()
		throws Exception {

		_setUpEntitlements(
			_createEntitlement(1, "sites"), _createEntitlement(1, "sites"),
			_createEntitlement(2, "logs"));

		Assertions.assertEquals(
			List.of(1L, 2L),
			_getEntitlementDefinitionIds(
				_entitlementService.getActiveEntitlementDefinitions(
					_ACCOUNT_ID)));
	}

	@Test
	public void testGetActiveEntitlementDefinitionsPreservesEncounterOrder()
		throws Exception {

		_setUpEntitlements(
			_createEntitlement(3, "logs"), _createEntitlement(1, "sites"),
			_createEntitlement(2, "storage"));

		Assertions.assertEquals(
			List.of(3L, 1L, 2L),
			_getEntitlementDefinitionIds(
				_entitlementService.getActiveEntitlementDefinitions(
					_ACCOUNT_ID)));
	}

	@Test
	public void testGetActiveEntitlementDefinitionsReadsNestedDefinition()
		throws Exception {

		_setUpEntitlements(_createEntitlement(1, "storage"));

		List<EntitlementDefinition> entitlementDefinitions =
			_entitlementService.getActiveEntitlementDefinitions(_ACCOUNT_ID);

		Assertions.assertEquals(1, entitlementDefinitions.size());

		EntitlementDefinition entitlementDefinition =
			entitlementDefinitions.get(0);

		Assertions.assertEquals(
			1, entitlementDefinition.getEntitlementDefinitionId());
		Assertions.assertEquals(
			_SKU_EXTERNAL_REFERENCE_CODE,
			entitlementDefinition.getSkuExternalReferenceCode());
		Assertions.assertEquals("TiB", entitlementDefinition.getUnit());
	}

	@Test
	public void testGetActiveEntitlementDefinitionsSkipsUnnestedDefinition()
		throws Exception {

		_setUpEntitlements(
			_createEntitlementWithoutDefinition(1, "sites"),
			_createEntitlement(2, "logs"));

		Assertions.assertEquals(
			List.of(2L),
			_getEntitlementDefinitionIds(
				_entitlementService.getActiveEntitlementDefinitions(
					_ACCOUNT_ID)));
	}

	@Test
	public void testGetEntitlementsRequestsNestedEntitlementDefinition()
		throws Exception {

		Mockito.doReturn(
			Collections.emptyList()
		).when(
			_entitlementService
		).getAllItems(
			Mockito.anyString(), Mockito.anyString(), Mockito.any(),
			Mockito.isNull(), Mockito.anyString()
		);

		_entitlementService.getEntitlements("name eq 'sites'");

		Mockito.verify(
			_entitlementService
		).getAllItems(
			Mockito.eq("/o/c/entitlements"), Mockito.eq("name eq 'sites'"),
			Mockito.any(), Mockito.isNull(),
			Mockito.eq("entitlementDefinitionToEntitlement")
		);
	}

	private Entitlement _createEntitlement(
		long entitlementDefinitionId, String name) {

		JSONObject jsonObject = _createEntitlementJSONObject(
			entitlementDefinitionId, name);

		jsonObject.put(
			"entitlementDefinitionToEntitlement",
			new JSONObject(
			).put(
				"id", entitlementDefinitionId
			).put(
				"skuExternalReferenceCode", _SKU_EXTERNAL_REFERENCE_CODE
			).put(
				"unit", "TiB"
			));

		return new Entitlement(jsonObject);
	}

	private EntitlementDefinition _createEntitlementDefinition(
		Double defaultQuantity, long entitlementDefinitionId, String name) {

		return new EntitlementDefinition(
			new JSONObject(
			).put(
				"active", true
			).put(
				"defaultQuantity", defaultQuantity
			).put(
				"grantType", "fixed"
			).put(
				"id", entitlementDefinitionId
			).put(
				"name", name
			).put(
				"skuExternalReferenceCode", _SKU_EXTERNAL_REFERENCE_CODE
			));
	}

	private JSONObject _createEntitlementJSONObject(
		long entitlementDefinitionId, String name) {

		return new JSONObject(
		).put(
			"id", entitlementDefinitionId
		).put(
			"name", name
		).put(
			"r_entitlementDefinitionToEntitlement_c_entitlementDefinitionId",
			entitlementDefinitionId
		);
	}

	private Entitlement _createEntitlementWithoutDefinition(
		long entitlementDefinitionId, String name) {

		return new Entitlement(
			_createEntitlementJSONObject(entitlementDefinitionId, name));
	}

	private OrderItem _createOrderItem() {
		OrderItem orderItem = new OrderItem();

		orderItem.setId(_ORDER_ITEM_ID);
		orderItem.setOrderId(_ORDER_ID);
		orderItem.setQuantity(BigDecimal.valueOf(2));
		orderItem.setSkuExternalReferenceCode(_SKU_EXTERNAL_REFERENCE_CODE);

		return orderItem;
	}

	private List<Long> _getEntitlementDefinitionIds(
		List<EntitlementDefinition> entitlementDefinitions) {

		List<Long> entitlementDefinitionIds = new ArrayList<>();

		for (EntitlementDefinition entitlementDefinition :
				entitlementDefinitions) {

			entitlementDefinitionIds.add(
				entitlementDefinition.getEntitlementDefinitionId());
		}

		return entitlementDefinitionIds;
	}

	private void _setUpEntitlements(Entitlement... entitlements)
		throws Exception {

		Mockito.doReturn(
			Arrays.asList(entitlements)
		).when(
			_entitlementService
		).getEntitlements(
			Mockito.anyString()
		);
	}

	private void _setUpOrderItem(OrderItem orderItem) throws Exception {
		Mockito.when(
			_commerceOrderItemService.fetchCommerceOrderItem(_ORDER_ITEM_ID)
		).thenReturn(
			orderItem
		);
	}

	private static final long _ACCOUNT_ID = 40001;

	private static final long _CONTRACT_ID = 60001;

	private static final long _ORDER_ID = 9001;

	private static final long _ORDER_ITEM_ID = 7001;

	private static final String _PROJECT_EXTERNAL_REFERENCE_CODE = "PRJCT-001";

	private static final String _SKU_EXTERNAL_REFERENCE_CODE = "SKU-001";

	private final CommerceOrderItemService _commerceOrderItemService =
		Mockito.mock(CommerceOrderItemService.class);
	private final CommerceOrderService _commerceOrderService = Mockito.mock(
		CommerceOrderService.class);
	private final EntitlementDefinitionService _entitlementDefinitionService =
		Mockito.mock(EntitlementDefinitionService.class);
	private EntitlementService _entitlementService;

}