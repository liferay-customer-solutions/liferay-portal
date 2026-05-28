/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.client.extension.util.spring.boot3.client.LiferayOAuth2AccessTokenManager;
import com.liferay.client.extension.util.spring.boot3.service.BaseService;
import com.liferay.one.model.CommerceOrder;
import com.liferay.portal.kernel.util.Validator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Felipe Veloso
 */
@Component
public class CommerceOrderService extends BaseService {

	public CommerceOrder fetchCommerceOrder(long commerceOrderId)
		throws Exception {

		String response = get(
			_getAuthorization(),
			UriComponentsBuilder.fromPath(
				"/o/headless-commerce-admin-order/v1.0/orders/" +
					commerceOrderId
			).build(
			).toUri());

		if (Validator.isNull(response)) {
			return null;
		}

		try {
			return new CommerceOrder(new JSONObject(response));
		}
		catch (Exception exception) {
			_log.error("Unable to parse JSON: " + response, exception);

			return null;
		}
	}

	private String _getAuthorization() {
		return _liferayOAuth2AccessTokenManager.getAuthorization(
			"liferay-one-etc-spring-boot-oahs");
	}

	private static final Log _log = LogFactory.getLog(
		CommerceOrderService.class);

	@Autowired
	private LiferayOAuth2AccessTokenManager _liferayOAuth2AccessTokenManager;

}