/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.model;

import com.liferay.one.constants.EntitlementConstants;

import java.math.BigDecimal;

import java.util.List;

import org.json.JSONObject;

/**
 * @author Felipe Veloso
 */
public class SaaSUsageStrategy extends BaseUsageStrategy {

	public SaaSUsageStrategy(String response, List<Entitlement> entitlements) {
		super(response);

		for (Entitlement entitlement : entitlements) {
			String name = entitlement.getName();

			if (name.equals(EntitlementConstants.NAME_APV)) {
				_anonymousPageViewsMax = getMaxCount(
					_anonymousPageViewsMax, entitlement);
			}
			else if (name.equals(
						EntitlementConstants.NAME_DOCUMENT_LIBRARY_SIZE)) {

				_storageCapacityMax = getMaxCount(
					_storageCapacityMax, entitlement);
			}
			else if (name.equals(EntitlementConstants.NAME_EXTENSIONS_RAM) ||
					 name.equals(EntitlementConstants.NAME_RAM)) {

				_extensionsCapacityRAMMax = getMaxCount(
					_extensionsCapacityRAMMax, entitlement);
			}
			else if (name.equals(EntitlementConstants.NAME_EXTENSIONS_VCPU) ||
					 name.equals(EntitlementConstants.NAME_VCPU)) {

				_extensionsCapacityCPUMax = getMaxCount(
					_extensionsCapacityCPUMax, entitlement);
			}
			else if (name.equals(EntitlementConstants.NAME_MALU)) {
				_monthlyActiveLoggedInUsersMax = getMaxCount(
					_monthlyActiveLoggedInUsersMax, entitlement);
			}
			else if (name.equals(EntitlementConstants.NAME_SITES)) {
				_sitesMax = getMaxCount(_sitesMax, entitlement);
			}
		}

		JSONObject usageJSONObject = getUsageJSONObject();

		if (usageJSONObject != null) {
			_anonymousPageViewsUsed = usageJSONObject.optBigDecimal(
				"totalAnonymousPageViewsCount", BigDecimal.ZERO);
			_extensionsCapacityCPUUsed = usageJSONObject.optBigDecimal(
				"totalClientExtensionsCapacityCPUCount", BigDecimal.ZERO);
			_extensionsCapacityRAMGiBUsed = usageJSONObject.optBigDecimal(
				"totalClientExtensionsCapacityRAM", BigDecimal.ZERO);
			_monthlyActiveLoggedInUsersUsed = usageJSONObject.optBigDecimal(
				"totalMonthlyActiveLoggedInUsersCount", BigDecimal.ZERO);
			_sitesUsed = usageJSONObject.optBigDecimal(
				"totalSitesCount", BigDecimal.ZERO);
			_storageCapacityGiBUsed = usageJSONObject.optBigDecimal(
				"totalStorageCapacityDocumentLibrary", BigDecimal.ZERO);
		}
	}

	@Override
	public JSONObject toJSONObject() {
		return new JSONObject(
		).put(
			"anonymousPageViews",
			createUsageJSONObject(
				_anonymousPageViewsMax, _anonymousPageViewsUsed)
		).put(
			"clientExtensionsCapacityCPU",
			createUsageJSONObject(
				_extensionsCapacityCPUMax, _extensionsCapacityCPUUsed)
		).put(
			"clientExtensionsCapacityRAM",
			createCapacityUsageJSONObject(
				_extensionsCapacityRAMMax, _extensionsCapacityRAMGiBUsed)
		).put(
			"monthlyActiveLoggedInUsers",
			createUsageJSONObject(
				_monthlyActiveLoggedInUsersMax, _monthlyActiveLoggedInUsersUsed)
		).put(
			"sites", createUsageJSONObject(_sitesMax, _sitesUsed)
		).put(
			"storageCapacityDocumentLibrary",
			createCapacityUsageJSONObject(
				_storageCapacityMax, _storageCapacityGiBUsed)
		);
	}

	private BigDecimal _anonymousPageViewsMax = BigDecimal.ZERO;
	private BigDecimal _anonymousPageViewsUsed = BigDecimal.ZERO;
	private BigDecimal _extensionsCapacityCPUMax = BigDecimal.ZERO;
	private BigDecimal _extensionsCapacityCPUUsed = BigDecimal.ZERO;
	private BigDecimal _extensionsCapacityRAMGiBUsed = BigDecimal.ZERO;
	private BigDecimal _extensionsCapacityRAMMax = BigDecimal.ZERO;
	private BigDecimal _monthlyActiveLoggedInUsersMax = BigDecimal.ZERO;
	private BigDecimal _monthlyActiveLoggedInUsersUsed = BigDecimal.ZERO;
	private BigDecimal _sitesMax = BigDecimal.ZERO;
	private BigDecimal _sitesUsed = BigDecimal.ZERO;
	private BigDecimal _storageCapacityGiBUsed = BigDecimal.ZERO;
	private BigDecimal _storageCapacityMax = BigDecimal.ZERO;

}