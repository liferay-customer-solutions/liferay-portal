/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.liferay.client.extension.util.spring.boot3.BaseRestController;
import com.liferay.one.service.CommerceOrderService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ricardo Mariz
 */
@RequestMapping("/commerce-orders")
@RestController
public class CommerceOrdersRestController extends BaseRestController {

	@PostMapping("/{commerceOrderId}/tax-calculate")
	public void postTaxCalculate(
			@PathVariable("commerceOrderId") long commerceOrderId)
		throws Exception {

		_commerceOrderService.taxCalculate(commerceOrderId);
	}

	@Autowired
	private CommerceOrderService _commerceOrderService;

}