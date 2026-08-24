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
 * @author Ryan Schuhler
 */
public class LDPUsageStrategy extends BaseUsageStrategy {

	public static final String METRIC_ACTIVE_BATCH_SEGMENTS =
		"activeBatchSegments";

	public static final String METRIC_ACTIVE_REAL_TIME_SEGMENTS =
		"activeRealTimeSegments";

	public static final String METRIC_API_REQUESTS = "apiRequests";

	public static final String METRIC_CONNECTORS = "connectors";

	public LDPUsageStrategy(String response, List<Entitlement> entitlements) {
		super(response);

		for (Entitlement entitlement : entitlements) {
			String name = entitlement.getName();

			if (name.equals(EntitlementConstants.NAME_ACTIVE_BATCH_SEGMENTS)) {
				_activeBatchSegmentsMax = getMaxCount(
					_activeBatchSegmentsMax, entitlement);
			}
			else if (name.equals(
						EntitlementConstants.NAME_ACTIVE_REAL_TIME_SEGMENTS)) {

				_activeRealTimeSegmentsMax = getMaxCount(
					_activeRealTimeSegmentsMax, entitlement);
			}
			else if (name.equals(EntitlementConstants.NAME_API_REQUESTS)) {
				_apiRequestsMax = getMaxCount(_apiRequestsMax, entitlement);
			}
			else if (name.equals(EntitlementConstants.NAME_CONNECTORS)) {
				_connectorsMax = getMaxCount(_connectorsMax, entitlement);
			}
		}

		JSONObject usageJSONObject = getUsageJSONObject();

		if (usageJSONObject != null) {
			_activeBatchSegmentsUsed = usageJSONObject.optBigDecimal(
				"activeBatchSegmentsCount", BigDecimal.ZERO);
			_activeRealTimeSegmentsUsed = usageJSONObject.optBigDecimal(
				"activeRealTimeSegmentsCount", BigDecimal.ZERO);
			_apiRequestsUsed = usageJSONObject.optBigDecimal(
				"apiRequestsCount", BigDecimal.ZERO);
			_connectorsUsed = usageJSONObject.optBigDecimal(
				"connectorsCount", BigDecimal.ZERO);
		}
	}

	@Override
	public JSONObject toJSONObject() {
		return new JSONObject(
		).put(
			METRIC_ACTIVE_BATCH_SEGMENTS,
			createUsageJSONObject(
				_activeBatchSegmentsMax, _activeBatchSegmentsUsed)
		).put(
			METRIC_ACTIVE_REAL_TIME_SEGMENTS,
			createUsageJSONObject(
				_activeRealTimeSegmentsMax, _activeRealTimeSegmentsUsed)
		).put(
			METRIC_API_REQUESTS,
			createUsageJSONObject(_apiRequestsMax, _apiRequestsUsed)
		).put(
			METRIC_CONNECTORS,
			createUsageJSONObject(_connectorsMax, _connectorsUsed)
		);
	}

	private BigDecimal _activeBatchSegmentsMax = BigDecimal.ZERO;
	private BigDecimal _activeBatchSegmentsUsed = BigDecimal.ZERO;
	private BigDecimal _activeRealTimeSegmentsMax = BigDecimal.ZERO;
	private BigDecimal _activeRealTimeSegmentsUsed = BigDecimal.ZERO;
	private BigDecimal _apiRequestsMax = BigDecimal.ZERO;
	private BigDecimal _apiRequestsUsed = BigDecimal.ZERO;
	private BigDecimal _connectorsMax = BigDecimal.ZERO;
	private BigDecimal _connectorsUsed = BigDecimal.ZERO;

}