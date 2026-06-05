/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.one.model.EntitlementDefinition;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * @author Felipe Veloso
 */
@Component
public class EntitlementDefinitionService extends OneBaseService {

	public List<EntitlementDefinition> getEntitlementDefinitions(
			String filterString, Map<String, String> productOptions)
		throws Exception {

		List<EntitlementDefinition> entitlementDefinitions = getAllItems(
			"/o/c/entitlementdefinitions", filterString,
			EntitlementDefinition::new);

		Iterator<EntitlementDefinition> iterator =
			entitlementDefinitions.iterator();

		while (iterator.hasNext()) {
			EntitlementDefinition entitlementDefinition = iterator.next();

			if (!_matches(
					entitlementDefinition.getProductOptions(),
					productOptions)) {

				iterator.remove();
			}
		}

		return entitlementDefinitions;
	}

	private boolean _matches(
		Map<String, String> entitlementDefinitionProductOptions,
		Map<String, String> productOptions) {

		for (Map.Entry<String, String> entry :
				entitlementDefinitionProductOptions.entrySet()) {

			String value = entry.getValue();

			if (!value.equals(productOptions.get(entry.getKey()))) {
				return false;
			}
		}

		return true;
	}

}