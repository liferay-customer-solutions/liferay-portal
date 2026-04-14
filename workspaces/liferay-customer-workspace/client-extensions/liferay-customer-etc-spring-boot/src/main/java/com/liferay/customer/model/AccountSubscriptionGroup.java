/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.customer.model;

import com.liferay.customer.constants.AccountSubscriptionGroupConstants;
import com.liferay.customer.constants.ExternalLinkDomain;
import com.liferay.customer.constants.ExternalLinkEntityName;
import com.liferay.osb.koroneiki.phloem.rest.client.dto.v1_0.ExternalLink;
import com.liferay.osb.koroneiki.phloem.rest.client.dto.v1_0.Product;
import com.liferay.osb.koroneiki.phloem.rest.client.dto.v1_0.ProductPurchase;
import com.liferay.petra.string.StringPool;
import com.liferay.petra.string.StringUtil;
import com.liferay.portal.kernel.util.ArrayUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author Felipe Veloso
 */
public class AccountSubscriptionGroup {

	public AccountSubscriptionGroup(
			String name, List<ProductPurchase> productPurchases,
			String accountKey, ExternalLink[] externalLinks,
			String manageUrlLiferayPaas)
		throws Exception {

		this.name = name;
		this.accountKey = accountKey;
		this.externalLinks = externalLinks;

		_manageUrlLiferayPaas = manageUrlLiferayPaas;

		externalReferenceCode = _getExternalReferenceCode();

		Map<String, List<ProductPurchase>> productPurchasesMap =
			new HashMap<>();

		for (ProductPurchase productPurchase : productPurchases) {
			Product product = productPurchase.getProduct();

			String productKey = product.getKey();

			productPurchasesMap.computeIfAbsent(
				productKey, k -> new ArrayList<>()
			).add(
				productPurchase
			);

			Map<String, String> properties = product.getProperties();

			String displayActivationName = properties.getOrDefault(
				"display-activation-name", StringPool.BLANK);

			if (!displayActivationName.isEmpty()) {
				activationProductNames.add(displayActivationName);
			}
		}

		for (Map.Entry<String, List<ProductPurchase>> entry :
				productPurchasesMap.entrySet()) {

			accountSubscriptionsMap.computeIfAbsent(
				entry.getKey(),
				k -> new AccountSubscription(
					accountKey, name, entry.getValue()));
		}

		if (activationProductNames.contains(
				AccountSubscriptionGroupConstants.
					ACTIVATION_PRODUCT_NAME_LIFERAY_PAAS) ||
			activationProductNames.contains(
				AccountSubscriptionGroupConstants.
					ACTIVATION_PRODUCT_NAME_LIFERAY_SAAS)) {

			activationProductNames.remove(
				AccountSubscriptionGroupConstants.
					ACTIVATION_PRODUCT_NAME_CLOUD_NATIVE);
		}
	}

	public JSONObject toJSONObject() {
		JSONObject jsonObject = new JSONObject();

		jsonObject.put("accountKey", accountKey);

		String activationProductName = StringUtil.merge(
			activationProductNames, StringPool.COMMA);

		if (name.equals(
				AccountSubscriptionGroupConstants.NAME_LIFERAY_SELF_HOSTED)) {

			activationProductName =
				AccountSubscriptionGroupConstants.ACTIVATION_PRODUCT_NAME_DXP;
		}

		jsonObject.put(
			"accountSubscriptions", _getAccountSubscriptionJSONArray()
		).put(
			"activationProductName", activationProductName
		).put(
			"externalReferenceCode", externalReferenceCode
		).put(
			"hasActivation",
			ArrayUtil.contains(
				AccountSubscriptionGroupConstants.NAMES_ACTIVATION, name)
		).put(
			"manageContactsURL", _getManageURLs()
		).put(
			"menuOrder", _getMenuOrder()
		).put(
			"name", name
		).put(
			"r_accountEntryToAccountSubscriptionGroup_accountEntryERC",
			accountKey
		).put(
			"tabOrder", _getTabOrder()
		);

		return jsonObject;
	}

	public String accountKey;
	public Map<String, AccountSubscription> accountSubscriptionsMap =
		new HashMap<>();
	public Set<String> activationProductNames = new TreeSet<>();
	public ExternalLink[] externalLinks;
	public String externalReferenceCode;
	public String name;

	private JSONArray _getAccountSubscriptionJSONArray() {
		JSONArray jsonArray = new JSONArray();

		for (AccountSubscription accountSubscription :
				accountSubscriptionsMap.values()) {

			jsonArray.put(accountSubscription.toJSONObject());
		}

		return jsonArray;
	}

	private String _getExternalLinkURL(
		ExternalLink[] externalLinks, String domain, String entityName) {

		if (ArrayUtil.isEmpty(externalLinks)) {
			return StringPool.BLANK;
		}

		for (ExternalLink externalLink : externalLinks) {
			if (domain.equals(externalLink.getDomain()) &&
				entityName.equals(externalLink.getEntityName())) {

				return externalLink.getUrl();
			}
		}

		return StringPool.BLANK;
	}

	private String _getExternalReferenceCode() {
		String nameFormatted = name.toLowerCase(
		).replace(
			StringPool.SPACE, StringPool.DASH
		);

		return accountKey + StringPool.UNDERLINE + nameFormatted;
	}

	private String _getManageURLs() {
		JSONObject manageURLsJSONObject = new JSONObject();

		if (name.equals(
				AccountSubscriptionGroupConstants.NAME_ANALYTICS_CLOUD)) {

			manageURLsJSONObject.put(
				name,
				_getExternalLinkURL(
					externalLinks, ExternalLinkDomain.ANALYTICS_CLOUD,
					ExternalLinkEntityName.ANALYTICS_CLOUD_GROUP));
		}
		else if (name.equals(
					AccountSubscriptionGroupConstants.NAME_LIFERAY_CLOUD)) {

			if (activationProductNames.contains(
					AccountSubscriptionGroupConstants.
						ACTIVATION_PRODUCT_NAME_LIFERAY_PAAS)) {

				manageURLsJSONObject.put(
					AccountSubscriptionGroupConstants.
						ACTIVATION_PRODUCT_NAME_LIFERAY_PAAS,
					_manageUrlLiferayPaas);
			}

			if (activationProductNames.contains(
					AccountSubscriptionGroupConstants.
						ACTIVATION_PRODUCT_NAME_LIFERAY_SAAS)) {

				manageURLsJSONObject.put(
					AccountSubscriptionGroupConstants.
						ACTIVATION_PRODUCT_NAME_LIFERAY_SAAS,
					_getExternalLinkURL(
						externalLinks, ExternalLinkDomain.LXC,
						ExternalLinkEntityName.LXC_PROJECT));
			}
		}

		return manageURLsJSONObject.toString() + StringPool.SPACE;
	}

	private int _getMenuOrder() {
		Map<String, Integer> menuOrders =
			AccountSubscriptionGroupConstants.menuOrders;

		return menuOrders.getOrDefault(
			name, AccountSubscriptionGroupConstants.ORDER_DEFAULT);
	}

	private int _getTabOrder() {
		Map<String, Integer> tabOrders =
			AccountSubscriptionGroupConstants.tabOrders;

		return tabOrders.getOrDefault(
			name, AccountSubscriptionGroupConstants.ORDER_DEFAULT);
	}

	private final String _manageUrlLiferayPaas;

}