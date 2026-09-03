/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.liferay.one.permission.CommerceOrderPermission;
import com.liferay.one.service.CloudAppService;
import com.liferay.one.service.CommerceOrderService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ricardo Mariz
 */
@RequestMapping("/commerce-orders")
@RestController
public class CommerceOrdersRestController extends OneBaseRestController {

	@PostMapping("/{commerceOrderId}/calculate-tax")
	public void postCalculateTax(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("commerceOrderId") long commerceOrderId)
		throws Exception {

		_commerceOrderPermission.check(commerceOrderId, jwt);

		_commerceOrderService.calculateTax(commerceOrderId);
	}

	@PostMapping("/{commerceOrderId}/complete-cloud-app")
	public void postCompleteCloudApp(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("commerceOrderId") long commerceOrderId)
		throws Exception {

		_commerceOrderPermission.check(commerceOrderId, jwt);

		_cloudAppService.completeCloudAppOrder(commerceOrderId);
	}

	@PostMapping("/{commerceOrderId}/complete-settled")
	public void postCompleteSettled(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("commerceOrderId") long commerceOrderId)
		throws Exception {

		_commerceOrderPermission.check(commerceOrderId, jwt);

		_commerceOrderService.completeSettledOrder(commerceOrderId);
	}

	@Autowired
	private CloudAppService _cloudAppService;

	@Autowired
	private CommerceOrderPermission _commerceOrderPermission;

	@Autowired
	private CommerceOrderService _commerceOrderService;

}