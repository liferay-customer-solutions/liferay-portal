/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.model;

import com.liferay.one.constants.EntitlementConstants;

import java.math.BigDecimal;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author Ryan Schuhler
 */
public class LDPEventUsageStrategy extends BaseUsageStrategy {

	public static final String FIELD_EVENT_HISTORY = "eventHistory";

	public static final String FIELD_EVENT_SUMMARY = "eventSummary";

	public LDPEventUsageStrategy(
		String response, List<Entitlement> entitlements) {

		super(response);

		for (Entitlement entitlement : entitlements) {
			String name = entitlement.getName();

			if (name.equals(EntitlementConstants.NAME_EVENTS)) {
				_baseAllotment = getMaxCount(_baseAllotment, entitlement);
			}
			else if (name.equals(
						EntitlementConstants.NAME_EVENTS_ADD_ON_BUCKET)) {

				_addOnBucketCount = getMaxCount(_addOnBucketCount, entitlement);
			}
		}

		JSONObject usageJSONObject = getUsageJSONObject();

		if (usageJSONObject == null) {
			_eventHistoryJSONArray = null;
			_eventSummaryJSONArray = null;

			return;
		}

		_eventHistoryJSONArray = usageJSONObject.optJSONArray(
			FIELD_EVENT_HISTORY);
		_eventSummaryJSONArray = usageJSONObject.optJSONArray(
			FIELD_EVENT_SUMMARY);

		if (_eventSummaryJSONArray != null) {
			_usedCount = _sumEventsCount(_eventSummaryJSONArray);
		}
		else if (_eventHistoryJSONArray != null) {
			for (int i = 0; i < _eventHistoryJSONArray.length(); i++) {
				JSONObject eventHistoryJSONObject =
					_eventHistoryJSONArray.getJSONObject(i);

				_usedCount = _usedCount.add(
					_sumEventsCount(
						eventHistoryJSONObject.optJSONArray(
							FIELD_EVENT_SUMMARY)));
			}
		}
	}

	@Override
	public JSONObject toJSONObject() {
		JSONObject jsonObject = new JSONObject(
		).put(
			"addOnBucketCount", _addOnBucketCount
		).put(
			"baseAllotment", _baseAllotment
		).put(
			"maxCount", _getMaxCount()
		);

		if (hasUsage()) {
			jsonObject.put("usedCount", _usedCount);
		}

		if (_eventHistoryJSONArray != null) {
			jsonObject.put(FIELD_EVENT_HISTORY, _eventHistoryJSONArray);
		}

		if (_eventSummaryJSONArray != null) {
			jsonObject.put(FIELD_EVENT_SUMMARY, _eventSummaryJSONArray);
		}

		return jsonObject;
	}

	private BigDecimal _getMaxCount() {
		if ((_addOnBucketCount.signum() < 0) || (_baseAllotment.signum() < 0)) {
			return _QUANTITY_UNLIMITED;
		}

		return _baseAllotment.add(
			_addOnBucketCount.multiply(_QUANTITY_EVENTS_ADD_ON_BUCKET));
	}

	private BigDecimal _sumEventsCount(JSONArray eventSummaryJSONArray) {
		BigDecimal eventsCount = BigDecimal.ZERO;

		if (eventSummaryJSONArray == null) {
			return eventsCount;
		}

		for (int i = 0; i < eventSummaryJSONArray.length(); i++) {
			JSONObject eventSummaryJSONObject =
				eventSummaryJSONArray.getJSONObject(i);

			eventsCount = eventsCount.add(
				eventSummaryJSONObject.optBigDecimal(
					"eventsCount", BigDecimal.ZERO));
		}

		return eventsCount;
	}

	private static final BigDecimal _QUANTITY_EVENTS_ADD_ON_BUCKET =
		BigDecimal.valueOf(EntitlementConstants.QUANTITY_EVENTS_ADD_ON_BUCKET);

	private static final BigDecimal _QUANTITY_UNLIMITED = new BigDecimal(-1);

	private BigDecimal _addOnBucketCount = BigDecimal.ZERO;
	private BigDecimal _baseAllotment = BigDecimal.ZERO;
	private final JSONArray _eventHistoryJSONArray;
	private final JSONArray _eventSummaryJSONArray;
	private BigDecimal _usedCount = BigDecimal.ZERO;

}