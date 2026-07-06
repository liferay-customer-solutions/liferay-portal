/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.liferay.headless.commerce.admin.order.client.dto.v1_0.Order;
import com.liferay.one.constants.CommerceOrderItemConstants;
import com.liferay.one.constants.CommerceProductConstants;
import com.liferay.one.constants.PropertyConstants;
import com.liferay.one.model.OrderItem;
import com.liferay.one.okta.service.OktaService;
import com.liferay.one.service.CommerceOrderItemService;
import com.liferay.one.service.CommerceOrderService;
import com.liferay.one.service.PropertyService;
import com.liferay.portal.kernel.util.Validator;

import java.util.Objects;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Felipe Veloso
 */
@RequestMapping("/object/action/commerce/order/item/update")
@RestController
public class ObjectActionCommerceOrderItemUpdateRestController
	extends OneBaseRestController {

	@PostMapping
	public void post(@RequestBody String json) throws Exception {
		JSONObject jsonObject = new JSONObject(json);

		OrderItem orderItem = _commerceOrderItemService.fetchCommerceOrderItem(
			jsonObject.getLong("classPK"));

		if ((orderItem == null) ||
			!Objects.equals(
				orderItem.getStatus(),
				CommerceOrderItemConstants.STATUS_CANCELED) ||
			!Objects.equals(
				orderItem.getName(),
				CommerceProductConstants.NAME_PAAS_EXPERIENCE)) {

			return;
		}

		Order order = _commerceOrderService.getCommerceOrder(
			orderItem.getOrderId());

		String oktaApplicationId = _propertyService.getPropertyValue(
			order.getAccountId(), PropertyConstants.NAME_OKTA_APPLICATION);

		if (Validator.isNotNull(oktaApplicationId)) {
			_oktaService.deleteApplication(oktaApplicationId);
		}
	}

	@Autowired
	private CommerceOrderItemService _commerceOrderItemService;

	@Autowired
	private CommerceOrderService _commerceOrderService;

	@Autowired
	private OktaService _oktaService;

	@Autowired
	private PropertyService _propertyService;

}