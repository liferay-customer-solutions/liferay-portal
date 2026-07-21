/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.commerce.admin.order.client.dto.v1_0.Order;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.OrderItem;
import com.liferay.one.exception.DuplicateEntitlementException;
import com.liferay.one.model.Entitlement;
import com.liferay.one.model.EntitlementDefinition;
import com.liferay.one.util.OrderItemUtil;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.GetterUtil;

import java.time.Instant;

import java.util.List;
import java.util.Map;
import java.util.Objects;

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
			long accountEntryId, long commerceOrderItemId, long contractId,
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

		if (accountEntryId > 0) {
			entitlementJSONObject.put(
				"r_accountEntryToEntitlement_accountEntryId", accountEntryId);
		}

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

		OrderItem orderItem = _commerceOrderItemService.fetchCommerceOrderItem(
			commerceOrderItemId);

		if (orderItem == null) {
			_log.error(
				"Unable to find commerce order item " + commerceOrderItemId);

			return;
		}

		if (OrderItemUtil.isCanceled(orderItem)) {
			if (_log.isInfoEnabled()) {
				_log.info(
					"Skipping entitlement generation for canceled order item " +
						commerceOrderItemId);
			}

			return;
		}

		List<EntitlementDefinition> entitlementDefinitions =
			_entitlementDefinitionService.getEntitlementDefinitions(
				StringBundler.concat(
					"(r_commerceProductToEntitlementDefinition_CProductId eq '",
					orderItem.getProductId(), "') and (active eq true)"),
				OrderItemUtil.getProductOptions(orderItem));

		if (entitlementDefinitions.isEmpty()) {
			if (_log.isInfoEnabled()) {
				_log.info(
					StringBundler.concat(
						"Skipping order item ", commerceOrderItemId,
						": no active entitlement definitions matched product ",
						orderItem.getProductId()));
			}

			return;
		}

		Order order = _commerceOrderService.fetchCommerceOrder(
			orderItem.getOrderId());

		long accountEntryId = _getAccountEntryId(order);
		long contractId = _getContractId(order);

		Instant endDateInstant = OrderItemUtil.getEndDateInstant(orderItem);
		Instant startDateInstant = OrderItemUtil.getStartDateInstant(orderItem);

		String endDate = null;

		if (endDateInstant != null) {
			endDate = endDateInstant.toString();
		}

		String startDate = null;

		if (startDateInstant != null) {
			startDate = startDateInstant.toString();
		}

		for (EntitlementDefinition entitlementDefinition :
				entitlementDefinitions) {

			try {
				addEntitlement(
					accountEntryId, commerceOrderItemId, contractId,
					entitlementDefinition.getEntitlementDefinitionId(), endDate,
					entitlementDefinition.getGrantType(), null,
					entitlementDefinition.getName(),
					entitlementDefinition.getDefaultQuantity(), startDate);
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

	public boolean hasEntitlement(long accountId, String... entitlementNames)
		throws Exception {

		if (entitlementNames.length == 0) {
			return false;
		}

		StringBundler sb = new StringBundler();

		sb.append("(r_accountEntryToEntitlement_accountEntryId eq '");
		sb.append(accountId);
		sb.append("') and (");

		for (int i = 0; i < entitlementNames.length; i++) {
			if (i > 0) {
				sb.append(" or ");
			}

			sb.append("name eq '");
			sb.append(entitlementNames[i]);
			sb.append("'");
		}

		sb.append(")");

		List<Entitlement> entitlements = getEntitlements(sb.toString());

		for (Entitlement entitlement : entitlements) {
			if (!entitlement.isExpired()) {
				return true;
			}
		}

		return false;
	}

	public void trimEntitlements(long commerceOrderItemId, String endDate)
		throws Exception {

		List<Entitlement> entitlements = getEntitlements(
			StringBundler.concat(
				"r_commerceOrderItemToEntitlement_commerceOrderItemId eq '",
				commerceOrderItemId, "'"));

		Instant endDateInstant = Instant.parse(endDate);

		for (Entitlement entitlement : entitlements) {
			Instant curEndDateInstant = entitlement.getEndDateInstant();

			if ((curEndDateInstant != null) &&
				!curEndDateInstant.isAfter(endDateInstant)) {

				continue;
			}

			Instant trimmedEndDateInstant = endDateInstant;

			Instant startDateInstant = entitlement.getStartDateInstant();

			if ((startDateInstant != null) &&
				startDateInstant.isAfter(endDateInstant)) {

				trimmedEndDateInstant = startDateInstant;
			}

			if (Objects.equals(curEndDateInstant, trimmedEndDateInstant)) {
				continue;
			}

			patch(
				getAuthorization(),
				new JSONObject(
				).put(
					"endDate", trimmedEndDateInstant.toString()
				).toString(),
				UriComponentsBuilder.fromPath(
					"/o/c/entitlements/" + entitlement.getEntitlementId()
				).build(
				).toUri());
		}
	}

	public void updateEntitlementContract(long entitlementId, long contractId)
		throws Exception {

		patch(
			getAuthorization(),
			new JSONObject(
			).put(
				"r_contractToEntitlement_c_contractId", contractId
			).toString(),
			UriComponentsBuilder.fromPath(
				"/o/c/entitlements/" + entitlementId
			).build(
			).toUri());
	}

	private long _getAccountEntryId(Order order) {
		if (order == null) {
			return 0;
		}

		return order.getAccountId();
	}

	private long _getContractId(Order order) {
		if (order == null) {
			return 0;
		}

		Map<String, Object> customFields =
			(Map<String, Object>)order.getCustomFields();

		if (customFields == null) {
			return 0;
		}

		return GetterUtil.getLong(customFields.get("contractId"));
	}

	private static final Log _log = LogFactory.getLog(EntitlementService.class);

	@Autowired
	private CommerceOrderItemService _commerceOrderItemService;

	@Autowired
	private CommerceOrderService _commerceOrderService;

	@Autowired
	private EntitlementDefinitionService _entitlementDefinitionService;

}