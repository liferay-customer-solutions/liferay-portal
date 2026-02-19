/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.customer.model;

import com.liferay.customer.constants.ProductConstants;
import com.liferay.osb.koroneiki.phloem.rest.client.dto.v1_0.Product;
import com.liferay.osb.koroneiki.phloem.rest.client.dto.v1_0.ProductPurchase;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

/**
 * @author Amos Fong
 */
public class AccountUsage {

	public AccountUsage(
		List<ProductPurchase> productPurchases, JSONObject usageJSONObject) {

		ProductPurchase usageProductPurchase = null;

		for (ProductPurchase productPurchase : productPurchases) {
			Product product = productPurchase.getProduct();

			String name = product.getName();

			if (name.equals(ProductConstants.NAME_PRODUCTION_ENVIRONMENT)) {
				usageProductPurchase = productPurchase;
			}
		}

		_initExperiencePlan(usageProductPurchase);

		_initUsage(usageJSONObject);
	}

	public JSONObject toJSONObject() {
		JSONObject jsonObject = new JSONObject();

		jsonObject.put(
			"clientExtensionsCPU",
			_getUsageJSONObject(
				_clientExtensionsCapacityCPUUsed, StringPool.BLANK,
				_clientExtensionsCapacityCPUMax, StringPool.BLANK)
		).put(
			"clientExtensionsRAM",
			_getUsageJSONObject(
				_clientExtensionsCapacityRAMUsed,
				_usageUnits.get(_METRIC_CLIENT_EXTENSIONS_RAM),
				_clientExtensionsCapacityRAMMax, _UNIT_GIB)
		).put(
			"databaseStorage",
			_getUsageJSONObject(
				_databaseCapacityUsed,
				_usageUnits.get(_METRIC_DATABASE_STORAGE), _databaseCapacityMax,
				_UNIT_TIB)
		).put(
			"documentLibraryAndBackupStorage",
			_getUsageJSONObject(
				_storageCapacityDocumentLibraryUsed,
				_usageUnits.get(_METRIC_DOCUMENT_LIBRARY),
				_storageCapacityDocumentLibraryMax, _UNIT_GIB)
		).put(
			"logStorage",
			_getUsageJSONObject(
				_monthlyActiveLoggedInUsersUsed,
				_usageUnits.get(_METRIC_LOG_STORAGE),
				_monthlyActiveLoggedInUsersMax, _UNIT_GIB)
		).put(
			"networkTraffic",
			_getUsageJSONObject(
				_networkingCapacityUsed,
				_usageUnits.get(_METRIC_NETWORK_TRAFFIC),
				_networkingCapacityMax, _networkingCapacityUsedUnit)
		);

		return jsonObject;
	}

	private float _format(BigDecimal bigDecimal, String metric) {
		if (bigDecimal != null) {
			String usageMetric = _UNIT_GIB;

			BigDecimal divisorGB = new BigDecimal(1024L * 1024L * 1024L);

			bigDecimal = bigDecimal.divide(divisorGB);

			if (bigDecimal.compareTo(new BigDecimal("1024")) >= 0) {
				bigDecimal = bigDecimal.divide(new BigDecimal(1024));
				usageMetric = _UNIT_TIB;
			}

			_usageUnits.put(metric, usageMetric);

			return bigDecimal.setScale(
				2, RoundingMode.DOWN
			).floatValue();
		}

		return BigDecimal.ZERO.floatValue();
	}

	private JSONObject _getUsageJSONObject(
		float usedCount, String usedCountUnits, long maxCount,
		String maxCountUnits) {

		float dividend = usedCount;

		if (!usedCountUnits.equals(maxCountUnits)) {
			dividend = usedCount / 1024;
		}

		float percentage = (dividend / maxCount) * 100;

		JSONObject jsonObject = new JSONObject();

		jsonObject.put(
			"maxCount", maxCount
		).put(
			"maxCountUnits", maxCountUnits
		).put(
			"percentage", percentage
		).put(
			"usedCount", usedCount
		).put(
			"usedCountUnits", usedCountUnits
		);

		return jsonObject;
	}

	private void _initExperiencePlan(ProductPurchase productPurchase) {
		if (productPurchase == null) {
			return;
		}

		Map<String, String> productPurchaseProperties =
			productPurchase.getProperties();

		String machineType = StringUtil.toLowerCase(
			GetterUtil.getString(productPurchaseProperties.get("machineType")));

		Product product = productPurchase.getProduct();

		Map<String, String> productProperties = product.getProperties();

		_clientExtensionsCapacityCPUMax = GetterUtil.getInteger(
			productProperties.get(machineType + "-extensions-vcpus"));

		_clientExtensionsCapacityRAMMax = GetterUtil.getInteger(
			productProperties.get(machineType + "-extensions-ram"));

		String databaseCapacityMaxPropertyValue = productProperties.get(
			machineType + "-database");

		databaseCapacityMaxPropertyValue = StringUtil.removeSubstring(
			databaseCapacityMaxPropertyValue, _UNIT_GIB);

		_databaseCapacityMax = GetterUtil.getInteger(
			databaseCapacityMaxPropertyValue);

		String monthlyActiveLoggedInUsersMaxPropertyValue =
			productProperties.get(machineType + "-logs");

		monthlyActiveLoggedInUsersMaxPropertyValue = StringUtil.removeSubstring(
			monthlyActiveLoggedInUsersMaxPropertyValue, _UNIT_GIB);

		_monthlyActiveLoggedInUsersMax = GetterUtil.getInteger(
			monthlyActiveLoggedInUsersMaxPropertyValue);

		String networkingCapacityMaxPropertyValue = productProperties.get(
			machineType + "-traffic-networking");

		if (machineType.equals("high")) {
			networkingCapacityMaxPropertyValue = StringUtil.removeSubstring(
				networkingCapacityMaxPropertyValue, _UNIT_TIB);
			_networkingCapacityUsedUnit = _UNIT_TIB;
		}
		else {
			networkingCapacityMaxPropertyValue = StringUtil.removeSubstring(
				networkingCapacityMaxPropertyValue, _UNIT_GIB);
			_networkingCapacityUsedUnit = _UNIT_GIB;
		}

		_networkingCapacityMax = GetterUtil.getInteger(
			networkingCapacityMaxPropertyValue);

		String storageCapacityDocumentLibraryMaxPropertyValue =
			productProperties.get(machineType + "-storage");

		storageCapacityDocumentLibraryMaxPropertyValue =
			StringUtil.removeSubstring(
				storageCapacityDocumentLibraryMaxPropertyValue, _UNIT_TIB);

		_storageCapacityDocumentLibraryMax = GetterUtil.getInteger(
			storageCapacityDocumentLibraryMaxPropertyValue);
	}

	private void _initUsage(JSONObject jsonObject) {
		if (jsonObject != null) {
			JSONObject usageJSONObject = jsonObject.getJSONObject("usage");

			_clientExtensionsCapacityCPUUsed = usageJSONObject.optInt(
				"clientExtensionsCPU", 0);
			_clientExtensionsCapacityRAMUsed = _format(
				usageJSONObject.optBigDecimal(
					"clientExtensionsRAM", BigDecimal.ZERO),
				_METRIC_CLIENT_EXTENSIONS_RAM);
			_databaseCapacityUsed = _format(
				usageJSONObject.optBigDecimal(
					"databaseStorage", BigDecimal.ZERO),
				_METRIC_DATABASE_STORAGE);
			_monthlyActiveLoggedInUsersUsed = _format(
				usageJSONObject.optBigDecimal("logStorage", BigDecimal.ZERO),
				_METRIC_LOG_STORAGE);
			_networkingCapacityUsed = _format(
				usageJSONObject.optBigDecimal(
					"networkTraffic", BigDecimal.ZERO),
				_METRIC_NETWORK_TRAFFIC);
			_storageCapacityDocumentLibraryUsed = _format(
				usageJSONObject.optBigDecimal(
					"documentLibraryAndBackupStorage", BigDecimal.ZERO),
				_METRIC_DOCUMENT_LIBRARY);
		}
	}

	private static final String _METRIC_CLIENT_EXTENSIONS_RAM =
		"clientExtensionsRAMMetric";

	private static final String _METRIC_DATABASE_STORAGE =
		"databaseStorageMetric";

	private static final String _METRIC_DOCUMENT_LIBRARY =
		"documentLibraryAndBackupStorageMetric";

	private static final String _METRIC_LOG_STORAGE = "logStorageMetric";

	private static final String _METRIC_NETWORK_TRAFFIC =
		"networkTrafficMetric";

	private static final String _UNIT_GIB = "GiB";

	private static final String _UNIT_TIB = "TiB";

	private int _clientExtensionsCapacityCPUMax;
	private int _clientExtensionsCapacityCPUUsed;
	private int _clientExtensionsCapacityRAMMax;
	private float _clientExtensionsCapacityRAMUsed;
	private int _databaseCapacityMax;
	private float _databaseCapacityUsed;
	private long _monthlyActiveLoggedInUsersMax;
	private float _monthlyActiveLoggedInUsersUsed;
	private int _networkingCapacityMax;
	private float _networkingCapacityUsed;
	private String _networkingCapacityUsedUnit;
	private int _storageCapacityDocumentLibraryMax;
	private float _storageCapacityDocumentLibraryUsed;
	private final Map<String, String> _usageUnits = new HashMap<>();

}