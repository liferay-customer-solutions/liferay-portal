/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.liferay.client.extension.util.spring.boot3.BaseRestController;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.BillingAddress;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.Order;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.OrderItem;
import com.liferay.one.constants.CommerceOrderConstants;
import com.liferay.one.service.AIHubService;
import com.liferay.one.service.CommerceOrderService;
import com.liferay.one.service.CountryService;
import com.liferay.one.service.SalesforceService;
import com.liferay.one.service.UserAccountService;
import com.liferay.one.util.CommerceOrderUtil;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.StringUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Keven Leone
 */
@RequestMapping("/object/action/ai/hub/tokens")
@RestController
public class ObjectActionAIHubTokensRestController extends BaseRestController {

	@PostMapping
	public void post(@AuthenticationPrincipal Jwt jwt, @RequestBody String json)
		throws Exception {

		JSONObject jsonObject = new JSONObject(json);

		JSONObject commerceOrderJSONObject = jsonObject.getJSONObject(
			"commerceOrder");

		int orderStatus = commerceOrderJSONObject.getInt("orderStatus");
		int paymentStatus = commerceOrderJSONObject.getInt("paymentStatus");

		if ((orderStatus == CommerceOrderConstants.ORDER_STATUS_COMPLETED) ||
			(paymentStatus !=
				CommerceOrderConstants.ORDER_PAYMENT_STATUS_COMPLETED)) {

			if (_log.isInfoEnabled()) {
				_log.info(
					StringBundler.concat(
						"Skipping POST AI Hub token for order ",
						commerceOrderJSONObject.getLong("id"),
						" because order or payment status is not completed"));
			}

			return;
		}

		Order order = _commerceOrderService.fetchCommerceOrder(
			commerceOrderJSONObject.getLong("id"));

		if (order == null) {
			if (_log.isInfoEnabled()) {
				_log.info(
					"Order was not found: " +
						commerceOrderJSONObject.getLong("id"));
			}

			return;
		}

		JSONObject aiHubApplicationJSONObject =
			_aiHubService.getAIHubApplicationJSONObject(
				"AI-HUB-" + order.getAccountExternalReferenceCode());

		if (aiHubApplicationJSONObject == null) {
			if (_log.isInfoEnabled()) {
				_log.info(
					"AI Hub application was not found for order " +
						order.getId());
			}

			return;
		}

		OrderItem[] orderItems = order.getOrderItems();

		if (ArrayUtil.isEmpty(orderItems)) {
			return;
		}

		OrderItem orderItem = orderItems[0];

		String skuOptionValue = _getSkuOptionValue(
			"license-usage-type", orderItem.getOptions());

		if (skuOptionValue == null) {
			return;
		}

		String tokensAmount = StringUtil.removeSubstring(
			skuOptionValue, "-lr-tokens");

		_aiHubService.purchaseQuotaPrepaidBlock(
			aiHubApplicationJSONObject.getInt("accountEntryId"),
			new JSONObject(
			).put(
				"size", Long.valueOf(tokensAmount)
			).put(
				"transactionId", order.getId()
			));

		_commerceOrderService.completeOrder(
			null, order.getId(),
			CommerceOrderConstants.ORDER_PAYMENT_STATUS_COMPLETED);

		_setUpSalesforceOpportunity(aiHubApplicationJSONObject, order);
	}

	private String _getSalesforceProjectId(
		JSONObject aiHubApplicationJSONObject) {

		JSONObject orderMetadataJSONObject =
			CommerceOrderUtil.getOrderMetadataJSONObject(
				Order.toDTO(
					String.valueOf(
						aiHubApplicationJSONObject.getJSONObject(
							"orderToAIHubApplication"))));

		return orderMetadataJSONObject.getString("salesforceProjectId");
	}

	private String _getSkuOptionValue(String key, String options) {
		if (options == null) {
			return null;
		}

		try {
			JSONArray optionsJSONArray = new JSONArray(options);

			for (int i = 0; i < optionsJSONArray.length(); i++) {
				JSONObject jsonObject = optionsJSONArray.getJSONObject(i);

				String skuOptionKey = jsonObject.optString("key");

				if (!skuOptionKey.endsWith(key)) {
					continue;
				}

				JSONArray jsonArray = jsonObject.getJSONArray("value");

				return jsonArray.getString(0);
			}
		}
		catch (Exception exception) {
			_log.error("Unable to parse SkuOption options JSON", exception);
		}

		return null;
	}

	private void _setUpSalesforceOpportunity(
			JSONObject aiHubApplicationJSONObject, Order order)
		throws Exception {

		order.setCustomFields(
			() -> HashMapBuilder.put(
				"order-metadata",
				new JSONObject(
				).put(
					"salesforceProjectId",
					_getSalesforceProjectId(aiHubApplicationJSONObject)
				).toString()
			).build());

		BillingAddress billingAddress = order.getBillingAddress();

		JSONObject salesforceOpportunityJSONObject =
			_salesforceService.postSalesforceOpportunity(
				_countryService.getCountryByA2(
					billingAddress.getCountryISOCode()),
				"Subscription", order,
				_userAccountService.getUserAccountByEmailAddress(
					order.getCreatorEmailAddress()));

		if (salesforceOpportunityJSONObject == null) {
			if (_log.isInfoEnabled()) {
				_log.info("Unable to post Salesforce opportunity");
			}

			return;
		}

		_commerceOrderService.patchOrderExternalReferenceCode(
			order.getId(),
			salesforceOpportunityJSONObject.getJSONObject(
				"data"
			).getString(
				"opportunityId"
			));
	}

	private static final Log _log = LogFactory.getLog(
		ObjectActionAIHubTokensRestController.class);

	@Autowired
	private AIHubService _aiHubService;

	@Autowired
	private CommerceOrderService _commerceOrderService;

	@Autowired
	private CountryService _countryService;

	@Autowired
	private SalesforceService _salesforceService;

	@Autowired
	private UserAccountService _userAccountService;

}