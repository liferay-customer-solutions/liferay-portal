/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.commerce.admin.order.client.dto.v1_0.Order;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.OrderItem;
import com.liferay.one.constants.EntitlementConstants;
import com.liferay.one.exception.DuplicateEntitlementException;
import com.liferay.one.model.Entitlement;
import com.liferay.one.model.EntitlementDefinition;
import com.liferay.one.util.OrderItemUtil;
import com.liferay.petra.function.transform.TransformUtil;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.math.BigDecimal;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
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
			"externalReferenceCode",
			StringBundler.concat(
				commerceOrderItemId, "-", entitlementDefinitionId)
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

		String skuExternalReferenceCode =
			orderItem.getSkuExternalReferenceCode();

		List<EntitlementDefinition> entitlementDefinitions =
			_entitlementDefinitionService.getEntitlementDefinitions(
				StringBundler.concat(
					"(skuExternalReferenceCode eq '", skuExternalReferenceCode,
					"') and (active eq true)"),
				OrderItemUtil.getProductOptions(orderItem));

		if (entitlementDefinitions.isEmpty()) {
			if (_log.isInfoEnabled()) {
				_log.info(
					StringBundler.concat(
						"Skipping order item ", commerceOrderItemId,
						" because no active entitlement definitions matched ",
						"SKU ", skuExternalReferenceCode));
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

		BigDecimal orderItemQuantity = orderItem.getQuantity();

		for (EntitlementDefinition entitlementDefinition :
				entitlementDefinitions) {

			try {
				Entitlement entitlement = addEntitlement(
					accountEntryId, commerceOrderItemId, contractId,
					entitlementDefinition.getEntitlementDefinitionId(), endDate,
					entitlementDefinition.getGrantType(),
					entitlementDefinition.getMaxQuantity(),
					entitlementDefinition.getName(),
					projectExternalReferenceCode,
					_multiply(
						orderItemQuantity,
						entitlementDefinition.getDefaultQuantity()),
					startDate);

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
			Collection<Long> accountEntryIds)
		throws Exception {

		return _getEntitlementDefinitions(
			getActiveEntitlements(accountEntryIds));
	}

	public List<EntitlementDefinition> getActiveEntitlementDefinitions(
			long accountEntryId)
		throws Exception {

		return _getEntitlementDefinitions(
			getActiveEntitlements(accountEntryId));
	}

	public List<EntitlementDefinition> getActiveEntitlementDefinitions(
			String projectExternalReferenceCode)
		throws Exception {

		return _getEntitlementDefinitions(
			getActiveEntitlements(projectExternalReferenceCode));
	}

	public List<Entitlement> getActiveEntitlements(
			Collection<Long> accountEntryIds)
		throws Exception {

		if (accountEntryIds.isEmpty()) {
			return Collections.emptyList();
		}

		List<String> statements = TransformUtil.transform(
			accountEntryIds,
			accountEntryId ->
				"r_accountEntryToEntitlement_accountEntryId eq '" +
					accountEntryId + "'");

		return _getActiveEntitlements(StringUtil.merge(statements, " or "));
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
				escapeODataString(projectExternalReferenceCode) + "'");
	}

	public List<Entitlement> getEntitlements(
			Instant endInstant, Collection<String> names, Instant startInstant)
		throws Exception {

		if (names.isEmpty()) {
			return Collections.emptyList();
		}

		List<String> statements = TransformUtil.transform(
			names, name -> "name eq '" + escapeODataString(name) + "'");

		return getEntitlements(
			StringBundler.concat(
				"(endDate eq null or endDate ge ", startInstant, ") and (",
				StringUtil.merge(statements, " or "),
				") and (startDate eq null or startDate lt ", endInstant, ")"));
	}

	public List<Entitlement> getEntitlements(long commerceOrderItemId)
		throws Exception {

		return getEntitlements(
			StringBundler.concat(
				"r_commerceOrderItemToEntitlement_commerceOrderItemId eq '",
				commerceOrderItemId, "'"));
	}

	public List<Entitlement> getEntitlements(
			long accountEntryId, long entitlementDefinitionId)
		throws Exception {

		return getEntitlements(
			StringBundler.concat(
				"(r_accountEntryToEntitlement_accountEntryId eq '",
				accountEntryId, "') and ",
				"(r_entitlementDefinitionToEntitlement_c_",
				"entitlementDefinitionId eq '", entitlementDefinitionId, "')"));
	}

	public List<Entitlement> getEntitlements(String filterString)
		throws Exception {

		return getAllItems(
			"/o/c/entitlements", filterString, Entitlement::new, null,
			_NESTED_FIELDS_ENTITLEMENT_DEFINITION);
	}

	public String getSubscriptionState(long accountEntryId) throws Exception {
		String state = StringPool.BLANK;

		List<Entitlement> entitlements = getEntitlements(
			"r_accountEntryToEntitlement_accountEntryId eq '" + accountEntryId +
				"'");

		for (Entitlement entitlement : entitlements) {
			String curState = _getEntitlementState(entitlement);

			if (_getStateRank(curState) > _getStateRank(state)) {
				state = curState;
			}
		}

		return state;
	}

	public boolean hasActiveEntitlement(
			String projectExternalReferenceCode, String... entitlementNames)
		throws Exception {

		if (entitlementNames.length == 0) {
			return false;
		}

		List<Entitlement> entitlements = _getActiveEntitlements(
			_getEntitlementNamesFilterString(
				"r_projectToEntitlement_c_projectERC eq '" +
					escapeODataString(projectExternalReferenceCode) + "'",
				entitlementNames));

		return !entitlements.isEmpty();
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
				escapeODataString(projectExternalReferenceCode) + "'",
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

	private List<EntitlementDefinition> _getEntitlementDefinitions(
		List<Entitlement> entitlements) {

		Map<Long, EntitlementDefinition> entitlementDefinitions =
			new LinkedHashMap<>();

		for (Entitlement entitlement : entitlements) {
			EntitlementDefinition entitlementDefinition =
				entitlement.getEntitlementDefinition();

			if (entitlementDefinition != null) {
				entitlementDefinitions.put(
					entitlementDefinition.getEntitlementDefinitionId(),
					entitlementDefinition);
			}
		}

		return new ArrayList<>(entitlementDefinitions.values());
	}

	private String _getEntitlementNamesFilterString(
		String filterString, String... entitlementNames) {

		StringBundler sb = new StringBundler((entitlementNames.length * 3) + 4);

		sb.append("(");
		sb.append(filterString);
		sb.append(") and (");

		for (int i = 0; i < entitlementNames.length; i++) {
			if (i > 0) {
				sb.append(" or ");
			}

			sb.append("name eq '");
			sb.append(escapeODataString(entitlementNames[i]));
			sb.append("'");
		}

		sb.append(")");

		return sb.toString();
	}

	private String _getEntitlementState(Entitlement entitlement) {
		if (entitlement.isExpired()) {
			return EntitlementConstants.STATE_EXPIRED;
		}

		Instant startDateInstant = entitlement.getStartDateInstant();

		if ((startDateInstant != null) &&
			startDateInstant.isAfter(Instant.now())) {

			return EntitlementConstants.STATE_UNACTIVATED;
		}

		return EntitlementConstants.STATE_ACTIVE;
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

	private int _getStateRank(String state) {
		if (state.equals(EntitlementConstants.STATE_ACTIVE)) {
			return 3;
		}

		if (state.equals(EntitlementConstants.STATE_UNACTIVATED)) {
			return 2;
		}

		if (state.equals(EntitlementConstants.STATE_EXPIRED)) {
			return 1;
		}

		return 0;
	}

	private boolean _hasEntitlement(
			String filterString, String... entitlementNames)
		throws Exception {

		if (entitlementNames.length == 0) {
			return false;
		}

		List<Entitlement> entitlements = getEntitlements(
			_getEntitlementNamesFilterString(filterString, entitlementNames));

		for (Entitlement entitlement : entitlements) {
			if (!entitlement.isExpired()) {
				return true;
			}
		}

		return false;
	}

	private Double _multiply(BigDecimal orderItemQuantity, Double quantity) {
		if ((orderItemQuantity == null) || (quantity == null)) {
			return quantity;
		}

		return quantity * orderItemQuantity.doubleValue();
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

	private static final String _NESTED_FIELDS_ENTITLEMENT_DEFINITION =
		"entitlementDefinitionToEntitlement";

	private static final Log _log = LogFactory.getLog(EntitlementService.class);

	@Autowired
	private CommerceOrderItemService _commerceOrderItemService;

	@Autowired
	private CommerceOrderService _commerceOrderService;

	@Autowired
	private EntitlementDefinitionService _entitlementDefinitionService;

}