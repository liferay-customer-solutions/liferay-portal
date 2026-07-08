/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.salesforce.pubsub;

import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.headless.commerce.admin.catalog.client.dto.v1_0.Sku;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.Order;
import com.liferay.one.constants.CommerceOrderConstants;
import com.liferay.one.constants.OpportunityConstants;
import com.liferay.one.model.Contract;
import com.liferay.one.pubsub.Message;
import com.liferay.one.pubsub.subscriber.BasePubsubSubscriber;
import com.liferay.one.salesforce.model.Account;
import com.liferay.one.salesforce.model.Opportunity;
import com.liferay.one.salesforce.model.OpportunityLineItem;
import com.liferay.one.salesforce.model.Project;
import com.liferay.one.salesforce.model.ProjectContactRole;
import com.liferay.one.service.AccountService;
import com.liferay.one.service.CommerceOrderItemService;
import com.liferay.one.service.CommerceOrderService;
import com.liferay.one.service.CommerceSkuService;
import com.liferay.one.service.ContractService;
import com.liferay.one.service.ProjectService;
import com.liferay.one.service.ProvisioningContactService;
import com.liferay.one.service.ProvisioningEmailService;
import com.liferay.one.service.ProvisioningIssueService;
import com.liferay.one.service.ProvisioningOrderService;
import com.liferay.one.service.ProvisioningSubdomainService;
import com.liferay.one.service.UserAccountService;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @author Kyle Bischof
 */
@Component
@ConditionalOnProperty(
	havingValue = "true",
	name = "liferay.one.salesforce.opportunity.pubsub.subscriber.enabled"
)
public class SalesforceOpportunityPubsubSubscriber
	extends BasePubsubSubscriber {

	@Override
	public String getTopic() {
		return _topic;
	}

	@Override
	public void receive(Message message) throws Exception {
		if (_log.isDebugEnabled()) {
			_log.debug("Parsing message: " + message.getPayload());
		}

		try {
			JSONObject jsonObject = new JSONObject(message.getPayload());

			JSONArray recordsJSONArray = jsonObject.getJSONArray("records");

			for (int i = 0; i < recordsJSONArray.length(); i++) {
				JSONObject recordJSONObject = recordsJSONArray.getJSONObject(i);

				try {
					_processProvisioningRecord(recordJSONObject);
				}
				catch (Exception exception) {
					_log.error(
						"Unable to process Salesforce opportunity record " +
							recordJSONObject,
						exception);

					_provisioningIssueService.addErrorIssue(
						message, recordJSONObject, exception);
				}
			}
		}
		catch (Exception exception) {
			_log.error(
				"Unable to process Salesforce opportunity message " +
					message.getPayload(),
				exception);

			_provisioningIssueService.addErrorIssue(message, exception);
		}
	}

	@Override
	protected String getProjectId() {
		return _projectId;
	}

	@Override
	protected String getSubscriptionName() {
		return _subscription;
	}

	@Override
	protected boolean isAutoCreateTopic() {
		return false;
	}

	private void _addWarning(
		List<String> warningMessages, String warningMessage) {

		warningMessages.add(warningMessage);

		if (_log.isWarnEnabled()) {
			_log.warn(warningMessage);
		}
	}

	private String _getCurrencyCode(
		Opportunity opportunity, List<OpportunityLineItem> opportunityLineItems,
		List<String> warningMessages) {

		String currencyCode = null;

		for (OpportunityLineItem opportunityLineItem : opportunityLineItems) {
			String lineCurrencyCode = opportunityLineItem.getCurrencyIsoCode();

			if (Validator.isNull(lineCurrencyCode)) {
				continue;
			}

			if (currencyCode == null) {
				currencyCode = lineCurrencyCode;
			}
			else if (!Objects.equals(currencyCode, lineCurrencyCode)) {
				_addWarning(
					warningMessages,
					"Unable to reconcile mixed currencies for opportunity " +
						opportunity.getId());

				return _DEFAULT_CURRENCY_CODE;
			}
		}

		if (currencyCode == null) {
			return _DEFAULT_CURRENCY_CODE;
		}

		return currencyCode;
	}

	private List<OpportunityLineItem> _getOpportunityLineItems(
		JSONObject recordJSONObject) {

		List<OpportunityLineItem> opportunityLineItems = new ArrayList<>();

		JSONArray opportunityLineItemsJSONArray = recordJSONObject.optJSONArray(
			"opportunityLineItems");

		if (opportunityLineItemsJSONArray == null) {
			return opportunityLineItems;
		}

		for (int i = 0; i < opportunityLineItemsJSONArray.length(); i++) {
			opportunityLineItems.add(
				new OpportunityLineItem(
					opportunityLineItemsJSONArray.getJSONObject(i)));
		}

		return opportunityLineItems;
	}

	private List<ProjectContactRole> _getProjectContactRoles(
		JSONObject recordJSONObject) {

		List<ProjectContactRole> projectContactRoles = new ArrayList<>();

		JSONArray projectContactRolesJSONArray = recordJSONObject.optJSONArray(
			"projectContactRoles");

		if (projectContactRolesJSONArray == null) {
			return projectContactRoles;
		}

		for (int i = 0; i < projectContactRolesJSONArray.length(); i++) {
			projectContactRoles.add(
				new ProjectContactRole(
					projectContactRolesJSONArray.getJSONObject(i)));
		}

		return projectContactRoles;
	}

	private boolean _hasProductFamily(Opportunity opportunity) {
		String productFamily = opportunity.getProductFamily();

		if (Validator.isNull(productFamily)) {
			return false;
		}

		for (String productFamilyToken : _PRODUCT_FAMILY_TOKENS) {
			if (productFamily.contains(productFamilyToken)) {
				return true;
			}
		}

		return false;
	}

	private boolean _isProvisioned(Order order) {
		if (order == null) {
			return false;
		}

		return ArrayUtil.isNotEmpty(order.getOrderItems());
	}

	private boolean _isValidOpportunity(Opportunity opportunity) {
		if (StringUtil.equalsIgnoreCase(
				opportunity.getType(), OpportunityConstants.TYPE_RENEWAL)) {

			return Objects.equals(
				opportunity.getStageName(), _STAGE_NAME_CLOSED_LOST);
		}

		return Objects.equals(
			opportunity.getStageName(), _STAGE_NAME_CLOSED_WON);
	}

	private void _processProvisioningRecord(JSONObject recordJSONObject)
		throws Exception {

		Opportunity opportunity = new Opportunity(
			recordJSONObject.getJSONObject("opportunity"));

		if (!_hasProductFamily(opportunity)) {
			if (_log.isInfoEnabled()) {
				_log.info(
					"Skipping opportunity " + opportunity.getId() +
						" without a valid product family");
			}

			return;
		}

		if (!_isValidOpportunity(opportunity)) {
			if (_log.isInfoEnabled()) {
				_log.info(
					"Skipping opportunity " + opportunity.getId() +
						" that is not closed won or a closed lost renewal");
			}

			return;
		}

		List<OpportunityLineItem> opportunityLineItems =
			_getOpportunityLineItems(recordJSONObject);

		if (opportunityLineItems.isEmpty()) {
			if (_log.isInfoEnabled()) {
				_log.info(
					"Skipping opportunity " + opportunity.getId() +
						" without line items");
			}

			return;
		}

		List<String> warningMessages = new ArrayList<>();

		Account salesforceAccount = new Account(
			recordJSONObject.optJSONObject("account", new JSONObject()),
			recordJSONObject.getJSONObject("opportunity"));

		Order order = _commerceOrderService.fetchOrderByExternalReferenceCode(
			opportunity.getId());

		if (_isProvisioned(order)) {
			_addWarning(
				warningMessages,
				StringBundler.concat(
					"The opportunity ", opportunity.getId(),
					" was not provisioned automatically since an order with ",
					"this opportunity already exists"));

			com.liferay.headless.admin.user.client.dto.v1_0.Account account =
				_accountService.fetchAccountByExternalReferenceCode(
					opportunity.getAccountId());

			if ((account != null) &&
				!Objects.equals(opportunity.getProductFamily(), "P")) {

				_provisioningIssueService.addOpportunityInvoicedIssue(
					account, salesforceAccount, opportunity,
					opportunityLineItems, warningMessages);
			}

			return;
		}

		List<OpportunityLineItem> provisionableOpportunityLineItems =
			new ArrayList<>();
		List<OpportunityLineItem> realignmentOpportunityLineItems =
			new ArrayList<>();

		for (OpportunityLineItem opportunityLineItem : opportunityLineItems) {
			if (opportunityLineItem.isRealignment()) {
				realignmentOpportunityLineItems.add(opportunityLineItem);
			}
			else {
				provisionableOpportunityLineItems.add(opportunityLineItem);
			}
		}

		if (Validator.isNull(salesforceAccount.getId())) {
			_addWarning(
				warningMessages,
				"Unable to provision opportunity " + opportunity.getId() +
					" without an account");

			return;
		}

		com.liferay.one.model.Project project = _projectService.fetchProject(
			opportunity.getProjectId());

		if ((project == null) &&
			StringUtil.equalsIgnoreCase(
				opportunity.getType(),
				OpportunityConstants.TYPE_EXISTING_BUSINESS)) {

			_addWarning(
				warningMessages,
				StringBundler.concat(
					"The opportunity type is ", opportunity.getType(),
					" and the project does not exist"));
		}
		else if ((project != null) &&
				 (StringUtil.equalsIgnoreCase(
					 opportunity.getType(),
					 OpportunityConstants.TYPE_NEW_BUSINESS) ||
				  StringUtil.equalsIgnoreCase(
					  opportunity.getType(),
					  OpportunityConstants.
						  TYPE_NEW_PROJECT_EXISTING_BUSINESS))) {

			_addWarning(
				warningMessages,
				StringBundler.concat(
					"The opportunity type is ", opportunity.getType(),
					" and the project already exists"));
		}

		_accountService.upsertAccount(
			salesforceAccount, opportunity.getSoldBy());

		com.liferay.headless.admin.user.client.dto.v1_0.Account account =
			_accountService.fetchAccountByExternalReferenceCode(
				salesforceAccount.getId());

		if (account == null) {
			_addWarning(
				warningMessages,
				"Unable to find account " + salesforceAccount.getId());

			return;
		}

		if (_accountService.hasDuplicateAccountName(
				account.getName(), account.getExternalReferenceCode())) {

			_addWarning(
				warningMessages,
				"Another account already uses the name " + account.getName());
		}

		Project salesforceProject = null;

		JSONObject projectJSONObject = recordJSONObject.optJSONObject(
			"project");

		if (projectJSONObject != null) {
			salesforceProject = new Project(projectJSONObject);

			_projectService.upsertProject(
				opportunity.getAccountId(), salesforceProject);
		}

		Contract contract = _contractService.fetchLatestContractByOpportunityId(
			opportunity.getId());

		Long contractId = null;

		if (contract != null) {
			contractId = contract.getId();

			if ((salesforceProject != null) &&
				Validator.isNull(contract.getProjectExternalReferenceCode())) {

				_contractService.attachContractToProject(
					contract.getId(), salesforceProject.getId());
			}
		}
		else {
			_addWarning(
				warningMessages,
				"Unable to find a contract for opportunity " +
					opportunity.getId());
		}

		if (!realignmentOpportunityLineItems.isEmpty()) {
			if (Validator.isNull(
					opportunity.getAmendedContractOpportunityId())) {

				for (OpportunityLineItem realignmentOpportunityLineItem :
						realignmentOpportunityLineItems) {

					String productName =
						realignmentOpportunityLineItem.getProductName();

					_addWarning(
						warningMessages,
						"Unable to find a parent opportunity for amended " +
							"line " + productName);
				}
			}
			else {
				Order parentOrder =
					_commerceOrderService.fetchOrderByExternalReferenceCode(
						opportunity.getAmendedContractOpportunityId());

				if (parentOrder == null) {
					_addWarning(
						warningMessages,
						StringBundler.concat(
							"Unable to find the parent order ",
							opportunity.getAmendedContractOpportunityId(),
							" for amended opportunity ", opportunity.getId()));
				}
				else {
					_provisioningOrderService.cancelRealignedOrder(
						parentOrder, realignmentOpportunityLineItems,
						warningMessages);
				}
			}
		}

		boolean renewal = opportunity.isRenewal();

		if (StringUtil.equalsIgnoreCase(
				opportunity.getType(), OpportunityConstants.TYPE_RENEWAL)) {

			renewal = true;
		}

		if (renewal) {
			_provisioningOrderService.trimRenewedOrderItems(
				account.getId(), opportunity.getId(),
				provisionableOpportunityLineItems, warningMessages);
		}

		String currencyCode = _getCurrencyCode(
			opportunity, provisionableOpportunityLineItems, warningMessages);

		if (Validator.isNotNull(opportunity.getOwnerEmailAddress())) {
			UserAccount userAccount =
				_userAccountService.fetchUserAccountByEmailAddress(
					opportunity.getOwnerEmailAddress());

			if (userAccount == null) {
				_addWarning(
					warningMessages,
					"Unable to find portal user " +
						opportunity.getOwnerEmailAddress() +
							" for opportunity creator");
			}
		}

		Order newOrder = _commerceOrderService.upsertOrder(
			account, contractId, currencyCode, opportunity,
			provisionableOpportunityLineItems, salesforceProject);

		if ((contract != null) && Validator.isNotNull(contract.getEndDate())) {
			for (OpportunityLineItem provisionableOpportunityLineItem :
					provisionableOpportunityLineItems) {

				String endDate = _toDateTime(
					provisionableOpportunityLineItem.getEndDate());

				if (Validator.isNotNull(endDate) &&
					!Objects.equals(endDate, contract.getEndDate())) {

					_addWarning(
						warningMessages,
						StringBundler.concat(
							"The end date of line ",
							provisionableOpportunityLineItem.getProductName(),
							" differs from the end date of contract ",
							contract.getExternalReferenceCode()));
				}
			}
		}

		int provisionedOrderItemCount = 0;

		for (OpportunityLineItem provisionableOpportunityLineItem :
				provisionableOpportunityLineItems) {

			Sku sku = _commerceSkuService.fetchSku(
				provisionableOpportunityLineItem.getProduct2Id());

			if (sku == null) {
				_addWarning(
					warningMessages,
					"Unable to find SKU for Salesforce product " +
						provisionableOpportunityLineItem.getProduct2Id());

				continue;
			}

			try {
				_commerceOrderItemService.upsertOrderItem(
					newOrder, provisionableOpportunityLineItem,
					opportunity.getStageName());

				provisionedOrderItemCount++;
			}
			catch (Exception exception) {
				_addWarning(
					warningMessages,
					"Unable to provision line " +
						provisionableOpportunityLineItem.getProductName());

				if (_log.isWarnEnabled()) {
					_log.warn(
						StringBundler.concat(
							"Unable to provision order item for Salesforce ",
							"product ",
							provisionableOpportunityLineItem.getProduct2Id()),
						exception);
				}
			}
		}

		if (provisionedOrderItemCount > 0) {
			try {
				_commerceOrderService.completeOrder(
					newOrder.getId(),
					CommerceOrderConstants.ORDER_PAYMENT_STATUS_NOT_REQUIRED);
			}
			catch (Exception exception) {
				_addWarning(
					warningMessages,
					"Unable to complete order " + opportunity.getId());

				if (_log.isWarnEnabled()) {
					_log.warn(
						"Unable to complete order " + newOrder.getId(),
						exception);
				}
			}
		}

		_provisioningSubdomainService.provisionSubdomain(
			account, provisionableOpportunityLineItems);

		List<Long> userIds = new ArrayList<>();

		if (!StringUtil.equalsIgnoreCase(
				opportunity.getType(), OpportunityConstants.TYPE_RENEWAL)) {

			userIds = _provisioningContactService.addProjectContacts(
				account, _getProjectContactRoles(recordJSONObject),
				salesforceProject, warningMessages);
		}

		_provisioningEmailService.sendWelcomeEmails(
			account, opportunity.getType(), userIds);

		if (!Objects.equals(opportunity.getProductFamily(), "P")) {
			_provisioningIssueService.addOpportunityInvoicedIssue(
				account, salesforceAccount, opportunity,
				provisionableOpportunityLineItems, warningMessages);
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

	private static final String _DEFAULT_CURRENCY_CODE = "USD";

	private static final String[] _PRODUCT_FAMILY_TOKENS = {"E", "P", "S"};

	private static final String _STAGE_NAME_CLOSED_LOST = "Closed Lost";

	private static final String _STAGE_NAME_CLOSED_WON = "Closed Won";

	private static final Log _log = LogFactory.getLog(
		SalesforceOpportunityPubsubSubscriber.class);

	private static final Pattern _datePattern = Pattern.compile(
		"\\d{4}-\\d{2}-\\d{2}");

	@Autowired
	private AccountService _accountService;

	@Autowired
	private CommerceOrderItemService _commerceOrderItemService;

	@Autowired
	private CommerceOrderService _commerceOrderService;

	@Autowired
	private CommerceSkuService _commerceSkuService;

	@Autowired
	private ContractService _contractService;

	@Value("${liferay.one.salesforce.opportunity.pubsub.subscriber.project.id}")
	private String _projectId;

	@Autowired
	private ProjectService _projectService;

	@Autowired
	private ProvisioningContactService _provisioningContactService;

	@Autowired
	private ProvisioningEmailService _provisioningEmailService;

	@Autowired
	private ProvisioningIssueService _provisioningIssueService;

	@Autowired
	private ProvisioningOrderService _provisioningOrderService;

	@Autowired
	private ProvisioningSubdomainService _provisioningSubdomainService;

	@Value(
		"${liferay.one.salesforce.opportunity.pubsub.subscriber.subscription}"
	)
	private String _subscription;

	@Value("${liferay.one.salesforce.opportunity.pubsub.subscriber.topic}")
	private String _topic;

	@Autowired
	private UserAccountService _userAccountService;

}