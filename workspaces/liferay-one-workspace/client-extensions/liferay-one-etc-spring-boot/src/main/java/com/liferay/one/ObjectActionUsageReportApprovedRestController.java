/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.liferay.headless.commerce.admin.order.client.dto.v1_0.BillingAddress;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.Order;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.OrderItem;
import com.liferay.one.model.Contract;
import com.liferay.one.service.CommerceOrderService;
import com.liferay.one.service.CommerceSkuService;
import com.liferay.one.service.ContractService;
import com.liferay.one.service.CountryService;
import com.liferay.one.service.ProjectService;
import com.liferay.one.service.SalesforceService;
import com.liferay.one.service.UsageReportService;
import com.liferay.one.service.UserAccountService;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.Validator;

import java.math.BigDecimal;

import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Creates the standalone overage commerce order and pushes it to Salesforce once
 * a usage report is approved. Wired to the C_USAGE_REPORT object as an
 * onAfterUpdate object action, so it fires on every update; it acts only when the
 * report is approved and has not already generated an order, mirroring the AI Hub
 * token purchasing flow.
 *
 * @author Ryan Schuhler
 */
@RequestMapping("/object/action/usage/report/approved")
@RestController
public class ObjectActionUsageReportApprovedRestController
	extends OneBaseRestController {

	@PostMapping
	public void post(@RequestBody String json) throws Exception {
		JSONObject jsonObject = new JSONObject(json);

		long classPK = jsonObject.getLong("classPK");

		JSONObject usageReportJSONObject = _usageReportService.fetchUsageReport(
			classPK);

		if (usageReportJSONObject == null) {
			if (_log.isWarnEnabled()) {
				_log.warn("Unable to find usage report " + classPK);
			}

			return;
		}

		if (!_isApproved(usageReportJSONObject)) {
			if (_log.isInfoEnabled()) {
				_log.info(
					"Skipping usage report " + classPK +
						" because it is not approved");
			}

			return;
		}

		if (usageReportJSONObject.optLong("commerceOrderId") > 0) {
			if (_log.isInfoEnabled()) {
				_log.info(
					"Skipping usage report " + classPK +
						" because it already generated an overage order");
			}

			return;
		}

		Order order = _createOverageOrder(classPK, usageReportJSONObject);

		_usageReportService.updateUsageReportCommerceOrderId(
			order.getId(), classPK);

		_setUpSalesforceOpportunity(order);
	}

	private Order _createOverageOrder(
			long usageReportId, JSONObject usageReportJSONObject)
		throws Exception {

		Contract contract =
			_contractService.fetchContractByExternalReferenceCode(
				usageReportJSONObject.getString(
					"contractExternalReferenceCode"));

		if (contract == null) {
			throw new IllegalStateException(
				"Unable to find contract for usage report " + usageReportId);
		}

		Long projectId = null;
		String projectName = null;

		long reportProjectId = usageReportJSONObject.optLong(
			"r_projectToUsageReport_c_projectId");

		if (reportProjectId > 0) {
			projectId = reportProjectId;
			projectName = _projectService.fetchProjectName(reportProjectId);
		}

		Order order = _commerceOrderService.createOverageOrder(
			usageReportJSONObject.getString("accountExternalReferenceCode"),
			contract.getId(),
			usageReportJSONObject.optString("overageCurrency", "USD"),
			_getOrderExternalReferenceCode(usageReportJSONObject), projectId,
			projectName,
			usageReportJSONObject.getString("skuExternalReferenceCode"),
			BigDecimal.valueOf(
				usageReportJSONObject.getDouble("overageAmount")));

		if (_log.isInfoEnabled()) {
			_log.info(
				StringBundler.concat(
					"Created overage order ", order.getId(),
					" for usage report ", usageReportId));
		}

		return order;
	}

	private String _getOrderExternalReferenceCode(
		JSONObject usageReportJSONObject) {

		String externalReferenceCode = usageReportJSONObject.getString(
			"externalReferenceCode");

		if (externalReferenceCode.startsWith(_USAGE_REPORT_ERC_PREFIX)) {
			String suffix = externalReferenceCode.substring(
				_USAGE_REPORT_ERC_PREFIX.length());

			return _OVERAGE_ORDER_ERC_PREFIX + suffix;
		}

		return externalReferenceCode + "_ORDER";
	}

	private boolean _isApproved(JSONObject usageReportJSONObject) {
		Object reviewStatus = usageReportJSONObject.opt("reviewStatus");

		if (reviewStatus instanceof JSONObject) {
			JSONObject reviewStatusJSONObject = (JSONObject)reviewStatus;

			return Objects.equals(
				reviewStatusJSONObject.optString("key"), "approved");
		}

		return Objects.equals(
			usageReportJSONObject.optString("reviewStatus"), "approved");
	}

	private void _setUpSalesforceOpportunity(Order order) throws Exception {
		BillingAddress billingAddress = order.getBillingAddress();

		OrderItem[] orderItems = order.getOrderItems();

		if ((billingAddress == null) || ArrayUtil.isEmpty(orderItems) ||
			Validator.isNull(order.getCreatorEmailAddress())) {

			if (_log.isInfoEnabled()) {
				_log.info(
					StringBundler.concat(
						"Skipping Salesforce opportunity for order ",
						order.getId(),
						" because it is missing billing, item, or creator ",
						"data"));
			}

			return;
		}

		JSONObject salesforceOpportunityJSONObject =
			_salesforceService.postSalesforceOpportunity(
				_countryService.getCountryByA2(
					billingAddress.getCountryISOCode()),
				"Subscription", order,
				_commerceSkuService.getSku(orderItems[0].getSkuId()),
				_userAccountService.getUserAccountByEmailAddress(
					order.getCreatorEmailAddress()));

		if (salesforceOpportunityJSONObject == null) {
			if (_log.isInfoEnabled()) {
				_log.info(
					"Unable to post Salesforce opportunity for order " +
						order.getId());
			}

			return;
		}

		_commerceOrderService.patchOrderExternalReferenceCode(
			order.getId(),
			salesforceOpportunityJSONObject.getJSONObject(
				"data"
			).getString(
				"opportunityId"
			));
	}

	private static final String _OVERAGE_ORDER_ERC_PREFIX = "C_OVERAGE_ORDER_";

	private static final String _USAGE_REPORT_ERC_PREFIX = "C_USAGE_REPORT_";

	private static final Log _log = LogFactory.getLog(
		ObjectActionUsageReportApprovedRestController.class);

	@Autowired
	private CommerceOrderService _commerceOrderService;

	@Autowired
	private CommerceSkuService _commerceSkuService;

	@Autowired
	private ContractService _contractService;

	@Autowired
	private CountryService _countryService;

	@Autowired
	private ProjectService _projectService;

	@Autowired
	private SalesforceService _salesforceService;

	@Autowired
	private UsageReportService _usageReportService;

	@Autowired
	private UserAccountService _userAccountService;

}