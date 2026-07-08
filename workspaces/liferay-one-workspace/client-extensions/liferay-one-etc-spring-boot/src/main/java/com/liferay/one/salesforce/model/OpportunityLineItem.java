/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.salesforce.model;

import org.json.JSONObject;

/**
 * @author Kyle Bischof
 */
public class OpportunityLineItem {

	public OpportunityLineItem(JSONObject jsonObject) {
		_cloudRegion = jsonObject.optString("Cloud_Region__c");
		_currencyIsoCode = jsonObject.optString("CurrencyIsoCode");
		_endDate = jsonObject.optString("End_Date__c");
		_id = jsonObject.optString("Id");
		_machineType = jsonObject.optString("Machine_Type__c");
		_product2Id = jsonObject.optString("Product2Id");
		_productName = jsonObject.optString("Product2.Name");
		_productType = jsonObject.optString("Product_Type__c");
		_serviceDate = jsonObject.optString("ServiceDate");

		if (jsonObject.isNull("Number_of_Pods__c")) {
			_numberOfPods = null;
		}
		else {
			_numberOfPods = jsonObject.optDouble("Number_of_Pods__c");
		}

		if (jsonObject.isNull("Quantity")) {
			_quantity = null;
		}
		else {
			_quantity = jsonObject.optDouble("Quantity");
		}

		if (jsonObject.isNull("TotalPrice")) {
			_totalPrice = null;
		}
		else {
			_totalPrice = jsonObject.optDouble("TotalPrice");
		}

		if (jsonObject.isNull("UnitPrice")) {
			_unitPrice = null;
		}
		else {
			_unitPrice = jsonObject.optDouble("UnitPrice");
		}
	}

	public String getCloudRegion() {
		return _cloudRegion;
	}

	public String getCurrencyIsoCode() {
		return _currencyIsoCode;
	}

	public String getEndDate() {
		return _endDate;
	}

	public String getId() {
		return _id;
	}

	public String getMachineType() {
		return _machineType;
	}

	public Double getNumberOfPods() {
		return _numberOfPods;
	}

	public String getProduct2Id() {
		return _product2Id;
	}

	public String getProductName() {
		return _productName;
	}

	public String getProductType() {
		return _productType;
	}

	public Double getQuantity() {
		return _quantity;
	}

	public String getServiceDate() {
		return _serviceDate;
	}

	public Double getTotalPrice() {
		return _totalPrice;
	}

	public Double getUnitPrice() {
		return _unitPrice;
	}

	public boolean isRealignment() {
		Double quantity = getQuantity();

		if ((quantity != null) && (quantity <= 0)) {
			return true;
		}

		return false;
	}

	private final String _cloudRegion;
	private final String _currencyIsoCode;
	private final String _endDate;
	private final String _id;
	private final String _machineType;
	private final Double _numberOfPods;
	private final String _product2Id;
	private final String _productName;
	private final String _productType;
	private final Double _quantity;
	private final String _serviceDate;
	private final Double _totalPrice;
	private final Double _unitPrice;

}