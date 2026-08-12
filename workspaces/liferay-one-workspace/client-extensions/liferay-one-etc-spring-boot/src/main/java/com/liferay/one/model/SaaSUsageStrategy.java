/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.model;

import com.liferay.one.constants.EntitlementConstants;
import com.liferay.portal.kernel.util.Validator;

import java.math.BigDecimal;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;

/**
 * @author Felipe Veloso
 */
public class SaaSUsageStrategy extends BaseUsageStrategy {

	public static final String METRIC_ANONYMOUS_PAGE_VIEWS =
		"anonymousPageViews";

	public static final String METRIC_CLIENT_EXTENSIONS_CAPACITY_CPU =
		"clientExtensionsCapacityCPU";

	public static final String METRIC_CLIENT_EXTENSIONS_CAPACITY_RAM =
		"clientExtensionsCapacityRAM";

	public static final String METRIC_MONTHLY_ACTIVE_LOGGED_IN_USERS =
		"monthlyActiveLoggedInUsers";

	public static final String METRIC_SITES = "sites";

	public static final String METRIC_STORAGE_CAPACITY_DOCUMENT_LIBRARY =
		"storageCapacityDocumentLibrary";

	public SaaSUsageStrategy(
		List<EntitlementDefinition> entitlementDefinitions,
		List<Entitlement> entitlements, String response) {

		super(_toUsageJSONObject(response));

		Map<Long, String> units = getUnits(entitlementDefinitions);

		for (Entitlement entitlement : entitlements) {
			String name = entitlement.getName();

			if (name.equals(EntitlementConstants.NAME_APV)) {
				_anonymousPageViewsMax = getMaxCount(
					entitlement, _anonymousPageViewsMax, units);
			}
			else if (name.equals(
						EntitlementConstants.NAME_DOCUMENT_LIBRARY_SIZE)) {

				_storageCapacityMax = getMaxCount(
					entitlement, _storageCapacityMax, units);
			}
			else if (name.equals(EntitlementConstants.NAME_EXTENSIONS_RAM) ||
					 name.equals(EntitlementConstants.NAME_RAM)) {

				_extensionsCapacityRAMMax = getMaxCount(
					entitlement, _extensionsCapacityRAMMax, units);
			}
			else if (name.equals(EntitlementConstants.NAME_EXTENSIONS_VCPU) ||
					 name.equals(EntitlementConstants.NAME_VCPU)) {

				_extensionsCapacityCPUMax = getMaxCount(
					entitlement, _extensionsCapacityCPUMax, units);
			}
			else if (name.equals(EntitlementConstants.NAME_MALU)) {
				_monthlyActiveLoggedInUsersMax = getMaxCount(
					entitlement, _monthlyActiveLoggedInUsersMax, units);
			}
			else if (name.equals(EntitlementConstants.NAME_SITES)) {
				_sitesMax = getMaxCount(entitlement, _sitesMax, units);
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
			METRIC_ANONYMOUS_PAGE_VIEWS,
			createUsageJSONObject(
				_anonymousPageViewsMax, _anonymousPageViewsUsed)
		).put(
			METRIC_CLIENT_EXTENSIONS_CAPACITY_CPU,
			createUsageJSONObject(
				_extensionsCapacityCPUMax, _extensionsCapacityCPUUsed)
		).put(
			METRIC_CLIENT_EXTENSIONS_CAPACITY_RAM,
			createCapacityUsageJSONObject(
				_extensionsCapacityRAMMax, _extensionsCapacityRAMGiBUsed)
		).put(
			METRIC_MONTHLY_ACTIVE_LOGGED_IN_USERS,
			createUsageJSONObject(
				_monthlyActiveLoggedInUsersMax, _monthlyActiveLoggedInUsersUsed)
		).put(
			METRIC_SITES, createUsageJSONObject(_sitesMax, _sitesUsed)
		).put(
			METRIC_STORAGE_CAPACITY_DOCUMENT_LIBRARY,
			createCapacityUsageJSONObject(
				_storageCapacityMax, _storageCapacityGiBUsed)
		);
	}

	private static JSONObject _toUsageJSONObject(String response) {
		if (Validator.isNull(response)) {
			return null;
		}

		return new JSONObject(response);
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