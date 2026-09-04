/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.one.constants.EntitlementConstants;
import com.liferay.one.model.EntitlementDefinition;
import com.liferay.petra.string.StringBundler;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;

/**
 * @author Felipe Veloso
 */
@Component
public class EntitlementDefinitionService extends OneBaseService {

	public EntitlementDefinition fetchEntitlementDefinition(
			String externalReferenceCode)
		throws Exception {

		List<EntitlementDefinition> entitlementDefinitions =
			getEntitlementDefinitions(
				"externalReferenceCode eq '" + externalReferenceCode + "'");

		if (entitlementDefinitions.isEmpty()) {
			return null;
		}

		return entitlementDefinitions.get(0);
	}

	/**
	 * The definition of the overage bucket sold for a usage definition. Every
	 * way of buying a metric hangs off the same usage definition, so the
	 * overage bucket is the sibling definition typed as an overage. It is what
	 * names the SKU an overage order is placed against.
	 */
	public EntitlementDefinition fetchOverageEntitlementDefinition(
			long usageDefinitionId)
		throws Exception {

		for (EntitlementDefinition entitlementDefinition :
				getEntitlementDefinitions(
					StringBundler.concat(
						"r_usageDefinitionToEntitlementDefinition_",
						"c_usageDefinitionId eq '", usageDefinitionId, "'"))) {

			if (entitlementDefinition.isActive() &&
				Objects.equals(
					entitlementDefinition.getType(),
					EntitlementConstants.TYPE_OVERAGE)) {

				return entitlementDefinition;
			}
		}

		return null;
	}

	public List<EntitlementDefinition> getEntitlementDefinitions(
			String filterString)
		throws Exception {

		return getAllItems(
			"/o/c/entitlementdefinitions", filterString,
			EntitlementDefinition::new);
	}

	public List<EntitlementDefinition> getEntitlementDefinitions(
			String filterString, Map<String, String> productOptions)
		throws Exception {

		List<EntitlementDefinition> entitlementDefinitions =
			getEntitlementDefinitions(filterString);

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