/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.one.model.CommerceOrder;
import com.liferay.one.model.CommerceOrderItem;
import com.liferay.one.model.Entitlement;
import com.liferay.one.model.EntitlementDefinition;

import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.Assertions;

import org.json.JSONArray;
import org.json.JSONObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Felipe Veloso
 */
@ExtendWith(MockitoExtension.class)
public class EntitlementServiceTest {

	@BeforeEach
	public void setUp() throws Exception {
		MockitoAnnotations.openMocks(this);

		Mockito.lenient(
		).doReturn(
			null
		).when(
			_entitlementService
		).addEntitlement(
			ArgumentMatchers.anyLong(), ArgumentMatchers.anyLong(),
			ArgumentMatchers.anyLong(), ArgumentMatchers.any(),
			ArgumentMatchers.any(), ArgumentMatchers.any(),
			ArgumentMatchers.any(), ArgumentMatchers.any(),
			ArgumentMatchers.any()
		);
	}

	@Test
	public void testGenerateWhenDuplicateCallSkipsExisting() throws Exception {
		CommerceOrderItem commerceOrderItem = _commerceOrderItem(
			11, 100, 0, null, null);

		List<EntitlementDefinition> entitlementDefinitions = Arrays.asList(
			_entitlementDefinition(7001, "database-size", "fixed", 50.0, null));

		Mockito.when(
			_commerceOrderItemService.fetchCommerceOrderItem(11)
		).thenReturn(
			commerceOrderItem
		);

		Mockito.when(
			_entitlementDefinitionService.getEntitlementDefinitions(
				ArgumentMatchers.anyString())
		).thenReturn(
			entitlementDefinitions
		);

		Mockito.doReturn(
			_entitlement(11, 7001)
		).when(
			_entitlementService
		).fetchEntitlement(
			11, 7001
		);

		_entitlementService.generateEntitlements(11);

		Mockito.verify(
			_entitlementService, Mockito.never()
		).addEntitlement(
			ArgumentMatchers.anyLong(), ArgumentMatchers.anyLong(),
			ArgumentMatchers.anyLong(), ArgumentMatchers.any(),
			ArgumentMatchers.anyString(), ArgumentMatchers.any(),
			ArgumentMatchers.any(), ArgumentMatchers.any(),
			ArgumentMatchers.any()
		);
	}

	@Test
	public void testGenerateWhenMachineTypeIsNullOmitsFilterClause()
		throws Exception {

		CommerceOrderItem commerceOrderItem = _commerceOrderItem(
			15, 100, 0, null, null);

		List<EntitlementDefinition> entitlementDefinitions = Arrays.asList(
			_entitlementDefinition(
				9001, "standard-database", "fixed", 100.0, "Standard"),
			_entitlementDefinition(
				9002, "high-database", "fixed", 200.0, "High"));

		Mockito.when(
			_commerceOrderItemService.fetchCommerceOrderItem(15)
		).thenReturn(
			commerceOrderItem
		);

		ArgumentCaptor<String> filterCaptor = ArgumentCaptor.forClass(
			String.class);

		Mockito.when(
			_entitlementDefinitionService.getEntitlementDefinitions(
				filterCaptor.capture())
		).thenReturn(
			entitlementDefinitions
		);

		Mockito.doReturn(
			null
		).when(
			_entitlementService
		).fetchEntitlement(
			ArgumentMatchers.eq(15L), ArgumentMatchers.anyLong()
		);

		_entitlementService.generateEntitlements(15);

		Assertions.assertThat(
			filterCaptor.getValue()
		).doesNotContain(
			"machineType"
		);
	}

	@Test
	public void testGenerateWhenMachineTypeIsSetAddsFilterClause()
		throws Exception {

		CommerceOrderItem commerceOrderItem = _commerceOrderItem(
			14, 100, 0, "Standard", null);

		List<EntitlementDefinition> entitlementDefinitions = Arrays.asList(
			_entitlementDefinition(
				9001, "standard-database", "fixed", 100.0, "Standard"),
			_entitlementDefinition(9003, "database-size", "fixed", 50.0, null));

		Mockito.when(
			_commerceOrderItemService.fetchCommerceOrderItem(14)
		).thenReturn(
			commerceOrderItem
		);

		ArgumentCaptor<String> filterCaptor = ArgumentCaptor.forClass(
			String.class);

		Mockito.when(
			_entitlementDefinitionService.getEntitlementDefinitions(
				filterCaptor.capture())
		).thenReturn(
			entitlementDefinitions
		);

		Mockito.doReturn(
			null
		).when(
			_entitlementService
		).fetchEntitlement(
			ArgumentMatchers.eq(14L), ArgumentMatchers.anyLong()
		);

		_entitlementService.generateEntitlements(14);

		Mockito.verify(
			_entitlementService, Mockito.times(1)
		).addEntitlement(
			14, 0, 9001, null, "fixed", null, "standard-database", 100.0, null
		);

		Mockito.verify(
			_entitlementService, Mockito.times(1)
		).addEntitlement(
			14, 0, 9003, null, "fixed", null, "database-size", 50.0, null
		);

		Assertions.assertThat(
			filterCaptor.getValue()
		).contains(
			"(r_commerceProductToEntitlementDefinition_CProductId eq '100')"
		).contains(
			"(entitlementDefinitionActive eq true)"
		).contains(
			"(machineType eq 'Standard') or (machineType eq null)"
		);
	}

	@Test
	public void testGenerateWhenOrderHasContractIdSetsRelationship()
		throws Exception {

		CommerceOrderItem commerceOrderItem = _commerceOrderItem(
			20, 100, 555, null, null);

		List<EntitlementDefinition> entitlementDefinitions = Arrays.asList(
			_entitlementDefinition(7001, "database-size", "fixed", 50.0, null));

		Mockito.when(
			_commerceOrderItemService.fetchCommerceOrderItem(20)
		).thenReturn(
			commerceOrderItem
		);

		Mockito.when(
			_entitlementDefinitionService.getEntitlementDefinitions(
				ArgumentMatchers.anyString())
		).thenReturn(
			entitlementDefinitions
		);

		Mockito.when(
			_commerceOrderService.fetchCommerceOrder(555)
		).thenReturn(
			_commerceOrder(555, 42)
		);

		Mockito.doReturn(
			null
		).when(
			_entitlementService
		).fetchEntitlement(
			20, 7001
		);

		_entitlementService.generateEntitlements(20);

		Mockito.verify(
			_entitlementService
		).addEntitlement(
			20, 42, 7001, null, "fixed", null, "database-size", 50.0, null
		);
	}

	@Test
	public void testGenerateWhenOrderItemIsMissingDoesNothing()
		throws Exception {

		Mockito.when(
			_commerceOrderItemService.fetchCommerceOrderItem(99)
		).thenReturn(
			null
		);

		_entitlementService.generateEntitlements(99);

		Mockito.verify(
			_entitlementService, Mockito.never()
		).addEntitlement(
			ArgumentMatchers.anyLong(), ArgumentMatchers.anyLong(),
			ArgumentMatchers.anyLong(), ArgumentMatchers.any(),
			ArgumentMatchers.any(), ArgumentMatchers.any(),
			ArgumentMatchers.any(), ArgumentMatchers.any(),
			ArgumentMatchers.any()
		);
	}

	@Test
	public void testGenerateWhenSizingOverridesDefaultQuantity()
		throws Exception {

		CommerceOrderItem commerceOrderItem = _commerceOrderItem(
			13, 100, 0, null, 250.0);

		List<EntitlementDefinition> entitlementDefinitions = Arrays.asList(
			_entitlementDefinition(8001, "sizing", "fixed", 10.0, null));

		Mockito.when(
			_commerceOrderItemService.fetchCommerceOrderItem(13)
		).thenReturn(
			commerceOrderItem
		);

		Mockito.when(
			_entitlementDefinitionService.getEntitlementDefinitions(
				ArgumentMatchers.anyString())
		).thenReturn(
			entitlementDefinitions
		);

		Mockito.doReturn(
			null
		).when(
			_entitlementService
		).fetchEntitlement(
			13, 8001
		);

		_entitlementService.generateEntitlements(13);

		Mockito.verify(
			_entitlementService
		).addEntitlement(
			13, 0, 8001, null, "fixed", null, "sizing", 250.0, null
		);
	}

	@Test
	public void testGenerateWhenStandardGeneration() throws Exception {
		CommerceOrderItem commerceOrderItem = _commerceOrderItem(
			12, 100, 0, null, null);

		List<EntitlementDefinition> entitlementDefinitions = Arrays.asList(
			_entitlementDefinition(7001, "database-size", "fixed", 50.0, null),
			_entitlementDefinition(7002, "vcpu", "fixed", 4.0, null));

		Mockito.when(
			_commerceOrderItemService.fetchCommerceOrderItem(12)
		).thenReturn(
			commerceOrderItem
		);

		Mockito.when(
			_entitlementDefinitionService.getEntitlementDefinitions(
				ArgumentMatchers.anyString())
		).thenReturn(
			entitlementDefinitions
		);

		Mockito.doReturn(
			null
		).when(
			_entitlementService
		).fetchEntitlement(
			ArgumentMatchers.eq(12L), ArgumentMatchers.anyLong()
		);

		_entitlementService.generateEntitlements(12);

		Mockito.verify(
			_entitlementService
		).addEntitlement(
			12, 0, 7001, null, "fixed", null, "database-size", 50.0, null
		);

		Mockito.verify(
			_entitlementService
		).addEntitlement(
			12, 0, 7002, null, "fixed", null, "vcpu", 4.0, null
		);
	}

	private CommerceOrder _commerceOrder(long id, long contractId) {
		JSONObject jsonObject = new JSONObject(
		).put(
			"id", id
		);

		if (contractId > 0) {
			jsonObject.put("r_contractToOrder_c_contractId", contractId);
		}

		return new CommerceOrder(jsonObject);
	}

	private CommerceOrderItem _commerceOrderItem(
		long id, long cProductId, long orderId, String machineType,
		Double sizing) {

		JSONArray customFieldsJSONArray = new JSONArray();

		if (machineType != null) {
			customFieldsJSONArray.put(_customField("machineType", machineType));
		}

		if (sizing != null) {
			customFieldsJSONArray.put(_customField("sizing", sizing));
		}

		return new CommerceOrderItem(
			new JSONObject(
			).put(
				"customFields", customFieldsJSONArray
			).put(
				"id", id
			).put(
				"orderId", orderId
			).put(
				"productId", cProductId
			));
	}

	private JSONObject _customField(String name, Object data) {
		return new JSONObject(
		).put(
			"customValue",
			new JSONObject(
			).put(
				"data", data
			)
		).put(
			"name", name
		);
	}

	private Entitlement _entitlement(
		long commerceOrderItemId, long entitlementDefinitionId) {

		String entitlementDefinitionFK =
			"r_entitlementDefinitionToEntitlement_c_entitlementDefinitionId";

		return new Entitlement(
			new JSONObject(
			).put(
				entitlementDefinitionFK, entitlementDefinitionId
			).put(
				"id", 1L
			).put(
				"r_commerceOrderItemToEntitlement_commerceOrderItemId",
				commerceOrderItemId
			));
	}

	private EntitlementDefinition _entitlementDefinition(
		long entitlementDefinitionId, String name, String grantType,
		Double defaultQuantity, String machineType) {

		JSONObject jsonObject = new JSONObject(
		).put(
			"defaultQuantity", defaultQuantity
		).put(
			"entitlementDefinitionActive", true
		).put(
			"grantType", grantType
		).put(
			"id", entitlementDefinitionId
		).put(
			"name", name
		);

		if (machineType != null) {
			jsonObject.put("machineType", machineType);
		}

		return new EntitlementDefinition(jsonObject);
	}

	@Mock
	private CommerceOrderItemService _commerceOrderItemService;

	@Mock
	private CommerceOrderService _commerceOrderService;

	@Mock
	private EntitlementDefinitionService _entitlementDefinitionService;

	@InjectMocks
	@Spy
	private EntitlementService _entitlementService;

}