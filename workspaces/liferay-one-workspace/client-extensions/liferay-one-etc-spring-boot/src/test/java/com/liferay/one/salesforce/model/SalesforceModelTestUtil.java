/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.salesforce.model;

import com.liferay.headless.commerce.admin.order.client.custom.field.CustomField;
import com.liferay.headless.commerce.admin.order.client.custom.field.CustomValue;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.OrderItem;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author Felipe Franca
 */
public class SalesforceModelTestUtil {

	public static JSONObject createAccountJSONObject(
		boolean activeSubscription, String billingCountry, String id,
		String name) {

		return new JSONObject(
		).put(
			"Active_Subscription__c", activeSubscription
		).put(
			"BillingCountry", billingCountry
		).put(
			"Id", id
		).put(
			"Name", name
		);
	}

	public static JSONObject createAccountJSONObject(String id, String name) {
		return createAccountJSONObject(true, "", id, name);
	}

	public static JSONObject createAccountJSONObject(
		String billingCountry, String id, String name) {

		return createAccountJSONObject(true, billingCountry, id, name);
	}

	public static JSONObject createObjectMessagePayload(
		String action, String salesforceObjectName,
		JSONObject... recordJSONObjects) {

		JSONArray recordsJSONArray = new JSONArray();

		for (JSONObject recordJSONObject : recordJSONObjects) {
			recordsJSONArray.put(recordJSONObject);
		}

		return new JSONObject(
		).put(
			"action", action
		).put(
			"records", recordsJSONArray
		).put(
			"salesforceObjectName", salesforceObjectName
		);
	}

	public static JSONObject createOpportunityJSONObject(
		String accountId, String amendedContractOpportunityId,
		boolean hasRenewal, String id, String ownerEmail, String productFamily,
		String projectId, String soldBy, String stageName, String type) {

		return new JSONObject(
		).put(
			"AccountId", accountId
		).put(
			"Has_Renewal__c", hasRenewal ? 1 : 0
		).put(
			"Id", id
		).put(
			"Owner.Email", ownerEmail
		).put(
			"Product_Family__c", productFamily
		).put(
			"Project__c", projectId
		).put(
			"SBQQ__AmendedContract__r.SBQQ__Opportunity__c",
			amendedContractOpportunityId
		).put(
			"Sold_By__c", soldBy
		).put(
			"StageName", stageName
		).put(
			"Type", type
		);
	}

	public static JSONObject createOpportunityLineItemJSONObject(
		String currencyIsoCode, String endDate, String id, String product2Id,
		String productName, String productType, double quantity,
		String serviceDate) {

		JSONObject jsonObject = new JSONObject(
		).put(
			"CurrencyIsoCode", currencyIsoCode
		).put(
			"Id", id
		).put(
			"Product_Type__c", productType
		).put(
			"Product2.Name", productName
		).put(
			"Product2Id", product2Id
		).put(
			"Quantity", quantity
		);

		if (endDate != null) {
			jsonObject.put("End_Date__c", endDate);
		}

		if (serviceDate != null) {
			jsonObject.put("ServiceDate", serviceDate);
		}

		return jsonObject;
	}

	public static JSONObject createOpportunityMessagePayload(
		JSONObject... recordJSONObjects) {

		JSONArray recordsJSONArray = new JSONArray();

		for (JSONObject recordJSONObject : recordJSONObjects) {
			recordsJSONArray.put(recordJSONObject);
		}

		return new JSONObject(
		).put(
			"records", recordsJSONArray
		);
	}

	public static JSONObject createOpportunityRecordJSONObject(
		JSONObject accountJSONObject, JSONObject opportunityJSONObject,
		JSONArray opportunityLineItemsJSONArray,
		JSONArray projectContactRolesJSONArray, JSONObject projectJSONObject) {

		JSONObject jsonObject = new JSONObject(
		).put(
			"account", accountJSONObject
		).put(
			"opportunity", opportunityJSONObject
		).put(
			"opportunityLineItems", opportunityLineItemsJSONArray
		).put(
			"projectContactRoles", projectContactRolesJSONArray
		);

		if (projectJSONObject != null) {
			jsonObject.put("project", projectJSONObject);
		}

		return jsonObject;
	}

	public static OrderItem createOrderItem(
		String customStatus, String effectiveEndDate, String endDate,
		String externalReferenceCode, Long id, String skuExternalReferenceCode,
		String startDate) {

		List<CustomField> customFields = new ArrayList<>();

		_addCustomField(customFields, "customStatus", customStatus);
		_addCustomField(customFields, "effectiveEndDate", effectiveEndDate);
		_addCustomField(customFields, "endDate", endDate);
		_addCustomField(customFields, "startDate", startDate);

		OrderItem orderItem = new OrderItem();

		orderItem.setCustomFields(customFields.toArray(new CustomField[0]));
		orderItem.setExternalReferenceCode(externalReferenceCode);
		orderItem.setId(id);
		orderItem.setSkuExternalReferenceCode(skuExternalReferenceCode);

		return orderItem;
	}

	public static JSONObject createProjectContactRoleJSONObject(
		String contactRole, String emailAddress, String firstName,
		String lastName, String projectId) {

		return new JSONObject(
		).put(
			"Contact__r.Email", emailAddress
		).put(
			"Contact__r.FirstName", firstName
		).put(
			"Contact__r.LastName", lastName
		).put(
			"Contact_Role__c", contactRole
		).put(
			"Project__c", projectId
		);
	}

	private static void _addCustomField(
		List<CustomField> customFields, String name, String value) {

		if (value == null) {
			return;
		}

		CustomValue customValue = new CustomValue();

		customValue.setData(value);

		CustomField customField = new CustomField();

		customField.setCustomValue(customValue);
		customField.setName(name);

		customFields.add(customField);
	}

}