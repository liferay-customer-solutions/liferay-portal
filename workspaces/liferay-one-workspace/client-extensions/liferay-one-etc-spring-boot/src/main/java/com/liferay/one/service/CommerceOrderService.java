/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.one.model.CommerceOrder;
import com.liferay.portal.kernel.util.Validator;

import org.json.JSONObject;

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

}