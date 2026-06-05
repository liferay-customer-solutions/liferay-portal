/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.one.exception.DuplicateEntitlementException;
import com.liferay.one.model.CommerceOrder;
import com.liferay.one.model.CommerceOrderItem;
import com.liferay.one.model.Entitlement;
import com.liferay.one.model.EntitlementDefinition;
import com.liferay.petra.string.StringBundler;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Felipe Veloso
 */
@Component
public class EntitlementService extends OneBaseService {

	public Entitlement addEntitlement(
			long commerceOrderItemId, long contractId,
			long entitlementDefinitionId, String endDate, String grantType,
			Double maxQuantity, String name, Double quantity, String startDate)
		throws Exception {

		Entitlement entitlement = fetchEntitlement(
			commerceOrderItemId, entitlementDefinitionId);

		if (entitlement != null) {
			throw new DuplicateEntitlementException(
				StringBundler.concat(
					"Duplicate entitlement with order item ",
					commerceOrderItemId, " and entitlement definition ",
					entitlementDefinitionId));
		}

		JSONObject entitlementJSONObject = new JSONObject(
		).put(
			"endDate", endDate
		).put(
			"grantType", grantType
		).put(
			"maxQuantity", maxQuantity
		).put(
			"name", name
		).put(
			"quantity", quantity
		).put(
			"r_commerceOrderItemToEntitlement_commerceOrderItemId",
			commerceOrderItemId
		).put(
			"r_entitlementDefinitionToEntitlement_c_entitlementDefinitionId",
			entitlementDefinitionId
		).put(
			"startDate", startDate
		);

		if (contractId > 0) {
			entitlementJSONObject.put(
				"r_contractToEntitlement_c_contractId", contractId);
		}

		String response = post(
			getAuthorization(), entitlementJSONObject.toString(),
			UriComponentsBuilder.fromPath(
				"/o/c/entitlements"
			).build(
			).toUri());

		if (_log.isInfoEnabled()) {
			_log.info(
				StringBundler.concat(
					"Added entitlement for order item ", commerceOrderItemId,
					" from entitlement definition ", entitlementDefinitionId));
		}

		return new Entitlement(new JSONObject(response));
	}

	public Entitlement fetchEntitlement(
			long commerceOrderItemId, long entitlementDefinitionId)
		throws Exception {

		List<Entitlement> entitlements = getEntitlements(
			StringBundler.concat(
				"(r_commerceOrderItemToEntitlement_commerceOrderItemId eq '",
				commerceOrderItemId, "') and ",
				"(r_entitlementDefinitionToEntitlement_c_",
				"entitlementDefinitionId eq '", entitlementDefinitionId, "')"));

		if (entitlements.isEmpty()) {
			return null;
		}

		return entitlements.get(0);
	}

	public void generateEntitlements(long commerceOrderItemId)
		throws Exception {

		CommerceOrderItem commerceOrderItem =
			_commerceOrderItemService.fetchCommerceOrderItem(
				commerceOrderItemId);

		if (commerceOrderItem == null) {
			_log.error(
				"Unable to find commerce order item " + commerceOrderItemId);

			return;
		}

		List<EntitlementDefinition> entitlementDefinitions =
			_entitlementDefinitionService.getEntitlementDefinitions(
				StringBundler.concat(
					"(r_commerceProductToEntitlementDefinition_CProductId eq '",
					commerceOrderItem.getCProductId(),
					"') and (active eq true)"),
				commerceOrderItem.getProductOptions());

		long contractId = _getContractId(commerceOrderItem);

		for (EntitlementDefinition entitlementDefinition :
				entitlementDefinitions) {

			try {
				addEntitlement(
					commerceOrderItemId, contractId,
					entitlementDefinition.getEntitlementDefinitionId(),
					commerceOrderItem.getEndDate(),
					entitlementDefinition.getGrantType(), null,
					entitlementDefinition.getName(),
					entitlementDefinition.getDefaultQuantity(),
					commerceOrderItem.getStartDate());
			}
			catch (Exception exception) {
				_log.error(
					StringBundler.concat(
						"Unable to create entitlement for order item ",
						commerceOrderItemId, " and entitlement definition ",
						entitlementDefinition.getEntitlementDefinitionId()),
					exception);
			}
		}
	}

	public List<Entitlement> getEntitlements(String filterString)
		throws Exception {

		return getAllItems("/o/c/entitlements", filterString, Entitlement::new);
	}

	private long _getContractId(CommerceOrderItem commerceOrderItem)
		throws Exception {

		CommerceOrder commerceOrder = _commerceOrderService.fetchCommerceOrder(
			commerceOrderItem.getOrderId());

		if (commerceOrder == null) {
			return 0;
		}

		return commerceOrder.getContractId();
	}

	private static final Log _log = LogFactory.getLog(EntitlementService.class);

	@Autowired
	private CommerceOrderItemService _commerceOrderItemService;

	@Autowired
	private CommerceOrderService _commerceOrderService;

	@Autowired
	private EntitlementDefinitionService _entitlementDefinitionService;

}