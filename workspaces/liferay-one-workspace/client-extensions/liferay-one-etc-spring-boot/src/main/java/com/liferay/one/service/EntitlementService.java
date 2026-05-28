/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.client.extension.util.spring.boot3.client.LiferayOAuth2AccessTokenManager;
import com.liferay.client.extension.util.spring.boot3.service.BaseService;
import com.liferay.one.model.CommerceOrder;
import com.liferay.one.model.CommerceOrderItem;
import com.liferay.one.model.Entitlement;
import com.liferay.one.model.EntitlementDefinition;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Felipe Veloso
 */
@Component
public class EntitlementService extends BaseService {

	public Entitlement addEntitlement(
			long commerceOrderItemId, long contractId,
			long entitlementDefinitionId, String endDate, String grantType,
			Double maxQuantity, String name, Double quantity, String startDate)
		throws Exception {

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
			_getAuthorization(), entitlementJSONObject.toString(),
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

	public void generateEntitlements(CommerceOrderItem commerceOrderItem)
		throws Exception {

		long commerceOrderItemId = commerceOrderItem.getCommerceOrderItemId();

		List<EntitlementDefinition> entitlementDefinitions =
			_entitlementDefinitionService.getEntitlementDefinitions(
				_getEntitlementDefinitionFilterString(commerceOrderItem));

		long contractId = _getContractId(commerceOrderItem);

		for (EntitlementDefinition entitlementDefinition :
				entitlementDefinitions) {

			long entitlementDefinitionId =
				entitlementDefinition.getEntitlementDefinitionId();

			try {
				Entitlement existingEntitlement = fetchEntitlement(
					commerceOrderItemId, entitlementDefinitionId);

				if (existingEntitlement != null) {
					if (_log.isInfoEnabled()) {
						_log.info(
							StringBundler.concat(
								_LOG_PREFIX,
								" Skipped entitlement for order item ",
								commerceOrderItemId, " entitlement definition ",
								entitlementDefinitionId, " (exists)"));
					}

					continue;
				}

				addEntitlement(
					commerceOrderItemId, contractId, entitlementDefinitionId,
					commerceOrderItem.getEndDate(),
					entitlementDefinition.getGrantType(), null,
					entitlementDefinition.getName(),
					_getQuantity(commerceOrderItem, entitlementDefinition),
					commerceOrderItem.getStartDate());

				if (_log.isInfoEnabled()) {
					_log.info(
						StringBundler.concat(
							_LOG_PREFIX, " Created entitlement for order item ",
							commerceOrderItemId, " entitlement definition ",
							entitlementDefinitionId));
				}
			}
			catch (Exception exception) {
				_log.error(
					StringBundler.concat(
						_LOG_PREFIX,
						" Unable to create entitlement for order item ",
						commerceOrderItemId, " entitlement definition ",
						entitlementDefinitionId),
					exception);
			}
		}
	}

	public void generateEntitlements(long commerceOrderItemId)
		throws Exception {

		CommerceOrderItem commerceOrderItem =
			_commerceOrderItemService.fetchCommerceOrderItem(
				commerceOrderItemId);

		if (commerceOrderItem == null) {
			_log.error(
				_LOG_PREFIX + " Unable to find commerce order item " +
					commerceOrderItemId);

			return;
		}

		generateEntitlements(commerceOrderItem);
	}

	public List<Entitlement> getEntitlements(String filterString)
		throws Exception {

		UriComponentsBuilder uriComponentsBuilder =
			UriComponentsBuilder.fromPath(
				"/o/c/entitlements"
			).queryParam(
				"pageSize", "500"
			);

		if (filterString != null) {
			uriComponentsBuilder.queryParam("filter", filterString);
		}

		String response = get(
			_getAuthorization(),
			uriComponentsBuilder.build(
			).toUri());

		List<Entitlement> entitlements = new ArrayList<>();

		if (Validator.isNull(response)) {
			return entitlements;
		}

		try {
			JSONObject jsonObject = new JSONObject(response);

			JSONArray jsonArray = jsonObject.getJSONArray("items");

			for (int i = 0; i < jsonArray.length(); i++) {
				entitlements.add(new Entitlement(jsonArray.getJSONObject(i)));
			}

			return entitlements;
		}
		catch (Exception exception) {
			_log.error("Unable to parse JSON: " + response, exception);

			return entitlements;
		}
	}

	public void regenerateEntitlements() throws Exception {
		int page = 1;

		while (true) {
			List<CommerceOrderItem> commerceOrderItems =
				_commerceOrderItemService.getCommerceOrderItems(
					page, _PAGE_SIZE);

			if (commerceOrderItems.isEmpty()) {
				break;
			}

			for (CommerceOrderItem commerceOrderItem : commerceOrderItems) {
				try {
					generateEntitlements(commerceOrderItem);
				}
				catch (Exception exception) {
					_log.error(
						_LOG_PREFIX +
							" Unable to generate entitlements for order item " +
								commerceOrderItem.getCommerceOrderItemId(),
						exception);
				}
			}

			if (commerceOrderItems.size() < _PAGE_SIZE) {
				break;
			}

			page++;
		}
	}

	private String _getAuthorization() {
		return _liferayOAuth2AccessTokenManager.getAuthorization(
			"liferay-one-etc-spring-boot-oahs");
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

	private String _getEntitlementDefinitionFilterString(
		CommerceOrderItem commerceOrderItem) {

		StringBundler sb = new StringBundler(6);

		sb.append("(r_commerceProductToEntitlementDefinition_CProductId eq '");
		sb.append(commerceOrderItem.getCProductId());
		sb.append("') and (entitlementDefinitionActive eq true)");

		String machineType = commerceOrderItem.getMachineType();

		if (Validator.isNotNull(machineType)) {
			sb.append(" and ((machineType eq '");
			sb.append(machineType);
			sb.append("') or (machineType eq null))");
		}

		return sb.toString();
	}

	private Double _getQuantity(
		CommerceOrderItem commerceOrderItem,
		EntitlementDefinition entitlementDefinition) {

		String name = entitlementDefinition.getName();

		if (name.equals(_SIZING) && (commerceOrderItem.getSizing() != null)) {
			return commerceOrderItem.getSizing();
		}

		return entitlementDefinition.getDefaultQuantity();
	}

	private static final String _LOG_PREFIX = "[ONE-LIFERAY-ENTITLEMENT-GEN]";

	private static final int _PAGE_SIZE = 500;

	private static final String _SIZING = "sizing";

	private static final Log _log = LogFactory.getLog(EntitlementService.class);

	@Autowired
	private CommerceOrderItemService _commerceOrderItemService;

	@Autowired
	private CommerceOrderService _commerceOrderService;

	@Autowired
	private EntitlementDefinitionService _entitlementDefinitionService;

	@Autowired
	private LiferayOAuth2AccessTokenManager _liferayOAuth2AccessTokenManager;

}