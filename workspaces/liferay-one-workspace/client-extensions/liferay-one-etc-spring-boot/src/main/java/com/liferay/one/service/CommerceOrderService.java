/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.admin.user.client.dto.v1_0.Account;
import com.liferay.one.constants.SupportRegionConstants;
import com.liferay.one.model.CommerceOrder;
import com.liferay.one.util.SupportRegionUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Felipe Veloso
 */
@Component
public class CommerceOrderService extends OneBaseService {

	public CommerceOrder fetchCommerceOrder(long commerceOrderId)
		throws Exception {

		String response = get(
			getAuthorization(),
			UriComponentsBuilder.fromPath(
				"/o/headless-commerce-admin-order/v1.0/orders/" +
					commerceOrderId
			).queryParam(
				"nestedFields", "customFields"
			).build(
			).toUri());

		if (Validator.isNull(response)) {
			return null;
		}

		return new CommerceOrder(new JSONObject(response));
	}

	public String getSupportRegion(Account account) throws Exception {
		Long defaultBillingAddressId = account.getDefaultBillingAddressId();

		String addressCountry = null;

		if (Validator.isNotNull(defaultBillingAddressId)) {
			addressCountry = _postalAddressService.getAddressCountry(
				defaultBillingAddressId);
		}

		for (CommerceOrder commerceOrder : getCommerceOrders(account.getId())) {
			String opportunitySoldBy = commerceOrder.getOpportunitySoldBy();

			if (Validator.isNull(opportunitySoldBy)) {
				continue;
			}

			return SupportRegionUtil.getSupportRegion(
				opportunitySoldBy, addressCountry);
		}

		return SupportRegionConstants.GLOBAL;
	}

	protected List<CommerceOrder> getCommerceOrders(long accountId)
		throws Exception {

		List<CommerceOrder> commerceOrders = new ArrayList<>();

		int page = 1;

		while (true) {
			String response = get(
				getAuthorization(),
				UriComponentsBuilder.fromPath(
					"/o/headless-commerce-admin-order/v1.0/orders"
				).queryParam(
					"filter", "accountId/any(x:x eq " + accountId + ")"
				).queryParam(
					"nestedFields", "customFields"
				).queryParam(
					"page", page
				).queryParam(
					"pageSize", _PAGE_SIZE
				).build(
				).toUri());

			if (Validator.isNull(response)) {
				return commerceOrders;
			}

			JSONObject jsonObject = new JSONObject(response);

			JSONArray jsonArray = jsonObject.optJSONArray("items");

			if (jsonArray == null) {
				return commerceOrders;
			}

			for (int i = 0; i < jsonArray.length(); i++) {
				commerceOrders.add(
					new CommerceOrder(jsonArray.getJSONObject(i)));
			}

			if (jsonArray.length() < _PAGE_SIZE) {
				return commerceOrders;
			}

			page++;
		}
	}

	private static final int _PAGE_SIZE = 200;

	@Autowired
	private PostalAddressService _postalAddressService;

}