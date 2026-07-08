/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.commerce.admin.order.client.dto.v1_0.Order;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.OrderItem;
import com.liferay.one.model.Contract;
import com.liferay.one.model.Entitlement;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.Validator;

import java.net.URI;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

		List<Contract> contracts = getAllItems(
			"/o/c/contracts", "opportunityId eq '" + opportunityId + "'",
			Contract::new);

		Contract latestContract = null;

		for (Contract contract : contracts) {
			if (latestContract == null) {
				latestContract = contract;

				continue;
			}

			int dateCreatedComparison = contract.getDateCreated(
			).compareTo(
				latestContract.getDateCreated()
			);

			if ((dateCreatedComparison > 0) ||
				((dateCreatedComparison == 0) &&
				 (contract.getId() > latestContract.getId()))) {

				latestContract = contract;
			}
		}

		return latestContract;
	}

	public void upsertContract(
			com.liferay.one.salesforce.model.Contract contract)
		throws Exception {

		if (Validator.isNull(contract.getId()) ||
			Validator.isNull(contract.getAccountId())) {

			if (_log.isWarnEnabled()) {
				_log.warn(
					"Unable to upsert contract " + contract.getId() +
						" without an ID and account");
			}

			return;
		}

		JSONObject jsonObject = new JSONObject(
		).put(
			"externalReferenceCode", contract.getId()
		).put(
			"r_accountEntryToContract_accountEntryERC", contract.getAccountId()
		);

		if (contract.getContractTerm() != null) {
			jsonObject.put("contractTerm", contract.getContractTerm());
		}

		String endDate = _toDateTime(contract.getEndDate());

		if (Validator.isNotNull(endDate)) {
			jsonObject.put("endDate", endDate);
		}

		if (Validator.isNotNull(contract.getOpportunityId())) {
			jsonObject.put("opportunityId", contract.getOpportunityId());
		}

		String startDate = _toDateTime(contract.getStartDate());

		if (Validator.isNotNull(startDate)) {
			jsonObject.put("startDate", startDate);
		}

		Order order = null;
		Map<String, Object> customFields = null;

		if (Validator.isNotNull(contract.getOpportunityId())) {
			order = _commerceOrderService.fetchOrderByExternalReferenceCode(
				contract.getOpportunityId());
		}

		if (order != null) {
			customFields = (Map<String, Object>)order.getCustomFields();
		}

		if (customFields == null) {
			customFields = Map.of();
		}

		String projectExternalReferenceCode = Objects.toString(
			customFields.get("salesforceProjectId"), null);

		Contract existingContract = fetchContractByExternalReferenceCode(
			contract.getId());

		if (Validator.isNotNull(projectExternalReferenceCode) &&
			((existingContract == null) ||
			 Validator.isNull(
				 existingContract.getProjectExternalReferenceCode()))) {

			jsonObject.put(
				"r_projectToContract_c_projectERC",
				projectExternalReferenceCode);
		}

		URI uri = UriComponentsBuilder.fromPath(
			"/o/c/contracts/by-external-reference-code/" + contract.getId()
		).build(
		).toUri();

		try {
			patch(getAuthorization(), jsonObject.toString(), uri);
		}
		catch (WebClientResponseException webClientResponseException) {
			int statusCode = webClientResponseException.getStatusCode(
			).value();

			if (statusCode != HttpStatus.NOT_FOUND.value()) {
				throw webClientResponseException;
			}

			put(getAuthorization(), jsonObject.toString(), uri);
		}

		if (order == null) {
			return;
		}

		Contract dxpContract = fetchContractByExternalReferenceCode(
			contract.getId());

		if (dxpContract == null) {
			return;
		}

		String contractIdString = Objects.toString(
			customFields.get("contractId"), null);

		if (!Objects.equals(
				contractIdString, String.valueOf(dxpContract.getId()))) {

			_commerceOrderService.patchOrderCustomFields(
				order.getId(), Map.of("contractId", dxpContract.getId()));
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
				if (entitlement.getContractId() > 0) {
					continue;
				}

				patch(
					getAuthorization(),
					new JSONObject(
					).put(
						"r_contractToEntitlement_c_contractId",
						dxpContract.getId()
					).toString(),
					UriComponentsBuilder.fromPath(
						"/o/c/entitlements/" + entitlement.getEntitlementId()
					).build(
					).toUri());
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