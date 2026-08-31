/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.salesforce.pubsub;

import com.liferay.headless.commerce.admin.catalog.client.dto.v1_0.Sku;
import com.liferay.headless.commerce.admin.pricing.client.dto.v2_0.PriceList;
import com.liferay.one.pubsub.Message;
import com.liferay.one.salesforce.model.SalesforceAccount;
import com.liferay.one.salesforce.model.SalesforceContract;
import com.liferay.one.salesforce.model.SalesforceModelTestUtil;
import com.liferay.one.salesforce.model.SalesforceProject;
import com.liferay.one.service.AccountService;
import com.liferay.one.service.CommercePriceEntryService;
import com.liferay.one.service.CommercePriceListService;
import com.liferay.one.service.CommerceProductService;
import com.liferay.one.service.CommerceSkuService;
import com.liferay.one.service.ContractService;
import com.liferay.one.service.ProjectService;
import com.liferay.petra.string.StringBundler;

import java.util.Collections;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Felipe Franca
 */
public class SalesforceObjectPubsubSubscriberTest {

	@BeforeEach
	public void setUp() {
		_subscriber = new SalesforceObjectPubsubSubscriber();

		_accountService = Mockito.mock(AccountService.class);
		_commercePriceEntryService = Mockito.mock(
			CommercePriceEntryService.class);
		_commercePriceListService = Mockito.mock(
			CommercePriceListService.class);
		_commerceProductService = Mockito.mock(CommerceProductService.class);
		_commerceSkuService = Mockito.mock(CommerceSkuService.class);
		_contractService = Mockito.mock(ContractService.class);
		_projectService = Mockito.mock(ProjectService.class);

		ReflectionTestUtils.setField(
			_subscriber, "_accountService", _accountService);
		ReflectionTestUtils.setField(
			_subscriber, "_commercePriceEntryService",
			_commercePriceEntryService);
		ReflectionTestUtils.setField(
			_subscriber, "_commercePriceListService",
			_commercePriceListService);
		ReflectionTestUtils.setField(
			_subscriber, "_commerceProductService", _commerceProductService);
		ReflectionTestUtils.setField(
			_subscriber, "_commerceSkuService", _commerceSkuService);
		ReflectionTestUtils.setField(
			_subscriber, "_contractService", _contractService);
		ReflectionTestUtils.setField(_subscriber, "_projectId", "test-project");
		ReflectionTestUtils.setField(
			_subscriber, "_projectService", _projectService);
		ReflectionTestUtils.setField(
			_subscriber, "_subscription", "test-subscription");
		ReflectionTestUtils.setField(_subscriber, "_topic", "test-topic");
	}

	@Test
	public void testIsAutoCreateTopicReturnsFalse() {
		Assertions.assertFalse(_subscriber.isAutoCreateTopic());
	}

	@Test
	public void testReceiveDeactivatesProduct2OnDelete() throws Exception {
		_receiveMessage(
			"delete", "Product2", _createProduct2JSONObject("Widget"));

		Mockito.verify(
			_commerceProductService
		).deactivateProduct(
			_PRODUCT_2_ID
		);

		Mockito.verify(
			_commerceProductService, Mockito.never()
		).updateProduct(
			Mockito.any(), Mockito.any(), Mockito.any()
		);
	}

	@Test
	public void testReceiveDeletesPricebookEntryOnDelete() throws Exception {
		_receiveMessage(
			"delete", "PricebookEntry",
			_createPricebookEntryJSONObject("USD", 100.0));

		Mockito.verify(
			_commercePriceEntryService
		).deletePriceEntry(
			_PRICEBOOK_ENTRY_ID
		);

		Mockito.verify(
			_commerceSkuService, Mockito.never()
		).fetchSku(
			Mockito.any()
		);

		Mockito.verify(
			_commercePriceEntryService, Mockito.never()
		).addOrUpdatePriceEntry(
			Mockito.anyBoolean(), Mockito.any(), Mockito.anyDouble(),
			Mockito.anyLong(), Mockito.anyLong()
		);
	}

	@Test
	public void testReceiveDoesNothingWhenRecordsArrayIsEmpty()
		throws Exception {

		Message message = new Message(
			Collections.emptyMap(),
			SalesforceModelTestUtil.createObjectMessagePayload(
				"update", "Account"
			).toString(),
			"test-topic");

		Assertions.assertDoesNotThrow(() -> _subscriber.receive(message));

		Mockito.verifyNoInteractions(
			_accountService, _commercePriceEntryService,
			_commercePriceListService, _commerceProductService,
			_commerceSkuService, _contractService, _projectService);
	}

	@Test
	public void testReceiveIgnoresUnknownSalesforceObjectName()
		throws Exception {

		Assertions.assertDoesNotThrow(
			() -> _receiveMessage(
				"update", "Widget__c",
				new JSONObject(
				).put(
					"Id", "W-1"
				)));

		Mockito.verifyNoInteractions(
			_accountService, _commercePriceEntryService,
			_commercePriceListService, _commerceProductService,
			_commerceSkuService, _contractService, _projectService);
	}

	@Test
	public void testReceiveProcessesRemainingRecordsAfterFailure()
		throws Exception {

		Mockito.doThrow(
			new Exception("Unable to add or update product")
		).doNothing(
		).when(
			_commerceProductService
		).updateProduct(
			Mockito.any(), Mockito.any(), Mockito.any()
		);

		Message message = new Message(
			Collections.emptyMap(),
			SalesforceModelTestUtil.createObjectMessagePayload(
				"update", "Product2", _createProduct2JSONObject("Widget"),
				_createProduct2JSONObject("Gadget")
			).toString(),
			"test-topic");

		Exception exception = Assertions.assertThrows(
			Exception.class, () -> _subscriber.receive(message));

		Assertions.assertTrue(
			exception.getMessage(
			).contains(
				"1 of 2"
			));

		Mockito.verify(
			_commerceProductService, Mockito.times(2)
		).updateProduct(
			Mockito.any(), Mockito.any(), Mockito.any()
		);

		Mockito.verify(
			_commerceProductService
		).updateProduct(
			Mockito.any(), Mockito.eq("Gadget"), Mockito.any()
		);
	}

	@Test
	public void testReceiveRethrowsOnMalformedPayload() {
		Message message = new Message(
			Collections.emptyMap(), "not json", "test-topic");

		Assertions.assertThrows(
			JSONException.class, () -> _subscriber.receive(message));
	}

	@Test
	public void testReceiveRethrowsWhenSalesforceObjectNameIsAbsent() {
		Message message = new Message(
			Collections.emptyMap(),
			new JSONObject(
			).put(
				"action", "update"
			).put(
				"records", new JSONArray()
			).toString(),
			"test-topic");

		Assertions.assertThrows(
			JSONException.class, () -> _subscriber.receive(message));
	}

	@Test
	public void testReceiveSkipsInactiveAccount() throws Exception {
		_receiveMessage(
			"update", "Account",
			SalesforceModelTestUtil.createAccountJSONObject(
				false, "", "SF-ACCOUNT-1", "Test Account"));

		Mockito.verify(
			_accountService, Mockito.never()
		).upsertAccount(
			Mockito.any()
		);
	}

	@Test
	public void testReceiveSkipsPricebookEntryWhenPriceListNotFound()
		throws Exception {

		Sku sku = new Sku();

		sku.setId(_SKU_ID);

		Mockito.when(
			_commerceSkuService.fetchSku(_PRODUCT_2_ID)
		).thenReturn(
			sku
		);

		Mockito.when(
			_commercePriceListService.fetchOrAddPriceList(
				Mockito.any(), Mockito.any(), Mockito.any())
		).thenReturn(
			null
		);

		_receiveMessage(
			"update", "PricebookEntry",
			_createPricebookEntryJSONObject("USD", 100.0));

		Mockito.verify(
			_commercePriceEntryService, Mockito.never()
		).addOrUpdatePriceEntry(
			Mockito.anyBoolean(), Mockito.any(), Mockito.anyDouble(),
			Mockito.anyLong(), Mockito.anyLong()
		);
	}

	@Test
	public void testReceiveSkipsPricebookEntryWithBlankPricebook2Id()
		throws Exception {

		JSONObject recordJSONObject = _createPricebookEntryJSONObject(
			"USD", 100.0);

		recordJSONObject.put("Pricebook2Id", "");

		_receiveMessage("update", "PricebookEntry", recordJSONObject);

		Mockito.verify(
			_commerceSkuService, Mockito.never()
		).fetchSku(
			Mockito.any()
		);

		Mockito.verify(
			_commercePriceEntryService, Mockito.never()
		).addOrUpdatePriceEntry(
			Mockito.anyBoolean(), Mockito.any(), Mockito.anyDouble(),
			Mockito.anyLong(), Mockito.anyLong()
		);
	}

	@Test
	public void testReceiveSkipsPricebookEntryWithoutSku() throws Exception {
		Mockito.when(
			_commerceSkuService.fetchSku(_PRODUCT_2_ID)
		).thenReturn(
			null
		);

		_receiveMessage(
			"update", "PricebookEntry",
			_createPricebookEntryJSONObject("USD", 100.0));

		Mockito.verify(
			_commercePriceEntryService, Mockito.never()
		).addOrUpdatePriceEntry(
			Mockito.anyBoolean(), Mockito.any(), Mockito.anyDouble(),
			Mockito.anyLong(), Mockito.anyLong()
		);
	}

	@Test
	public void testReceiveSkipsPricebookEntryWithUnsupportedCurrency()
		throws Exception {

		_receiveMessage(
			"update", "PricebookEntry",
			_createPricebookEntryJSONObject("CHF", 100.0));

		Mockito.verify(
			_commerceSkuService, Mockito.never()
		).fetchSku(
			Mockito.any()
		);

		Mockito.verify(
			_commercePriceEntryService, Mockito.never()
		).addOrUpdatePriceEntry(
			Mockito.anyBoolean(), Mockito.any(), Mockito.anyDouble(),
			Mockito.anyLong(), Mockito.anyLong()
		);
	}

	@Test
	public void testReceiveThrowsWhenAllRecordsFail() throws Exception {
		Mockito.doThrow(
			new Exception("Unable to add or update product")
		).when(
			_commerceProductService
		).updateProduct(
			Mockito.any(), Mockito.any(), Mockito.any()
		);

		Message message = new Message(
			Collections.emptyMap(),
			SalesforceModelTestUtil.createObjectMessagePayload(
				"update", "Product2", _createProduct2JSONObject("Widget"),
				_createProduct2JSONObject("Gadget")
			).toString(),
			"test-topic");

		Exception exception = Assertions.assertThrows(
			Exception.class, () -> _subscriber.receive(message));

		Assertions.assertTrue(
			exception.getMessage(
			).contains(
				"2 of 2"
			));
	}

	@Test
	public void testReceiveUpsertsActiveAccount() throws Exception {
		_receiveMessage(
			"update", "Account",
			SalesforceModelTestUtil.createAccountJSONObject(
				true, "", "SF-ACCOUNT-1", "Test Account"));

		ArgumentCaptor<SalesforceAccount> salesforceAccountArgumentCaptor =
			ArgumentCaptor.forClass(SalesforceAccount.class);

		Mockito.verify(
			_accountService
		).upsertAccount(
			salesforceAccountArgumentCaptor.capture()
		);

		Assertions.assertEquals(
			"SF-ACCOUNT-1",
			salesforceAccountArgumentCaptor.getValue(
			).getId());
	}

	@Test
	public void testReceiveUpsertsContract() throws Exception {
		JSONObject recordJSONObject = new JSONObject(
		).put(
			"Id", "SF-CONTRACT-1"
		).put(
			"SBQQ__Opportunity__c", "OPP-1"
		);

		_receiveMessage("update", "Contract", recordJSONObject);

		ArgumentCaptor<SalesforceContract> salesforceContractArgumentCaptor =
			ArgumentCaptor.forClass(SalesforceContract.class);

		Mockito.verify(
			_contractService
		).upsertContract(
			Mockito.eq("update"), salesforceContractArgumentCaptor.capture()
		);

		Assertions.assertEquals(
			"SF-CONTRACT-1",
			salesforceContractArgumentCaptor.getValue(
			).getId());
	}

	@Test
	public void testReceiveUpsertsInactivePricebookEntry() throws Exception {
		Sku sku = new Sku();

		sku.setId(_SKU_ID);

		Mockito.when(
			_commerceSkuService.fetchSku(_PRODUCT_2_ID)
		).thenReturn(
			sku
		);

		PriceList priceList = new PriceList();

		priceList.setId(_PRICE_LIST_ID);

		Mockito.when(
			_commercePriceListService.fetchOrAddPriceList(
				Mockito.any(), Mockito.any(), Mockito.any())
		).thenReturn(
			priceList
		);

		_receiveMessage(
			"update", "PricebookEntry",
			_createPricebookEntryJSONObject(false, "USD", 100.0));

		Mockito.verify(
			_commercePriceEntryService
		).addOrUpdatePriceEntry(
			false, _PRICEBOOK_ENTRY_ID, 100.0, _PRICE_LIST_ID, _SKU_ID
		);
	}

	@Test
	public void testReceiveUpsertsPricebookEntry() throws Exception {
		Sku sku = new Sku();

		sku.setId(_SKU_ID);

		Mockito.when(
			_commerceSkuService.fetchSku(_PRODUCT_2_ID)
		).thenReturn(
			sku
		);

		PriceList priceList = new PriceList();

		priceList.setId(_PRICE_LIST_ID);

		Mockito.when(
			_commercePriceListService.fetchOrAddPriceList(
				"USD",
				StringBundler.concat(
					"SALESFORCE_PRICE_LIST_", _PRICEBOOK_2_ID, "_USD"),
				StringBundler.concat("Salesforce ", _PRICEBOOK_2_ID, " USD"))
		).thenReturn(
			priceList
		);

		_receiveMessage(
			"update", "PricebookEntry",
			_createPricebookEntryJSONObject("USD", 100.0));

		Mockito.verify(
			_commercePriceListService
		).fetchOrAddPriceList(
			"USD",
			StringBundler.concat(
				"SALESFORCE_PRICE_LIST_", _PRICEBOOK_2_ID, "_USD"),
			StringBundler.concat("Salesforce ", _PRICEBOOK_2_ID, " USD")
		);

		Mockito.verify(
			_commercePriceEntryService
		).addOrUpdatePriceEntry(
			true, _PRICEBOOK_ENTRY_ID, 100.0, _PRICE_LIST_ID, _SKU_ID
		);
	}

	@Test
	public void testReceiveUpsertsProduct2() throws Exception {
		_receiveMessage(
			"update", "Product2", _createProduct2JSONObject("Widget"));

		Mockito.verify(
			_commerceProductService
		).updateProduct(
			"A description", "Widget", _PRODUCT_2_ID
		);
	}

	@Test
	public void testReceiveUpsertsProject() throws Exception {
		JSONObject recordJSONObject = new JSONObject(
		).put(
			"Id", "SF-PROJ-1"
		).put(
			"Name", "My Project"
		);

		_receiveMessage("update", "Project__c", recordJSONObject);

		ArgumentCaptor<SalesforceProject> salesforceProjectArgumentCaptor =
			ArgumentCaptor.forClass(SalesforceProject.class);

		Mockito.verify(
			_projectService
		).upsertProject(
			salesforceProjectArgumentCaptor.capture()
		);

		Assertions.assertEquals(
			"SF-PROJ-1",
			salesforceProjectArgumentCaptor.getValue(
			).getId());
	}

	private JSONObject _createPricebookEntryJSONObject(
		boolean active, String currencyIsoCode, double unitPrice) {

		return new JSONObject(
		).put(
			"CurrencyIsoCode", currencyIsoCode
		).put(
			"Id", _PRICEBOOK_ENTRY_ID
		).put(
			"IsActive", active
		).put(
			"Pricebook2Id", _PRICEBOOK_2_ID
		).put(
			"Product2Id", _PRODUCT_2_ID
		).put(
			"UnitPrice", unitPrice
		);
	}

	private JSONObject _createPricebookEntryJSONObject(
		String currencyIsoCode, double unitPrice) {

		return _createPricebookEntryJSONObject(
			true, currencyIsoCode, unitPrice);
	}

	private JSONObject _createProduct2JSONObject(String name) {
		return new JSONObject(
		).put(
			"Description", "A description"
		).put(
			"Id", _PRODUCT_2_ID
		).put(
			"Name", name
		);
	}

	private void _receiveMessage(
			String action, String salesforceObjectName,
			JSONObject... recordJSONObjects)
		throws Exception {

		Message message = new Message(
			Collections.emptyMap(),
			SalesforceModelTestUtil.createObjectMessagePayload(
				action, salesforceObjectName, recordJSONObjects
			).toString(),
			"test-topic");

		_subscriber.receive(message);
	}

	private static final long _PRICE_LIST_ID = 3000L;

	private static final String _PRICEBOOK_2_ID = "PB-1";

	private static final String _PRICEBOOK_ENTRY_ID = "PBE-1";

	private static final String _PRODUCT_2_ID = "PROD-1";

	private static final long _SKU_ID = 2000L;

	private AccountService _accountService;
	private CommercePriceEntryService _commercePriceEntryService;
	private CommercePriceListService _commercePriceListService;
	private CommerceProductService _commerceProductService;
	private CommerceSkuService _commerceSkuService;
	private ContractService _contractService;
	private ProjectService _projectService;
	private SalesforceObjectPubsubSubscriber _subscriber;

}