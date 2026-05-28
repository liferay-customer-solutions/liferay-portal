/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.client.extension.util.spring.boot3.client.LiferayOAuth2AccessTokenManager;
import com.liferay.client.extension.util.spring.boot3.service.BaseService;
import com.liferay.one.model.CommerceOrderItem;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Felipe Veloso
 */
@Component
public class CommerceOrderItemService extends BaseService {

	public CommerceOrderItem fetchCommerceOrderItem(long commerceOrderItemId)
		throws Exception {

		String response = get(
			_getAuthorization(),
			UriComponentsBuilder.fromPath(
				"/o/headless-commerce-admin-order/v1.0/orderItems/" +
					commerceOrderItemId
			).queryParam(
				"nestedFields", "customFields"
			).build(
			).toUri());

		if (Validator.isNull(response)) {
			return null;
		}

		return new CommerceOrderItem(new JSONObject(response));
	}

	public List<CommerceOrderItem> getCommerceOrderItems(int page, int pageSize)
		throws Exception {

		String response = get(
			_getAuthorization(),
			UriComponentsBuilder.fromPath(
				"/o/headless-commerce-admin-order/v1.0/orderItems"
			).queryParam(
				"nestedFields", "customFields"
			).queryParam(
				"page", page
			).queryParam(
				"pageSize", pageSize
			).build(
			).toUri());

		List<CommerceOrderItem> commerceOrderItems = new ArrayList<>();

		if (Validator.isNull(response)) {
			return commerceOrderItems;
		}

		try {
			JSONObject jsonObject = new JSONObject(response);

			JSONArray jsonArray = jsonObject.getJSONArray("items");

			for (int i = 0; i < jsonArray.length(); i++) {
				commerceOrderItems.add(
					new CommerceOrderItem(jsonArray.getJSONObject(i)));
			}

			return commerceOrderItems;
		}
		catch (Exception exception) {
			_log.error("Unable to parse JSON: " + response, exception);

			return commerceOrderItems;
		}
	}

	private String _getAuthorization() {
		return _liferayOAuth2AccessTokenManager.getAuthorization(
			"liferay-one-etc-spring-boot-oahs");
	}

	private static final Log _log = LogFactory.getLog(
		CommerceOrderItemService.class);

	@Autowired
	private LiferayOAuth2AccessTokenManager _liferayOAuth2AccessTokenManager;

}