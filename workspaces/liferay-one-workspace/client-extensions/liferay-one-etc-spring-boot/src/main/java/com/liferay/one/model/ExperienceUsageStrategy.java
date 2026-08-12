/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.model;

import com.liferay.one.constants.EntitlementConstants;
import com.liferay.portal.kernel.util.Validator;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;

/**
 * @author Felipe Veloso
 */
public class ExperienceUsageStrategy extends BaseUsageStrategy {

	public static final String METRIC_CLIENT_EXTENSIONS_CPU =
		"clientExtensionsCPU";

	public static final String METRIC_CLIENT_EXTENSIONS_RAM =
		"clientExtensionsRAM";

	public static final String METRIC_DATABASE_STORAGE = "databaseStorage";

	public static final String METRIC_DOCUMENT_LIBRARY_AND_BACKUP_STORAGE =
		"documentLibraryAndBackupStorage";

	public static final String METRIC_LOG_STORAGE = "logStorage";

	public static final String METRIC_NETWORK_TRAFFIC = "networkTraffic";

	public ExperienceUsageStrategy(
		List<EntitlementDefinition> entitlementDefinitions,
		List<Entitlement> entitlements, String response) {

		super(_toUsageJSONObject(response));

		Map<Long, String> units = getUnits(entitlementDefinitions);

		for (Entitlement entitlement : entitlements) {
			String name = entitlement.getName();

			if (name.equals(EntitlementConstants.NAME_DATABASE)) {
				_databaseCapacityMax = getMaxCount(
					entitlement, _databaseCapacityMax, units);
			}
			else if (name.equals(EntitlementConstants.NAME_EXTENSIONS_RAM)) {
				_extensionsCapacityRAMMax = getMaxCount(
					entitlement, _extensionsCapacityRAMMax, units);
			}
			else if (name.equals(EntitlementConstants.NAME_EXTENSIONS_VCPU) ||
					 name.equals(EntitlementConstants.NAME_EXTENSIONS_VCPUS)) {

				_extensionsCapacityCPUMax = getMaxCount(
					entitlement, _extensionsCapacityCPUMax, units);
			}
			else if (name.equals(EntitlementConstants.NAME_LOGS)) {
				_logCapacityMax = getMaxCount(
					entitlement, _logCapacityMax, units);
			}
			else if (name.equals(EntitlementConstants.NAME_STORAGE)) {
				_storageCapacityMax = getMaxCount(
					entitlement, _storageCapacityMax, units);
			}
			else if (name.equals(
						EntitlementConstants.NAME_TRAFFIC_NETWORKING)) {

				_networkingCapacityMax = getMaxCount(
					entitlement, _networkingCapacityMax, units);
			}
		}

		JSONObject usageJSONObject = getUsageJSONObject();

		if (usageJSONObject != null) {
			_databaseCapacityGiBUsed = _toGibibytes(
				usageJSONObject.optLong(METRIC_DATABASE_STORAGE));
			_extensionsCapacityCPUUsed = usageJSONObject.optBigDecimal(
				METRIC_CLIENT_EXTENSIONS_CPU, BigDecimal.ZERO);
			_extensionsCapacityRAMGiBUsed = _toGibibytes(
				usageJSONObject.optLong(METRIC_CLIENT_EXTENSIONS_RAM));
			_logCapacityGiBUsed = _toGibibytes(
				usageJSONObject.optLong(METRIC_LOG_STORAGE));
			_networkingCapacityGiBUsed = _toGibibytes(
				usageJSONObject.optLong(METRIC_NETWORK_TRAFFIC));
			_storageCapacityGiBUsed = _toGibibytes(
				usageJSONObject.optLong(
					METRIC_DOCUMENT_LIBRARY_AND_BACKUP_STORAGE));
		}
	}

	@Override
	public JSONObject toJSONObject() {
		return new JSONObject(
		).put(
			METRIC_CLIENT_EXTENSIONS_CPU,
			createUsageJSONObject(
				_extensionsCapacityCPUMax, _extensionsCapacityCPUUsed)
		).put(
			METRIC_CLIENT_EXTENSIONS_RAM,
			createCapacityUsageJSONObject(
				_extensionsCapacityRAMMax, _extensionsCapacityRAMGiBUsed)
		).put(
			METRIC_DATABASE_STORAGE,
			createCapacityUsageJSONObject(
				_databaseCapacityMax, _databaseCapacityGiBUsed)
		).put(
			METRIC_DOCUMENT_LIBRARY_AND_BACKUP_STORAGE,
			createCapacityUsageJSONObject(
				_storageCapacityMax, _storageCapacityGiBUsed)
		).put(
			METRIC_LOG_STORAGE,
			createCapacityUsageJSONObject(_logCapacityMax, _logCapacityGiBUsed)
		).put(
			METRIC_NETWORK_TRAFFIC,
			createCapacityUsageJSONObject(
				_networkingCapacityMax, _networkingCapacityGiBUsed)
		);
	}

	private static JSONObject _toUsageJSONObject(String response) {
		if (Validator.isNull(response)) {
			return null;
		}

		JSONObject jsonObject = new JSONObject(response);

		return jsonObject.optJSONObject("usage");
	}

	private BigDecimal _toGibibytes(long bytes) {
		return new BigDecimal(
			bytes
		).divide(
			_GIB_DIVISOR, 2, RoundingMode.DOWN
		);
	}

	private static final BigDecimal _GIB_DIVISOR = new BigDecimal(
		1024L * 1024L * 1024L);

	private BigDecimal _databaseCapacityGiBUsed = BigDecimal.ZERO;
	private BigDecimal _databaseCapacityMax = BigDecimal.ZERO;
	private BigDecimal _extensionsCapacityCPUMax = BigDecimal.ZERO;
	private BigDecimal _extensionsCapacityCPUUsed = BigDecimal.ZERO;
	private BigDecimal _extensionsCapacityRAMGiBUsed = BigDecimal.ZERO;
	private BigDecimal _extensionsCapacityRAMMax = BigDecimal.ZERO;
	private BigDecimal _logCapacityGiBUsed = BigDecimal.ZERO;
	private BigDecimal _logCapacityMax = BigDecimal.ZERO;
	private BigDecimal _networkingCapacityGiBUsed = BigDecimal.ZERO;
	private BigDecimal _networkingCapacityMax = BigDecimal.ZERO;
	private BigDecimal _storageCapacityGiBUsed = BigDecimal.ZERO;
	private BigDecimal _storageCapacityMax = BigDecimal.ZERO;

}