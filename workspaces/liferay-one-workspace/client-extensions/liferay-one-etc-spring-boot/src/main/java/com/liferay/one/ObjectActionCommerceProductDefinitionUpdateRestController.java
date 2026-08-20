/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.liferay.one.service.EntitlementDefinitionService;
import com.liferay.portal.kernel.workflow.WorkflowConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Felipe Veloso
 */
@RequestMapping("/object/action/commerce/product/definition/update")
@RestController
public class ObjectActionCommerceProductDefinitionUpdateRestController
	extends OneBaseRestController {

	@PostMapping
	public void post(@RequestBody String json) throws Exception {
		JSONObject jsonObject = new JSONObject(json);

		JSONObject modelCPDefinitionJSONObject = jsonObject.optJSONObject(
			"modelCPDefinition");

		if (modelCPDefinitionJSONObject == null) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					"Unable to find the CPDefinition model attributes for " +
						"class PK " + jsonObject.optLong("classPK"));
			}

			return;
		}

		if (modelCPDefinitionJSONObject.optInt("status", -1) !=
				WorkflowConstants.STATUS_APPROVED) {

			return;
		}

		_entitlementDefinitionService.generateEntitlementDefinition(
			modelCPDefinitionJSONObject.getLong("CProductId"));
	}

	private static final Log _log = LogFactory.getLog(
		ObjectActionCommerceProductDefinitionUpdateRestController.class);

	@Autowired
	private EntitlementDefinitionService _entitlementDefinitionService;

}