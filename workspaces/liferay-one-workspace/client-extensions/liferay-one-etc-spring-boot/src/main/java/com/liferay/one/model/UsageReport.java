/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.model;

import com.liferay.portal.kernel.util.Validator;

import java.time.Instant;

import org.json.JSONObject;

/**
 * @author Drew Brokke
 */
public class UsageReport {

	public UsageReport(JSONObject jsonObject) {
		_accountExternalReferenceCode = jsonObject.optString(
			"accountExternalReferenceCode");
		_aggregateQuantity = jsonObject.optDoubleObject(
			"aggregateQuantity", null);
		_commerceOrderId = jsonObject.optLong("commerceOrderId");
		_contractExternalReferenceCode = jsonObject.optString(
			"contractExternalReferenceCode");
		_dateFromInstant = _parseInstant(jsonObject.optString("dateFrom"));
		_dateToInstant = _parseInstant(jsonObject.optString("dateTo"));
		_entitledQuantity = jsonObject.optDoubleObject(
			"entitledQuantity", null);
		_externalReferenceCode = jsonObject.optString("externalReferenceCode");
		_generatedAtInstant = _parseInstant(
			jsonObject.optString("generatedAt"));
		_generatorClassName = jsonObject.optString("generatorClassName");
		_overageAmount = jsonObject.optDoubleObject("overageAmount", null);
		_overageCurrency = jsonObject.optString("overageCurrency");
		_overageQuantity = jsonObject.optDoubleObject("overageQuantity", null);
		_projectId = jsonObject.optLong("r_projectToUsageReport_c_projectId");
		_reviewStatus = _parseReviewStatus(jsonObject);
		_skuExternalReferenceCode = jsonObject.optString(
			"skuExternalReferenceCode");
		_targetClassName = jsonObject.optString("targetClassName");
		_targetPK = jsonObject.optLong("targetPK");
		_targetType = jsonObject.optString("targetType");
		_usageDefinitionId = jsonObject.optLong(
			"r_usageDefinitionToUsageReport_c_usageDefinitionId");
		_usageReportId = jsonObject.getLong("id");
	}

	public String getAccountExternalReferenceCode() {
		return _accountExternalReferenceCode;
	}

	public Double getAggregateQuantity() {
		return _aggregateQuantity;
	}

	public long getCommerceOrderId() {
		return _commerceOrderId;
	}

	public String getContractExternalReferenceCode() {
		return _contractExternalReferenceCode;
	}

	public Instant getDateFromInstant() {
		return _dateFromInstant;
	}

	public Instant getDateToInstant() {
		return _dateToInstant;
	}

	public Double getEntitledQuantity() {
		return _entitledQuantity;
	}

	public String getExternalReferenceCode() {
		return _externalReferenceCode;
	}

	public Instant getGeneratedAtInstant() {
		return _generatedAtInstant;
	}

	public String getGeneratorClassName() {
		return _generatorClassName;
	}

	public Double getOverageAmount() {
		return _overageAmount;
	}

	public String getOverageCurrency() {
		return _overageCurrency;
	}

	public Double getOverageQuantity() {
		return _overageQuantity;
	}

	public long getProjectId() {
		return _projectId;
	}

	public String getReviewStatus() {
		return _reviewStatus;
	}

	public String getSkuExternalReferenceCode() {
		return _skuExternalReferenceCode;
	}

	public String getTargetClassName() {
		return _targetClassName;
	}

	public long getTargetPK() {
		return _targetPK;
	}

	public String getTargetType() {
		return _targetType;
	}

	public long getUsageDefinitionId() {
		return _usageDefinitionId;
	}

	public long getUsageReportId() {
		return _usageReportId;
	}

	private Instant _parseInstant(String value) {
		if (Validator.isNull(value)) {
			return null;
		}

		return Instant.parse(value);
	}

	private String _parseReviewStatus(JSONObject jsonObject) {
		JSONObject reviewStatusJSONObject = jsonObject.optJSONObject(
			"reviewStatus");

		if (reviewStatusJSONObject != null) {
			return reviewStatusJSONObject.optString("key");
		}

		return jsonObject.optString("reviewStatus");
	}

	private final String _accountExternalReferenceCode;
	private final Double _aggregateQuantity;
	private final long _commerceOrderId;
	private final String _contractExternalReferenceCode;
	private final Instant _dateFromInstant;
	private final Instant _dateToInstant;
	private final Double _entitledQuantity;
	private final String _externalReferenceCode;
	private final Instant _generatedAtInstant;
	private final String _generatorClassName;
	private final Double _overageAmount;
	private final String _overageCurrency;
	private final Double _overageQuantity;
	private final long _projectId;
	private final String _reviewStatus;
	private final String _skuExternalReferenceCode;
	private final String _targetClassName;
	private final long _targetPK;
	private final String _targetType;
	private final long _usageDefinitionId;
	private final long _usageReportId;

}