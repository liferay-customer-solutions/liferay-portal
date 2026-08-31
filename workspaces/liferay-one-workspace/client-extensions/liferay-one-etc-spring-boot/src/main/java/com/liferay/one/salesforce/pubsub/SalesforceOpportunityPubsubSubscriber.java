/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.salesforce.pubsub;

import com.liferay.headless.admin.user.client.dto.v1_0.Account;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.Order;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.OrderItem;
import com.liferay.one.constants.CommerceOrderConstants;
import com.liferay.one.constants.OpportunityConstants;
import com.liferay.one.model.Contract;
import com.liferay.one.model.Project;
import com.liferay.one.pubsub.Message;
import com.liferay.one.pubsub.subscriber.BasePubsubSubscriber;
import com.liferay.one.salesforce.model.SalesforceAccount;
import com.liferay.one.salesforce.model.SalesforceOpportunity;
import com.liferay.one.salesforce.model.SalesforceOpportunityLineItem;
import com.liferay.one.salesforce.model.SalesforceProject;
import com.liferay.one.salesforce.model.SalesforceProjectContactRole;
import com.liferay.one.service.AccountService;
import com.liferay.one.service.CommerceOrderItemService;
import com.liferay.one.service.CommerceOrderService;
import com.liferay.one.service.CommerceSkuService;
import com.liferay.one.service.ContractService;
import com.liferay.one.service.EntitlementService;
import com.liferay.one.service.ProjectService;
import com.liferay.one.service.ProvisioningContactService;
import com.liferay.one.service.ProvisioningEmailService;
import com.liferay.one.service.ProvisioningEnvironmentService;
import com.liferay.one.service.ProvisioningIssueService;
import com.liferay.one.service.ProvisioningOrderService;
import com.liferay.one.service.ProvisioningSubdomainService;
import com.liferay.one.service.UserAccountService;
import com.liferay.one.util.OrderItemUtil;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
		SalesforceOpportunity salesforceOpportunity,
		List<SalesforceOpportunityLineItem> salesforceOpportunityLineItems,
		List<String> warningMessages) {

		String currencyCode = null;

		for (SalesforceOpportunityLineItem salesforceOpportunityLineItem :
				salesforceOpportunityLineItems) {

			String lineCurrencyCode =
				salesforceOpportunityLineItem.getCurrencyIsoCode();

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
						salesforceOpportunity.getId());

				return _DEFAULT_CURRENCY_CODE;
			}
		}

		if (currencyCode == null) {
			return _DEFAULT_CURRENCY_CODE;
		}

		return currencyCode;
	}

	private List<SalesforceOpportunityLineItem>
		_getSalesforceOpportunityLineItems(JSONObject recordJSONObject) {

		List<SalesforceOpportunityLineItem> salesforceOpportunityLineItems =
			new ArrayList<>();

		JSONArray opportunityLineItemsJSONArray = recordJSONObject.optJSONArray(
			"opportunityLineItems");

		if (opportunityLineItemsJSONArray == null) {
			return salesforceOpportunityLineItems;
		}

		for (int i = 0; i < opportunityLineItemsJSONArray.length(); i++) {
			salesforceOpportunityLineItems.add(
				new SalesforceOpportunityLineItem(
					opportunityLineItemsJSONArray.getJSONObject(i)));
		}

		return salesforceOpportunityLineItems;
	}

	private List<SalesforceProjectContactRole>
		_getSalesforceProjectContactRoles(JSONObject recordJSONObject) {

		List<SalesforceProjectContactRole> salesforceProjectContactRoles =
			new ArrayList<>();

		JSONArray projectContactRolesJSONArray = recordJSONObject.optJSONArray(
			"projectContactRoles");

		if (projectContactRolesJSONArray == null) {
			return salesforceProjectContactRoles;
		}

		for (int i = 0; i < projectContactRolesJSONArray.length(); i++) {
			salesforceProjectContactRoles.add(
				new SalesforceProjectContactRole(
					projectContactRolesJSONArray.getJSONObject(i)));
		}

		return salesforceProjectContactRoles;
	}

	private boolean _hasProductFamily(
		SalesforceOpportunity salesforceOpportunity) {

		String productFamily = salesforceOpportunity.getProductFamily();

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

	private boolean _isCompleted(Order order) {
		if (order == null) {
			return false;
		}

		return Objects.equals(
			order.getOrderStatus(),
			CommerceOrderConstants.ORDER_STATUS_COMPLETED);
	}

	private boolean _isProvisioned(Order order) {
		if (order == null) {
			return false;
		}

		return ArrayUtil.isNotEmpty(order.getOrderItems());
	}

	private boolean _isValidOpportunity(
		SalesforceOpportunity salesforceOpportunity) {

		if (StringUtil.equalsIgnoreCase(
				salesforceOpportunity.getType(),
				OpportunityConstants.TYPE_RENEWAL)) {

			return Objects.equals(
				salesforceOpportunity.getStageName(), _STAGE_NAME_CLOSED_LOST);
		}

		return Objects.equals(
			salesforceOpportunity.getStageName(), _STAGE_NAME_CLOSED_WON);
	}

	private void _processProvisioningRecord(JSONObject recordJSONObject)
		throws Exception {

		SalesforceOpportunity salesforceOpportunity = new SalesforceOpportunity(
			recordJSONObject.getJSONObject("opportunity"));

		if (!_hasProductFamily(salesforceOpportunity)) {
			if (_log.isInfoEnabled()) {
				_log.info(
					"Skipping opportunity " + salesforceOpportunity.getId() +
						" without a valid product family");
			}

			return;
		}

		if (!_isValidOpportunity(salesforceOpportunity)) {
			if (_log.isInfoEnabled()) {
				_log.info(
					"Skipping opportunity " + salesforceOpportunity.getId() +
						" that is not closed won or a closed lost renewal");
			}

			return;
		}

		List<SalesforceOpportunityLineItem> salesforceOpportunityLineItems =
			_getSalesforceOpportunityLineItems(recordJSONObject);

		if (salesforceOpportunityLineItems.isEmpty()) {
			if (_log.isInfoEnabled()) {
				_log.info(
					"Skipping opportunity " + salesforceOpportunity.getId() +
						" without line items");
			}

			return;
		}

		List<String> warningMessages = new ArrayList<>();

		SalesforceAccount salesforceAccount = new SalesforceAccount(
			recordJSONObject.optJSONObject("account", new JSONObject()),
			recordJSONObject.getJSONObject("opportunity"));

		Order order = _commerceOrderService.fetchOrderByExternalReferenceCode(
			salesforceOpportunity.getId());

		boolean reprocessing = _isProvisioned(order);

		if (reprocessing && _log.isInfoEnabled()) {
			_log.info(
				"Reprocessing opportunity " + salesforceOpportunity.getId());
		}

		List<SalesforceOpportunityLineItem>
			provisionableSalesforceOpportunityLineItems = new ArrayList<>();
		List<SalesforceOpportunityLineItem>
			realignmentSalesforceOpportunityLineItems = new ArrayList<>();

		for (SalesforceOpportunityLineItem salesforceOpportunityLineItem :
				salesforceOpportunityLineItems) {

			if (salesforceOpportunityLineItem.isRealignment()) {
				realignmentSalesforceOpportunityLineItems.add(
					salesforceOpportunityLineItem);
			}
			else {
				provisionableSalesforceOpportunityLineItems.add(
					salesforceOpportunityLineItem);
			}
		}

		if (Validator.isNull(salesforceAccount.getId())) {
			_addWarning(
				warningMessages,
				"Unable to provision opportunity " +
					salesforceOpportunity.getId() + " without an account");

			return;
		}

		Project project = _projectService.fetchProject(
			salesforceOpportunity.getProjectId());

		if ((project == null) &&
			StringUtil.equalsIgnoreCase(
				salesforceOpportunity.getType(),
				OpportunityConstants.TYPE_EXISTING_BUSINESS)) {

			_addWarning(
				warningMessages,
				StringBundler.concat(
					"The opportunity type is ", salesforceOpportunity.getType(),
					" and the project does not exist"));
		}
		else if ((project != null) &&
				 (StringUtil.equalsIgnoreCase(
					 salesforceOpportunity.getType(),
					 OpportunityConstants.TYPE_NEW_BUSINESS) ||
				  StringUtil.equalsIgnoreCase(
					  salesforceOpportunity.getType(),
					  OpportunityConstants.
						  TYPE_NEW_PROJECT_EXISTING_BUSINESS))) {

			_addWarning(
				warningMessages,
				StringBundler.concat(
					"The opportunity type is ", salesforceOpportunity.getType(),
					" and the project already exists"));
		}

		_accountService.upsertAccount(
			salesforceAccount, salesforceOpportunity.getSoldBy());

		Account account = _accountService.fetchAccountByExternalReferenceCode(
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

		SalesforceProject salesforceProject = null;

		JSONObject projectJSONObject = recordJSONObject.optJSONObject(
			"project");

		if (projectJSONObject != null) {
			salesforceProject = new SalesforceProject(projectJSONObject);

			_projectService.upsertProject(
				salesforceOpportunity.getAccountId(), salesforceProject);
		}

		Contract contract = _contractService.fetchLatestContractByOpportunityId(
			salesforceOpportunity.getId());

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
					salesforceOpportunity.getId());
		}

		if (!realignmentSalesforceOpportunityLineItems.isEmpty()) {
			if (Validator.isNull(
					salesforceOpportunity.getAmendedContractOpportunityId())) {

				for (SalesforceOpportunityLineItem
						realignmentSalesforceOpportunityLineItem :
							realignmentSalesforceOpportunityLineItems) {

					String productName =
						realignmentSalesforceOpportunityLineItem.
							getProductName();

					_addWarning(
						warningMessages,
						"Unable to find a parent opportunity for amended " +
							"line " + productName);
				}
			}
			else {
				_provisioningOrderService.trimRealignedOrderItems(
					account.getId(), salesforceOpportunity.getId(),
					salesforceOpportunity.getAmendedContractOpportunityId(),
					realignmentSalesforceOpportunityLineItems, warningMessages);
			}
		}

		boolean renewal = salesforceOpportunity.isRenewal();

		if (StringUtil.equalsIgnoreCase(
				salesforceOpportunity.getType(),
				OpportunityConstants.TYPE_RENEWAL)) {

			renewal = true;
		}

		if (renewal) {
			_provisioningOrderService.trimRenewedOrderItems(
				account.getId(), salesforceOpportunity.getId(),
				provisionableSalesforceOpportunityLineItems, warningMessages);
		}

		String currencyCode = _getCurrencyCode(
			salesforceOpportunity, provisionableSalesforceOpportunityLineItems,
			warningMessages);

		if (Validator.isNotNull(salesforceOpportunity.getOwnerEmailAddress())) {
			UserAccount userAccount =
				_userAccountService.fetchUserAccountByEmailAddress(
					salesforceOpportunity.getOwnerEmailAddress());

			if (userAccount == null) {
				_addWarning(
					warningMessages,
					"Unable to find portal user " +
						salesforceOpportunity.getOwnerEmailAddress() +
							" for opportunity creator");
			}
		}

		Order newOrder = _commerceOrderService.upsertOrder(
			account, contractId, currencyCode, salesforceOpportunity,
			provisionableSalesforceOpportunityLineItems, salesforceProject);

		if ((contract != null) && (contract.getEndDateInstant() != null)) {
			for (SalesforceOpportunityLineItem
					provisionableSalesforceOpportunityLineItem :
						provisionableSalesforceOpportunityLineItems) {

				if (!Objects.equals(
						provisionableSalesforceOpportunityLineItem.
							getEndDateInstant(),
						contract.getEndDateInstant())) {

					_addWarning(
						warningMessages,
						StringBundler.concat(
							"The end date of line ",
							provisionableSalesforceOpportunityLineItem.
								getProductName(),
							" differs from the end date of contract ",
							contract.getExternalReferenceCode()));
				}
			}
		}

		int provisionedOrderItemCount = 0;

		for (SalesforceOpportunityLineItem
				provisionableSalesforceOpportunityLineItem :
					provisionableSalesforceOpportunityLineItems) {

			String product2Id =
				provisionableSalesforceOpportunityLineItem.getProduct2Id();

			if (_commerceSkuService.fetchSku(product2Id) == null) {
				_addWarning(
					warningMessages,
					"Unable to find SKU for Salesforce product " + product2Id);

				continue;
			}

			try {
				_commerceOrderItemService.upsertOrderItem(
					newOrder, provisionableSalesforceOpportunityLineItem,
					salesforceOpportunity.getStageName());

				provisionedOrderItemCount++;

				OrderItem existingOrderItem = OrderItemUtil.fetchOrderItem(
					provisionableSalesforceOpportunityLineItem.getId(), order);

				if (existingOrderItem != null) {
					try {
						_entitlementService.updateEntitlements(
							existingOrderItem.getId());
					}
					catch (Exception exception) {
						_log.error(
							"Unable to update entitlements for order item " +
								existingOrderItem.getId(),
							exception);
					}
				}
			}
			catch (Exception exception) {
				String productName =
					provisionableSalesforceOpportunityLineItem.getProductName();

				_addWarning(
					warningMessages, "Unable to provision line " + productName);

				if (_log.isWarnEnabled()) {
					_log.warn(
						StringBundler.concat(
							"Unable to provision order item for Salesforce ",
							"product ",
							provisionableSalesforceOpportunityLineItem.
								getProduct2Id()),
						exception);
				}
			}
		}

		if ((provisionedOrderItemCount > 0) && !_isCompleted(order)) {
			try {
				_commerceOrderService.completeOrder(
					newOrder.getId(),
					CommerceOrderConstants.ORDER_PAYMENT_STATUS_NOT_REQUIRED);
			}
			catch (Exception exception) {
				_addWarning(
					warningMessages,
					"Unable to complete order " +
						salesforceOpportunity.getId());

				if (_log.isWarnEnabled()) {
					_log.warn(
						"Unable to complete order " + newOrder.getId(),
						exception);
				}
			}
		}

		_provisioningEnvironmentService.provisionCloudNativeEnvironments(
			account, contract, provisionableSalesforceOpportunityLineItems);

		_provisioningSubdomainService.provisionSubdomain(
			account, provisionableSalesforceOpportunityLineItems);

		List<Long> userIds = new ArrayList<>();

		if (!StringUtil.equalsIgnoreCase(
				salesforceOpportunity.getType(),
				OpportunityConstants.TYPE_RENEWAL)) {

			userIds = _provisioningContactService.addProjectContacts(
				account, _getSalesforceProjectContactRoles(recordJSONObject),
				salesforceProject, warningMessages);
		}

		if (reprocessing) {
			_provisioningEmailService.sendAssignedWelcomeEmails(
				account, userIds);
		}
		else {
			_provisioningEmailService.sendWelcomeEmails(
				account, salesforceOpportunity.getType(), userIds);
		}

		if (!reprocessing &&
			!Objects.equals(salesforceOpportunity.getProductFamily(), "P")) {

			_provisioningIssueService.addOpportunityInvoicedIssue(
				account, salesforceAccount, salesforceOpportunity,
				provisionableSalesforceOpportunityLineItems, warningMessages);
		}
	}

	private static final String _DEFAULT_CURRENCY_CODE = "USD";

	private static final String[] _PRODUCT_FAMILY_TOKENS = {"E", "P", "S"};

	private static final String _STAGE_NAME_CLOSED_LOST = "Closed Lost";

	private static final String _STAGE_NAME_CLOSED_WON = "Closed Won";

	private static final Log _log = LogFactory.getLog(
		SalesforceOpportunityPubsubSubscriber.class);

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

	@Autowired
	private EntitlementService _entitlementService;

	@Value("${liferay.one.salesforce.opportunity.pubsub.subscriber.project.id}")
	private String _projectId;

	@Autowired
	private ProjectService _projectService;

	@Autowired
	private ProvisioningContactService _provisioningContactService;

	@Autowired
	private ProvisioningEmailService _provisioningEmailService;

	@Autowired
	private ProvisioningEnvironmentService _provisioningEnvironmentService;

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