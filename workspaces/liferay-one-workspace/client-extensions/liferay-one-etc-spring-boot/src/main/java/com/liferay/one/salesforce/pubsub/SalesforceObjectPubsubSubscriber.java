/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.salesforce.pubsub;

import com.liferay.headless.commerce.admin.catalog.client.dto.v1_0.Sku;
import com.liferay.headless.commerce.admin.pricing.client.dto.v2_0.PriceList;
import com.liferay.one.constants.CommerceCurrencyConstants;
import com.liferay.one.pubsub.Message;
import com.liferay.one.pubsub.subscriber.BasePubsubSubscriber;
import com.liferay.one.salesforce.model.SalesforceAccount;
import com.liferay.one.salesforce.model.SalesforceContract;
import com.liferay.one.salesforce.model.SalesforcePricebookEntry;
import com.liferay.one.salesforce.model.SalesforceProduct2;
import com.liferay.one.salesforce.model.SalesforceProject;
import com.liferay.one.service.AccountService;
import com.liferay.one.service.CommercePriceEntryService;
import com.liferay.one.service.CommercePriceListService;
import com.liferay.one.service.CommerceProductService;
import com.liferay.one.service.CommerceSkuService;
import com.liferay.one.service.ContractService;
import com.liferay.one.service.ProjectService;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @author Kyle Bischof
 */
@Component
@ConditionalOnProperty(
	havingValue = "true",
	name = "liferay.one.salesforce.object.pubsub.subscriber.enabled"
)
public class SalesforceObjectPubsubSubscriber extends BasePubsubSubscriber {

	@Override
	public String getTopic() {
		return _topic;
	}

	@Override
	public void receive(Message message) throws Exception {
		JSONObject jsonObject = new JSONObject(message.getPayload());

		String action = jsonObject.getString("action");
		String salesforceObjectName = jsonObject.getString(
			"salesforceObjectName");

		JSONArray recordsJSONArray = jsonObject.getJSONArray("records");

		int failureCount = 0;

		for (int i = 0; i < recordsJSONArray.length(); i++) {
			try {
				JSONObject recordJSONObject = recordsJSONArray.getJSONObject(i);

				if (Objects.equals(salesforceObjectName, "Account")) {
					_processAccount(recordJSONObject);
				}
				else if (Objects.equals(salesforceObjectName, "Contract")) {
					_processContract(action, recordJSONObject);
				}
				else if (Objects.equals(
							salesforceObjectName, "PricebookEntry")) {

					_processPricebookEntry(action, recordJSONObject);
				}
				else if (Objects.equals(salesforceObjectName, "Product2")) {
					_processProduct2(action, recordJSONObject);
				}
				else if (Objects.equals(salesforceObjectName, "Project__c")) {
					_processProject(recordJSONObject);
				}
				else if (_log.isInfoEnabled()) {
					_log.info(
						"Unable to handle Salesforce object " +
							salesforceObjectName);
				}
			}
			catch (Exception exception) {
				failureCount++;

				_log.error(
					StringBundler.concat(
						"Unable to process Salesforce ", salesforceObjectName,
						" record ", recordsJSONArray.opt(i)),
					exception);
			}
		}

		if (failureCount > 0) {
			throw new Exception(
				StringBundler.concat(
					"Unable to process ", failureCount, " of ",
					recordsJSONArray.length(), " Salesforce ",
					salesforceObjectName, " records"));
		}
	}

	@Override
	protected String getProjectId() {
		return _projectId;
	}

	@Override
	protected String getSubscriptionName() {
		return _subscription;
	}

	@Override
	protected boolean isAutoCreateTopic() {
		return false;
	}

	private void _processAccount(JSONObject recordJSONObject) throws Exception {
		SalesforceAccount salesforceAccount = new SalesforceAccount(
			recordJSONObject);

		if (!salesforceAccount.isActiveSubscription()) {
			if (_log.isInfoEnabled()) {
				_log.info(
					"Skipping inactive Salesforce account " +
						salesforceAccount.getId());
			}

			return;
		}

		_accountService.upsertAccount(salesforceAccount);
	}

	private void _processContract(String action, JSONObject recordJSONObject)
		throws Exception {

		_contractService.upsertContract(
			action, new SalesforceContract(recordJSONObject));
	}

	private void _processPricebookEntry(
			String action, JSONObject recordJSONObject)
		throws Exception {

		SalesforcePricebookEntry salesforcePricebookEntry =
			new SalesforcePricebookEntry(recordJSONObject);

		if (Objects.equals(action, "delete")) {
			_commercePriceEntryService.deletePriceEntry(
				salesforcePricebookEntry.getId());

			return;
		}

		String currencyIsoCode = salesforcePricebookEntry.getCurrencyIsoCode();

		if (!ArrayUtil.contains(
				CommerceCurrencyConstants.CODES_SUPPORTED_CURRENCIES,
				currencyIsoCode)) {

			return;
		}

		String pricebook2Id = salesforcePricebookEntry.getPricebook2Id();

		if (Validator.isNull(pricebook2Id)) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					"Unable to process Salesforce price book entry " +
						salesforcePricebookEntry.getId() +
							" without a price book");
			}

			return;
		}

		Sku sku = _commerceSkuService.fetchSku(
			salesforcePricebookEntry.getProduct2Id());

		if (sku == null) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					"Unable to find SKU for Salesforce product " +
						salesforcePricebookEntry.getProduct2Id());
			}

			return;
		}

		String priceListExternalReferenceCode = StringBundler.concat(
			"SALESFORCE_PRICE_LIST_", pricebook2Id, "_", currencyIsoCode);

		PriceList priceList = _commercePriceListService.fetchOrAddPriceList(
			currencyIsoCode, priceListExternalReferenceCode,
			StringBundler.concat(
				"Salesforce ", pricebook2Id, " ", currencyIsoCode));

		if (priceList == null) {
			return;
		}

		_commercePriceEntryService.addOrUpdatePriceEntry(
			salesforcePricebookEntry.isActive(),
			salesforcePricebookEntry.getId(),
			salesforcePricebookEntry.getUnitPrice(), priceList.getId(),
			sku.getId());
	}

	private void _processProduct2(String action, JSONObject recordJSONObject)
		throws Exception {

		SalesforceProduct2 salesforceProduct2 = new SalesforceProduct2(
			recordJSONObject);

		if (Objects.equals(action, "delete")) {
			_commerceProductService.deactivateProduct(
				salesforceProduct2.getId());
		}
		else {
			_commerceProductService.updateProduct(
				salesforceProduct2.getDescription(),
				salesforceProduct2.getName(), salesforceProduct2.getId());
		}
	}

	private void _processProject(JSONObject recordJSONObject) throws Exception {
		_projectService.upsertProject(new SalesforceProject(recordJSONObject));
	}

	private static final Log _log = LogFactory.getLog(
		SalesforceObjectPubsubSubscriber.class);

	@Autowired
	private AccountService _accountService;

	@Autowired
	private CommercePriceEntryService _commercePriceEntryService;

	@Autowired
	private CommercePriceListService _commercePriceListService;

	@Autowired
	private CommerceProductService _commerceProductService;

	@Autowired
	private CommerceSkuService _commerceSkuService;

	@Autowired
	private ContractService _contractService;

	@Value("${liferay.one.salesforce.object.pubsub.subscriber.project.id}")
	private String _projectId;

	@Autowired
	private ProjectService _projectService;

	@Value("${liferay.one.salesforce.object.pubsub.subscriber.subscription}")
	private String _subscription;

	@Value("${liferay.one.salesforce.object.pubsub.subscriber.topic}")
	private String _topic;

}