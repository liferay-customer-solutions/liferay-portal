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
import com.liferay.petra.function.transform.TransformUtil;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
			Double maxQuantity, String name,
			String projectExternalReferenceCode, Double quantity,
			String startDate)
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

		if (Validator.isNotNull(projectExternalReferenceCode)) {
			entitlementJSONObject.put(
				"r_projectToEntitlement_c_projectERC",
				projectExternalReferenceCode);
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
		String projectExternalReferenceCode = _getProjectExternalReferenceCode(
			order);

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
				Entitlement entitlement = addEntitlement(
					accountEntryId, commerceOrderItemId, contractId,
					entitlementDefinition.getEntitlementDefinitionId(), endDate,
					entitlementDefinition.getGrantType(), null,
					entitlementDefinition.getName(),
					projectExternalReferenceCode,
					entitlementDefinition.getDefaultQuantity(), startDate);

				if (_log.isInfoEnabled()) {
					_log.info(
						StringBundler.concat(
							"Created entitlement ",
							entitlement.getEntitlementId(), " for order item ",
							commerceOrderItemId));
				}
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

	public List<EntitlementDefinition> getActiveEntitlementDefinitions(
			long accountEntryId)
		throws Exception {

		return _getActiveEntitlementDefinitions(
			getActiveEntitlements(accountEntryId));
	}

	public List<EntitlementDefinition> getActiveEntitlementDefinitions(
			String projectExternalReferenceCode)
		throws Exception {

		return _getActiveEntitlementDefinitions(
			getActiveEntitlements(projectExternalReferenceCode));
	}

	public List<Entitlement> getActiveEntitlements(long accountEntryId)
		throws Exception {

		return _getActiveEntitlements(
			"r_accountEntryToEntitlement_accountEntryId eq '" + accountEntryId +
				"'");
	}

	public List<Entitlement> getActiveEntitlements(
			String projectExternalReferenceCode)
		throws Exception {

		return _getActiveEntitlements(
			"r_projectToEntitlement_c_projectERC eq '" +
				projectExternalReferenceCode + "'");
	}

	public List<Entitlement> getEntitlements(long commerceOrderItemId)
		throws Exception {

		return getEntitlements(
			StringBundler.concat(
				"r_commerceOrderItemToEntitlement_commerceOrderItemId eq '",
				commerceOrderItemId, "'"));
	}

	public List<Entitlement> getEntitlements(String filterString)
		throws Exception {

		return getAllItems("/o/c/entitlements", filterString, Entitlement::new);
	}

	public boolean hasEntitlement(long accountId, String... entitlementNames)
		throws Exception {

		return _hasEntitlement(
			"r_accountEntryToEntitlement_accountEntryId eq '" + accountId + "'",
			entitlementNames);
	}

	public boolean hasEntitlement(
			String projectExternalReferenceCode, String... entitlementNames)
		throws Exception {

		return _hasEntitlement(
			"r_projectToEntitlement_c_projectERC eq '" +
				projectExternalReferenceCode + "'",
			entitlementNames);
	}

	public void trimEntitlements(long commerceOrderItemId, String endDate)
		throws Exception {

		List<Entitlement> entitlements = getEntitlements(commerceOrderItemId);

		Instant endDateInstant = Instant.parse(endDate);

		for (Entitlement entitlement : entitlements) {
			Instant curEndDateInstant = entitlement.getEndDateInstant();

			if ((curEndDateInstant != null) &&
				!curEndDateInstant.isAfter(endDateInstant)) {

				continue;
			}

			Instant trimmedEndDateInstant = _getLatestInstant(
				endDateInstant, entitlement.getStartDateInstant());

			if (Objects.equals(curEndDateInstant, trimmedEndDateInstant)) {
				continue;
			}

			_patchEntitlement(
				entitlement.getEntitlementId(),
				new JSONObject(
				).put(
					"endDate", trimmedEndDateInstant.toString()
				));
		}
	}

	public void updateEntitlementContract(long entitlementId, long contractId)
		throws Exception {

		_patchEntitlement(
			entitlementId,
			new JSONObject(
			).put(
				"r_contractToEntitlement_c_contractId", contractId
			));
	}

	public void updateEntitlementProject(
			long entitlementId, String projectExternalReferenceCode)
		throws Exception {

		_patchEntitlement(
			entitlementId,
			new JSONObject(
			).put(
				"r_projectToEntitlement_c_projectERC",
				projectExternalReferenceCode
			));
	}

	public void updateEntitlements(long commerceOrderItemId) throws Exception {
		OrderItem orderItem = _commerceOrderItemService.fetchCommerceOrderItem(
			commerceOrderItemId);

		if ((orderItem == null) || OrderItemUtil.isCanceled(orderItem)) {
			return;
		}

		Instant endDateInstant = OrderItemUtil.getEntitlementEndDateInstant(
			orderItem);
		Instant startDateInstant = OrderItemUtil.getStartDateInstant(orderItem);

		List<Entitlement> entitlements = getEntitlements(commerceOrderItemId);

		for (Entitlement entitlement : entitlements) {
			JSONObject entitlementJSONObject = new JSONObject();

			if ((startDateInstant != null) &&
				!Objects.equals(
					entitlement.getStartDateInstant(), startDateInstant)) {

				entitlementJSONObject.put(
					"startDate", startDateInstant.toString());
			}

			if (endDateInstant != null) {
				Instant effectiveStartDateInstant = startDateInstant;

				if (effectiveStartDateInstant == null) {
					effectiveStartDateInstant =
						entitlement.getStartDateInstant();
				}

				Instant targetEndDateInstant = _getLatestInstant(
					endDateInstant, effectiveStartDateInstant);

				if (!Objects.equals(
						entitlement.getEndDateInstant(),
						targetEndDateInstant)) {

					entitlementJSONObject.put(
						"endDate", targetEndDateInstant.toString());
				}
			}

			if (entitlementJSONObject.length() == 0) {
				continue;
			}

			_patchEntitlement(
				entitlement.getEntitlementId(), entitlementJSONObject);
		}
	}

	private long _getAccountEntryId(Order order) {
		if (order == null) {
			return 0;
		}

		return order.getAccountId();
	}

	private List<EntitlementDefinition> _getActiveEntitlementDefinitions(
			List<Entitlement> entitlements)
		throws Exception {

		Set<Long> entitlementDefinitionIds = new LinkedHashSet<>();

		for (Entitlement entitlement : entitlements) {
			long entitlementDefinitionId =
				entitlement.getEntitlementDefinitionId();

			if (entitlementDefinitionId > 0) {
				entitlementDefinitionIds.add(entitlementDefinitionId);
			}
		}

		if (entitlementDefinitionIds.isEmpty()) {
			return Collections.emptyList();
		}

		List<String> statements = TransformUtil.transform(
			entitlementDefinitionIds,
			entitlementDefinitionId ->
				"(id eq '" + entitlementDefinitionId + "')");

		return _entitlementDefinitionService.getEntitlementDefinitions(
			StringUtil.merge(statements, " or "));
	}

	private List<Entitlement> _getActiveEntitlements(String filterString)
		throws Exception {

		Instant instant = Instant.now(
		).truncatedTo(
			ChronoUnit.MILLIS
		);

		return getEntitlements(
			StringBundler.concat(
				"(endDate eq null or endDate ge ", instant, ") and (",
				filterString, ") and (startDate eq null or startDate le ",
				instant, ")"));
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

	private Instant _getLatestInstant(
		Instant endDateInstant, Instant startDateInstant) {

		if ((startDateInstant != null) &&
			startDateInstant.isAfter(endDateInstant)) {

			return startDateInstant;
		}

		return endDateInstant;
	}

	private String _getProjectExternalReferenceCode(Order order) {
		if (order == null) {
			return null;
		}

		Map<String, Object> customFields =
			(Map<String, Object>)order.getCustomFields();

		if (customFields == null) {
			return null;
		}

		return GetterUtil.getString(customFields.get("salesforceProjectId"));
	}

	private boolean _hasEntitlement(
			String filterString, String... entitlementNames)
		throws Exception {

		if (entitlementNames.length == 0) {
			return false;
		}

		StringBundler sb = new StringBundler();

		sb.append("(");
		sb.append(filterString);
		sb.append(") and (");

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

	private void _patchEntitlement(
			long entitlementId, JSONObject entitlementJSONObject)
		throws Exception {

		patch(
			getAuthorization(), entitlementJSONObject.toString(),
			UriComponentsBuilder.fromPath(
				"/o/c/entitlements/" + entitlementId
			).build(
			).toUri());
	}

	private static final Log _log = LogFactory.getLog(EntitlementService.class);

	@Autowired
	private CommerceOrderItemService _commerceOrderItemService;

	@Autowired
	private CommerceOrderService _commerceOrderService;

	@Autowired
	private EntitlementDefinitionService _entitlementDefinitionService;

}