/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.commerce.admin.order.client.dto.v1_0.Order;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.OrderItem;
import com.liferay.one.model.Contract;
import com.liferay.one.model.Entitlement;
import com.liferay.one.salesforce.model.SalesforceContract;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Validator;

import java.net.URI;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Kyle Bischof
 */
@Component
public class ContractService extends OneBaseService {

	public void attachContractToProject(
			long contractId, String projectExternalReferenceCode)
		throws Exception {

		JSONObject jsonObject = new JSONObject(
		).put(
			"r_projectToContract_c_projectERC", projectExternalReferenceCode
		);

		patch(
			getAuthorization(), jsonObject.toString(),
			UriComponentsBuilder.fromPath(
				"/o/c/contracts/" + contractId
			).build(
			).toUri());
	}

	public Contract fetchContractByExternalReferenceCode(
			String externalReferenceCode)
		throws Exception {

		String response = null;

		try {
			response = get(
				getAuthorization(),
				UriComponentsBuilder.fromPath(
					"/o/c/contracts/by-external-reference-code" +
						"/{externalReferenceCode}"
				).buildAndExpand(
					externalReferenceCode
				).toUri());
		}
		catch (WebClientResponseException webClientResponseException) {
			int statusCode = webClientResponseException.getStatusCode(
			).value();

			if (statusCode != HttpStatus.NOT_FOUND.value()) {
				throw webClientResponseException;
			}
		}

		if (Validator.isNull(response)) {
			return null;
		}

		return new Contract(new JSONObject(response));
	}

	public Contract fetchLatestContractByOpportunityId(String opportunityId)
		throws Exception {

		String response = get(
			getAuthorization(),
			UriComponentsBuilder.fromPath(
				"/o/c/contracts"
			).queryParam(
				"filter", "opportunityId eq '" + opportunityId + "'"
			).queryParam(
				"page", 1
			).queryParam(
				"pageSize", 1
			).queryParam(
				"sort", "dateCreated:desc,id:desc"
			).build(
			).toUri());

		if (Validator.isNull(response)) {
			return null;
		}

		JSONObject jsonObject = new JSONObject(response);

		JSONArray jsonArray = jsonObject.optJSONArray("items");

		if ((jsonArray == null) || (jsonArray.length() == 0)) {
			return null;
		}

		return new Contract(jsonArray.getJSONObject(0));
	}

	public void upsertContract(SalesforceContract salesforceContract)
		throws Exception {

		if (Validator.isNull(salesforceContract.getId()) ||
			Validator.isNull(salesforceContract.getAccountId())) {

			if (_log.isWarnEnabled()) {
				_log.warn(
					"Unable to upsert contract " + salesforceContract.getId() +
						" without an ID and account");
			}

			return;
		}

		JSONObject jsonObject = new JSONObject(
		).put(
			"externalReferenceCode", salesforceContract.getId()
		).put(
			"r_accountEntryToContract_accountEntryERC",
			salesforceContract.getAccountId()
		);

		if (salesforceContract.getContractTerm() != null) {
			jsonObject.put(
				"contractTerm", salesforceContract.getContractTerm());
		}

		String endDate = _toDateTime(salesforceContract.getEndDate());

		if (Validator.isNotNull(endDate)) {
			jsonObject.put("endDate", endDate);
		}

		if (Validator.isNotNull(salesforceContract.getOpportunityId())) {
			jsonObject.put(
				"opportunityId", salesforceContract.getOpportunityId());
		}

		String startDate = _toDateTime(salesforceContract.getStartDate());

		if (Validator.isNotNull(startDate)) {
			jsonObject.put("startDate", startDate);
		}

		Order order = null;
		Map<String, Object> customFields = null;

		if (Validator.isNotNull(salesforceContract.getOpportunityId())) {
			order = _commerceOrderService.fetchOrderByExternalReferenceCode(
				salesforceContract.getOpportunityId());
		}

		if (order != null) {
			customFields = (Map<String, Object>)order.getCustomFields();
		}

		if (customFields == null) {
			customFields = Map.of();
		}

		String projectExternalReferenceCode = GetterUtil.getString(
			customFields.get("salesforceProjectId"));

		Contract existingContract = fetchContractByExternalReferenceCode(
			salesforceContract.getId());

		if (Validator.isNotNull(projectExternalReferenceCode) &&
			((existingContract == null) ||
			 Validator.isNull(
				 existingContract.getProjectExternalReferenceCode()))) {

			jsonObject.put(
				"r_projectToContract_c_projectERC",
				projectExternalReferenceCode);
		}

		URI uri = UriComponentsBuilder.fromPath(
			"/o/c/contracts/by-external-reference-code/" +
				salesforceContract.getId()
		).build(
		).toUri();

		String response = null;

		try {
			response = patch(getAuthorization(), jsonObject.toString(), uri);
		}
		catch (WebClientResponseException webClientResponseException) {
			int statusCode = webClientResponseException.getStatusCode(
			).value();

			if (statusCode != HttpStatus.NOT_FOUND.value()) {
				throw webClientResponseException;
			}

			response = put(getAuthorization(), jsonObject.toString(), uri);
		}

		if ((order == null) || Validator.isNull(response)) {
			return;
		}

		existingContract = new Contract(new JSONObject(response));

		String orderContractId = GetterUtil.getString(
			customFields.get("contractId"));

		if (!Objects.equals(
				orderContractId, String.valueOf(existingContract.getId()))) {

			_commerceOrderService.patchOrderCustomFields(
				order.getId(), Map.of("contractId", existingContract.getId()));
		}

		OrderItem[] orderItems = order.getOrderItems();

		if (orderItems == null) {
			return;
		}

		for (OrderItem orderItem : orderItems) {
			List<Entitlement> entitlements =
				_entitlementService.getEntitlements(
					StringBundler.concat(
						"r_commerceOrderItemToEntitlement_commerceOrderItemId ",
						"eq '", orderItem.getId(), "'"));

			for (Entitlement entitlement : entitlements) {
				if (entitlement.getContractId() <= 0) {
					_entitlementService.updateEntitlementContract(
						entitlement.getEntitlementId(),
						existingContract.getId());
				}

				if (Validator.isNotNull(projectExternalReferenceCode) &&
					Validator.isNull(
						entitlement.getProjectExternalReferenceCode())) {

					_entitlementService.updateEntitlementProject(
						entitlement.getEntitlementId(),
						projectExternalReferenceCode);
				}
			}
		}
	}

	private String _toDateTime(String value) {
		if (Validator.isNull(value)) {
			return null;
		}

		Matcher matcher = _datePattern.matcher(value);

		if (matcher.matches()) {
			return value + "T00:00:00Z";
		}

		return value;
	}

	private static final Log _log = LogFactory.getLog(ContractService.class);

	private static final Pattern _datePattern = Pattern.compile(
		"\\d{4}-\\d{2}-\\d{2}");

	@Autowired
	private CommerceOrderService _commerceOrderService;

	@Autowired
	private EntitlementService _entitlementService;

}