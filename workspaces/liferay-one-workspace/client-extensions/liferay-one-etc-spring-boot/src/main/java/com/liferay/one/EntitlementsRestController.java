/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.liferay.client.extension.util.spring.boot3.BaseRestController;
import com.liferay.one.service.EntitlementService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Felipe Veloso
 */
@RequestMapping("/entitlements")
@RestController
public class EntitlementsRestController extends BaseRestController {

	@PostMapping("/generate")
	public void postEntitlementsGenerate(@RequestParam long commerceOrderItemId)
		throws Exception {

		_entitlementService.generateEntitlements(commerceOrderItemId);
	}

	@Autowired
	private EntitlementService _entitlementService;

}