/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.salesforce.model;

import org.json.JSONObject;

/**
 * @author Kyle Bischof
 */
public class Account {

	public Account(JSONObject jsonObject) {
		_accountTier = jsonObject.optString("Account_Tier__c");
		_activeSubscription = jsonObject.optBoolean("Active_Subscription__c");
		_billingCity = jsonObject.optString("BillingCity");
		_billingCountry = jsonObject.optString("BillingCountry");
		_billingPostalCode = jsonObject.optString("BillingPostalCode");
		_billingState = jsonObject.optString("BillingState");
		_billingStreet = jsonObject.optString("BillingStreet");
		_description = jsonObject.optString("Description");
		_fax = jsonObject.optString("Fax");
		_id = jsonObject.optString("Id");
		_name = jsonObject.optString("Name");
		_ownerEmail = jsonObject.optString("Owner_Email__c");
		_phone = jsonObject.optString("Phone");
		_shippingCity = jsonObject.optString("ShippingCity");
		_shippingCountry = jsonObject.optString("ShippingCountry");
		_shippingPostalCode = jsonObject.optString("ShippingPostalCode");
		_shippingState = jsonObject.optString("ShippingState");
		_shippingStreet = jsonObject.optString("ShippingStreet");
		_website = jsonObject.optString("Website");
	}

	public Account(
		JSONObject accountJSONObject, JSONObject opportunityJSONObject) {

		_accountTier = accountJSONObject.optString("Account_Tier__c");
		_activeSubscription = false;
		_billingCity = opportunityJSONObject.optString(
			"AccountAddress_Billing_Address__r.City__c");
		_billingCountry = opportunityJSONObject.optString(
			"AccountAddress_Billing_Address__r.Country__c");
		_billingPostalCode = opportunityJSONObject.optString(
			"AccountAddress_Billing_Address__r.Postal_Code__c");
		_billingState = opportunityJSONObject.optString(
			"AccountAddress_Billing_Address__r.State__c");
		_billingStreet = opportunityJSONObject.optString(
			"AccountAddress_Billing_Address__r.Street__c");
		_description = accountJSONObject.optString("Description");
		_fax = accountJSONObject.optString("Fax");
		_id = accountJSONObject.optString("Id");
		_name = accountJSONObject.optString("Name");
		_ownerEmail = accountJSONObject.optString("Owner.Email");
		_phone = accountJSONObject.optString("Phone");
		_shippingCity = opportunityJSONObject.optString(
			"AccountAddress_Shipping_Address__r.City__c");
		_shippingCountry = opportunityJSONObject.optString(
			"AccountAddress_Shipping_Address__r.Country__c");
		_shippingPostalCode = opportunityJSONObject.optString(
			"AccountAddress_Shipping_Address__r.Postal_Code__c");
		_shippingState = opportunityJSONObject.optString(
			"AccountAddress_Shipping_Address__r.State__c");
		_shippingStreet = opportunityJSONObject.optString(
			"AccountAddress_Shipping_Address__r.Street__c");
		_website = accountJSONObject.optString("Website");
	}

	public String getAccountTier() {
		return _accountTier;
	}

	public String getBillingCity() {
		return _billingCity;
	}

	public String getBillingCountry() {
		return _billingCountry;
	}

	public String getBillingPostalCode() {
		return _billingPostalCode;
	}

	public String getBillingState() {
		return _billingState;
	}

	public String getBillingStreet() {
		return _billingStreet;
	}

	public String getDescription() {
		return _description;
	}

	public String getFax() {
		return _fax;
	}

	public String getId() {
		return _id;
	}

	public String getName() {
		return _name;
	}

	public String getOwnerEmail() {
		return _ownerEmail;
	}

	public String getPhone() {
		return _phone;
	}

	public String getShippingCity() {
		return _shippingCity;
	}

	public String getShippingCountry() {
		return _shippingCountry;
	}

	public String getShippingPostalCode() {
		return _shippingPostalCode;
	}

	public String getShippingState() {
		return _shippingState;
	}

	public String getShippingStreet() {
		return _shippingStreet;
	}

	public String getWebsite() {
		return _website;
	}

	public boolean isActiveSubscription() {
		return _activeSubscription;
	}

	private final String _accountTier;
	private final boolean _activeSubscription;
	private final String _billingCity;
	private final String _billingCountry;
	private final String _billingPostalCode;
	private final String _billingState;
	private final String _billingStreet;
	private final String _description;
	private final String _fax;
	private final String _id;
	private final String _name;
	private final String _ownerEmail;
	private final String _phone;
	private final String _shippingCity;
	private final String _shippingCountry;
	private final String _shippingPostalCode;
	private final String _shippingState;
	private final String _shippingStreet;
	private final String _website;

}