/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.customer.model;

import com.liferay.customer.constants.AccountSubscriptionConstants;
import com.liferay.osb.koroneiki.phloem.rest.client.dto.v1_0.Product;
import com.liferay.osb.koroneiki.phloem.rest.client.dto.v1_0.ProductPurchase;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.util.Validator;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONObject;

/**
 * @author Felipe Veloso
 */
public class AccountSubscription {

	public AccountSubscription(
		String accountKey, String displayGroupName,
		List<ProductPurchase> productPurchases) {

		this.accountKey = accountKey;

		displayGroupName = displayGroupName.toLowerCase(
		).replace(
			StringPool.SPACE, StringPool.DASH
		);

		accountSubscriptionGroupExternalReferenceCode =
			accountKey + StringPool.UNDERLINE + displayGroupName;

		for (ProductPurchase productPurchase : productPurchases) {
			if (Validator.isNull(productKey)) {
				Product product = productPurchase.getProduct();

				Map<String, String> properties = product.getProperties();

				name = properties.getOrDefault(
					"display-name", product.getName());

				productKey = product.getKey();

				externalReferenceCode =
					accountSubscriptionGroupExternalReferenceCode +
						StringPool.UNDERLINE + productKey;
			}

			Date now = new Date();
			Date newStartDate = productPurchase.getStartDate();
			Date newEndDate = productPurchase.getEndDate();
			Date newOriginalEndDate =
				(productPurchase.getOriginalEndDate() != null) ?
					productPurchase.getOriginalEndDate() :
						productPurchase.getEndDate();

			if ((startDate == null) ||
				((newStartDate != null) && startDate.after(newStartDate))) {

				startDate = newStartDate;
			}

			if ((newStartDate != null) && now.before(newStartDate) &&
				((upcomingStartDate == null) ||
				 upcomingStartDate.after(newStartDate))) {

				upcomingStartDate = newStartDate;
			}

			if ((endDate == null) ||
				((newEndDate != null) && endDate.before(newEndDate))) {

				endDate = newEndDate;
			}

			if ((originalEndDate == null) ||
				((newOriginalEndDate != null) &&
				 originalEndDate.before(newOriginalEndDate))) {

				originalEndDate = newOriginalEndDate;
			}

			Map<String, String> properties = productPurchase.getProperties();

			int newInstanceSize = _parseSize(
				(properties != null) ? properties.get("sizing") : null);

			String newStatus = _getStatus(newEndDate, newStartDate);

			if (Validator.isNull(status)) {
				status = newStatus;
				instanceSize = newInstanceSize;
			}
			else if (status.equals(newStatus)) {
				if (newInstanceSize > instanceSize) {
					instanceSize = newInstanceSize;
				}
			}
			else if (newStatus.equals(
						AccountSubscriptionConstants.STATUS_ACTIVE) &&
					 !status.equals(
						 AccountSubscriptionConstants.STATUS_ACTIVE)) {

				status = newStatus;
				instanceSize = newInstanceSize;
			}
			else if (newStatus.equals(
						AccountSubscriptionConstants.STATUS_FUTURE) &&
					 status.equals(
						 AccountSubscriptionConstants.STATUS_EXPIRED)) {

				status = newStatus;
				instanceSize = newInstanceSize;
			}
		}
	}

	public JSONObject toJSONObject() {
		JSONObject jsonObject = new JSONObject();

		DateFormat dateFormat = new SimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm:ss'Z'");

		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

		jsonObject.put(
			"accountKey", accountKey
		).put(
			"accountSubscriptionGroupERC",
			accountSubscriptionGroupExternalReferenceCode
		);

		if (originalEndDate != null) {
			jsonObject.put("endDate", dateFormat.format(originalEndDate));
		}

		jsonObject.put("externalReferenceCode", externalReferenceCode);

		if (instanceSize > 0) {
			jsonObject.put("instanceSize", String.valueOf(instanceSize));
		}

		jsonObject.put(
			"name", name
		).put(
			"productKey", productKey
		).put(
			"r_accountEntryToAccountSubscription_accountEntryERC", accountKey
		);

		if (status.equals(AccountSubscriptionConstants.STATUS_FUTURE) &&
			(upcomingStartDate != null)) {

			jsonObject.put("startDate", dateFormat.format(upcomingStartDate));
		}
		else if (startDate != null) {
			jsonObject.put("startDate", dateFormat.format(startDate));
		}

		return jsonObject;
	}

	public String accountKey;
	public String accountSubscriptionGroupExternalReferenceCode;
	public Date endDate;
	public String externalReferenceCode;
	public int instanceSize;
	public String name;
	public Date originalEndDate;
	public String productKey;
	public Date startDate;
	public String status;
	public Date upcomingStartDate;

	private String _getStatus(Date endDate, Date startDate) {
		Date now = new Date();

		if ((endDate != null) && endDate.before(now)) {
			return "Expired";
		}

		if ((startDate != null) && startDate.after(now)) {
			return "Future";
		}

		return "Active";
	}

	private int _parseSize(String size) {
		if ((size == null) || size.isEmpty()) {
			return 0;
		}

		try {
			return Integer.parseInt(size);
		}
		catch (NumberFormatException numberFormatException) {
			_log.error(
				"Failed to parse subscription size: " + externalReferenceCode,
				numberFormatException);
		}

		return 0;
	}

	private static final Log _log = LogFactory.getLog(
		AccountSubscription.class);

}