/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.jira.synchronizer;

import com.liferay.headless.admin.user.client.dto.v1_0.Account;
import com.liferay.headless.admin.user.client.dto.v1_0.AccountBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.AccountContactInformation;
import com.liferay.headless.admin.user.client.dto.v1_0.Organization;
import com.liferay.headless.admin.user.client.dto.v1_0.PostalAddress;
import com.liferay.headless.admin.user.client.dto.v1_0.Role;
import com.liferay.headless.admin.user.client.dto.v1_0.RoleBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.one.jira.converter.ExternalLinkConverter;
import com.liferay.one.jira.model.JiraBusinessEvent;
import com.liferay.one.jira.service.JiraBusinessEventService;
import com.liferay.one.model.AccountSupportInfo;
import com.liferay.one.model.EntitlementDefinition;
import com.liferay.one.model.Project;
import com.liferay.one.model.Property;
import com.liferay.one.service.CommerceOrderService;
import com.liferay.one.service.EntitlementService;
import com.liferay.one.service.OrganizationService;
import com.liferay.one.service.ProjectService;
import com.liferay.one.service.PropertyService;
import com.liferay.one.service.RoleService;
import com.liferay.one.service.UserAccountService;
import com.liferay.one.util.FindUtil;
import com.liferay.one.util.role.EmployeeRoles;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Drew Brokke
 */
public class AccountSyncModel {

	public AccountSyncModel(
		Account account, CommerceOrderService commerceOrderService,
		EntitlementService entitlementService,
		ExternalLinkConverter externalLinkConverter,
		JiraBusinessEventService jiraBusinessEventService,
		OrganizationService organizationService, ProjectService projectService,
		PropertyService propertyService, RoleService roleService,
		UserAccountService userAccountService) {

		_account = account;
		_commerceOrderService = commerceOrderService;
		_entitlementService = entitlementService;
		_externalLinkConverter = externalLinkConverter;
		_jiraBusinessEventService = jiraBusinessEventService;
		_organizationService = organizationService;
		_projectService = projectService;
		_propertyService = propertyService;
		_roleService = roleService;
		_userAccountService = userAccountService;
	}

	public Account getAccount() {
		return _account;
	}

	public List<Organization> getAccountOrganizations() throws Exception {
		if (_accountOrganizations == null) {
			_accountOrganizations =
				_organizationService.getAccountOrganizations(_account.getId());
		}

		return _accountOrganizations;
	}

	public Map<String, Role> getAccountRolesByExternalReferenceCode()
		throws Exception {

		if (_accountRolesByExternalReferenceCode == null) {
			_accountRolesByExternalReferenceCode = new LinkedHashMap<>();

			for (Role role : _roleService.getAccountRoles()) {
				_accountRolesByExternalReferenceCode.put(
					role.getExternalReferenceCode(), role);
			}
		}

		return _accountRolesByExternalReferenceCode;
	}

	public List<UserAccount> getAccountUserAccounts() throws Exception {
		if (_accountUserAccounts == null) {
			_accountUserAccounts = _userAccountService.getAccountUserAccounts(
				_account.getId());
		}

		return _accountUserAccounts;
	}

	public List<EntitlementDefinition> getActiveEntitlementDefinitions()
		throws Exception {

		if (_activeEntitlementDefinitions == null) {
			_activeEntitlementDefinitions =
				_entitlementService.getActiveEntitlementDefinitions(
					_account.getId());
		}

		return _activeEntitlementDefinitions;
	}

	public String getBusinessEventsFieldValue() throws Exception {
		if (_businessEventsFieldValue == null) {
			_businessEventsFieldValue = _toBusinessEventsFieldValue();
		}

		return _businessEventsFieldValue;
	}

	public List<UserAccount> getCustomerUserAccounts() throws Exception {
		UserAccountBucket userAccountBucket = _getUserAccountBucket();

		return userAccountBucket.getCustomerUserAccounts();
	}

	public List<Property> getExternalLinkProperties() throws Exception {
		if (_externalLinkProperties != null) {
			return _externalLinkProperties;
		}

		_externalLinkProperties = new ArrayList<>();

		for (Property property : _getAccountProperties()) {
			if (_externalLinkConverter.isExternalLinkProperty(property)) {
				_externalLinkProperties.add(property);
			}
		}

		return _externalLinkProperties;
	}

	public String getExternalReferenceCode() {
		return _account.getExternalReferenceCode();
	}

	public List<PostalAddress> getPostalAddresses() {
		AccountContactInformation accountContactInformation =
			_account.getAccountContactInformation();

		if (accountContactInformation == null) {
			return null;
		}

		return ListUtil.fromArray(
			accountContactInformation.getPostalAddresses());
	}

	public List<Project> getProjects() throws Exception {
		if (_projects == null) {
			_projects = _projectService.getProjects(_account.getId());
		}

		return _projects;
	}

	public String getSupportLanguage() throws Exception {
		AccountSupportInfo accountSupportInfo = _getAccountSupportInfo();

		return GetterUtil.getString(accountSupportInfo.getSupportLanguage());
	}

	public String getSupportRegion() throws Exception {
		AccountSupportInfo accountSupportInfo = _getAccountSupportInfo();

		return GetterUtil.getString(accountSupportInfo.getSupportRegion());
	}

	public List<UserAccount> getWorkerUserAccounts() throws Exception {
		UserAccountBucket userAccountBucket = _getUserAccountBucket();

		return userAccountBucket.getWorkerUserAccounts();
	}

	private void _addJiraBusinessEventLine(
		List<String> lines, String fieldName, String value) {

		if (Validator.isNull(value)) {
			return;
		}

		lines.add(fieldName + ": " + value);
	}

	private List<Property> _getAccountProperties() throws Exception {
		if (_accountProperties == null) {
			_accountProperties = _propertyService.getAccountProperties(
				_account.getId());
		}

		return _accountProperties;
	}

	private AccountSupportInfo _getAccountSupportInfo() throws Exception {
		if (_accountSupportInfo == null) {
			_accountSupportInfo = _commerceOrderService.getAccountSupportInfo(
				_account.getId(), _account.getDefaultBillingAddressId());
		}

		return _accountSupportInfo;
	}

	private UserAccountBucket _getUserAccountBucket() throws Exception {
		if (_userAccountBucket != null) {
			return _userAccountBucket;
		}

		_userAccountBucket = new UserAccountBucket();

		for (UserAccount accountUserAccount : getAccountUserAccounts()) {
			AccountBrief accountBrief = FindUtil.findFirst(
				accountUserAccount.getAccountBriefs(),
				accountBrief1 -> Objects.equals(
					_account.getExternalReferenceCode(),
					accountBrief1.getExternalReferenceCode()));

			if (accountBrief == null) {
				_log.error(
					"accountBrief is null for user account = " +
						accountUserAccount);

				continue;
			}

			RoleBrief roleBrief = FindUtil.findFirst(
				accountBrief.getRoleBriefs(),
				roleBrief1 -> _employeeRoleNames.contains(
					roleBrief1.getName()));

			if (roleBrief != null) {
				_userAccountBucket.addWorkerUserAccount(accountUserAccount);
			}
			else {
				_userAccountBucket.addCustomerUserAccount(accountUserAccount);
			}
		}

		return _userAccountBucket;
	}

	private String _toBusinessEventFieldValuePart(
		JiraBusinessEvent jiraBusinessEvent) {

		List<String> lines = new ArrayList<>();

		_addJiraBusinessEventLine(lines, "name", jiraBusinessEvent.getName());
		_addJiraBusinessEventLine(
			lines, "targetGoLiveDateTime",
			jiraBusinessEvent.getPlannedEventDate());
		_addJiraBusinessEventLine(
			lines, "description", jiraBusinessEvent.getDescription());
		_addJiraBusinessEventLine(
			lines, "type", jiraBusinessEvent.getEventTypeName());
		_addJiraBusinessEventLine(
			lines, "currentVersion",
			jiraBusinessEvent.getCurrentLiferayVersionName());
		_addJiraBusinessEventLine(
			lines, "newVersion", jiraBusinessEvent.getNewLiferayVersionName());

		return StringUtil.merge(lines, ",\n");
	}

	private String _toBusinessEventsFieldValue() throws Exception {
		List<String> parts = new ArrayList<>();

		List<JiraBusinessEvent> jiraBusinessEvents =
			_jiraBusinessEventService.getJiraBusinessEvents(
				_account.getExternalReferenceCode());

		for (JiraBusinessEvent jiraBusinessEvent : jiraBusinessEvents) {
			String part = _toBusinessEventFieldValuePart(jiraBusinessEvent);

			if (Validator.isNotNull(part)) {
				parts.add(part);
			}
		}

		return StringUtil.merge(parts, "\n\n");
	}

	private static final Log _log = LogFactory.getLog(AccountSyncModel.class);

	private final Account _account;
	private List<Organization> _accountOrganizations;
	private List<Property> _accountProperties;
	private Map<String, Role> _accountRolesByExternalReferenceCode;
	private AccountSupportInfo _accountSupportInfo;
	private List<UserAccount> _accountUserAccounts;
	private List<EntitlementDefinition> _activeEntitlementDefinitions;
	private String _businessEventsFieldValue;
	private final CommerceOrderService _commerceOrderService;
	private final List<String> _employeeRoleNames = EmployeeRoles.getNames();
	private final EntitlementService _entitlementService;
	private final ExternalLinkConverter _externalLinkConverter;
	private List<Property> _externalLinkProperties;
	private final JiraBusinessEventService _jiraBusinessEventService;
	private final OrganizationService _organizationService;
	private List<Project> _projects;
	private final ProjectService _projectService;
	private final PropertyService _propertyService;
	private final RoleService _roleService;
	private UserAccountBucket _userAccountBucket;
	private final UserAccountService _userAccountService;

}