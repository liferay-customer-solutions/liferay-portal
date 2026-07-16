/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.jira.service;

import com.liferay.headless.admin.user.client.dto.v1_0.Account;
import com.liferay.headless.admin.user.client.dto.v1_0.AccountBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.Organization;
import com.liferay.headless.admin.user.client.dto.v1_0.RoleBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.one.jira.constants.AccountConstants;
import com.liferay.one.jira.converter.AccountConverter;
import com.liferay.one.jira.converter.ContactConverter;
import com.liferay.one.jira.converter.EntitlementConverter;
import com.liferay.one.jira.converter.ExternalLinkConverter;
import com.liferay.one.jira.converter.PostalAddressConverter;
import com.liferay.one.jira.converter.TeamConverter;
import com.liferay.one.jira.model.BusinessEvent;
import com.liferay.one.jira.model.JiraAssetObject;
import com.liferay.one.model.Entitlement;
import com.liferay.one.model.EntitlementDefinition;
import com.liferay.one.model.Project;
import com.liferay.one.model.Property;
import com.liferay.one.service.AccountService;
import com.liferay.one.service.CommerceOrderService;
import com.liferay.one.service.EntitlementDefinitionService;
import com.liferay.one.service.EntitlementService;
import com.liferay.one.service.OrganizationService;
import com.liferay.one.service.ProjectService;
import com.liferay.one.service.PropertyService;
import com.liferay.one.service.UserAccountService;
import com.liferay.petra.function.transform.TransformUtil;
import com.liferay.petra.string.CharPool;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Drew Brokke
 */
@Component
public class AccountSyncService {

	public void syncAccount(Account account) throws Exception {
		if (_log.isInfoEnabled()) {
			_log.info(
				"Syncing account " + account.getExternalReferenceCode() +
					" to JSM");
		}

		_syncAccountAsset(
			account, account.getExternalReferenceCode(), account.getName());

		for (Project project : _projectService.getProjects(account.getId())) {
			syncProject(project);
		}
	}

	public void syncProject(Project project) throws Exception {
		if (_log.isInfoEnabled()) {
			_log.info(
				"Syncing project " + project.getExternalReferenceCode() +
					" to JSM");
		}

		_syncAccountAsset(
			_accountService.getAccount(
				project.getAccountExternalReferenceCode()),
			project.getExternalReferenceCode(), project.getName());
	}

	private void _addBusinessEventLine(
		List<String> lines, String fieldName, String value) {

		if (Validator.isNull(value)) {
			return;
		}

		lines.add(fieldName + ": " + value);
	}

	private <T> T _findFirst(List<T> list, Predicate<T> predicate) {
		if (ListUtil.isEmpty(list)) {
			return null;
		}

		for (T t : list) {
			if (predicate.test(t)) {
				return t;
			}
		}

		return null;
	}

	private <T> T _findFirst(T[] arr, Predicate<T> predicate) {
		return _findFirst(Arrays.asList(arr), predicate);
	}

	private AccountUserAccountBucket _getAccountUserAccountBucket(
			Account account)
		throws Exception {

		AccountUserAccountBucket accountUserAccountBucket =
			new AccountUserAccountBucket();

		for (UserAccount accountUserAccount :
				_userAccountService.getAccountUserAccounts(account.getId())) {

			AccountBrief accountBrief = _findFirst(
				Arrays.asList(accountUserAccount.getAccountBriefs()),
				accountBrief1 -> Objects.equals(
					account.getExternalReferenceCode(),
					accountBrief1.getExternalReferenceCode()));

			if (accountBrief == null) {
				_log.error(
					"accountBrief is null for user account = " +
						accountUserAccount);

				continue;
			}

			RoleBrief roleBrief = _findFirst(
				accountBrief.getRoleBriefs(),
				roleBrief1 -> _employeeRoleNames.contains(
					roleBrief1.getName()));

			if (roleBrief != null) {
				accountUserAccountBucket.addWorkerUserAccount(
					accountUserAccount);
			}
			else {
				accountUserAccountBucket.addCustomerUserAccount(
					accountUserAccount);
			}
		}

		return accountUserAccountBucket;
	}

	private List<Organization> _getAssignedTeamOrganizations(Account account) {
		try {
			return _organizationService.getAccountOrganizations(
				account.getId());
		}
		catch (Exception exception) {
			_log.error("Could not retrieve organizations", exception);

			return null;
		}
	}

	private List<EntitlementDefinition> _getEntitlementDefinitions(
		Account account) {

		try {
			List<Entitlement> entitlements =
				_entitlementService.getEntitlements(
					"(r_accountEntryToEntitlement_accountEntryId eq '" +
						account.getId() + "')");

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
		catch (Exception exception) {
			_log.error(
				"Unable to get entitlement definitions for account " +
					account.getExternalReferenceCode(),
				exception);

			return null;
		}
	}

	private List<Property> _getExternalLinkProperties(Account account) {
		List<Property> externalLinkProperties = new ArrayList<>();

		try {
			List<Property> properties = _propertyService.getAccountProperties(
				account.getId());

			for (Property property : properties) {
				String[] split = StringUtil.split(
					property.getName(), CharPool.COLON);

				if (split.length == 2) {
					externalLinkProperties.add(property);
				}
			}
		}
		catch (Exception exception) {
			_log.error("Could not find External Link", exception);
		}

		return externalLinkProperties;
	}

	private String _getLanguage(Account account) {
		try {
			return _commerceOrderService.getSupportLanguage(
				account.getId(), account.getDefaultBillingAddressId());
		}
		catch (Exception exception) {
			_log.error("Unable to get support language", exception);

			return null;
		}
	}

	private String _getSupportRegion(Account account) {
		try {
			return _commerceOrderService.getSupportRegion(
				account.getId(), account.getDefaultBillingAddressId());
		}
		catch (Exception exception) {
			_log.error("Unable to get support region", exception);

			return null;
		}
	}

	private void _syncAccountAsset(
			Account account, String externalKey, String name)
		throws Exception {

		JiraAssetObject assetObject = _accountConverter.toAssetObject(
			account, externalKey, name);

		assetObject.setAttributeValue(
			AccountConstants.ATTRIBUTE_NAME_BUSINESS_EVENTS,
			_toBusinessEventsFieldValue(account));
		assetObject.setAttributeValue(
			AccountConstants.ATTRIBUTE_NAME_LANGUAGE, _getLanguage(account));
		assetObject.setAttributeValue(
			AccountConstants.ATTRIBUTE_NAME_SUPPORT_REGION,
			_getSupportRegion(account));

		assetObject.setAttributeValue(
			AccountConstants.ATTRIBUTE_NAME_ASSIGNED_TEAMS,
			_assetReferenceObjectService.fetchReferenceObjectIds(
				_teamConverter, _getAssignedTeamOrganizations(account),
				Organization::getExternalReferenceCode));

		AccountUserAccountBucket accountUserAccountBucket =
			_getAccountUserAccountBucket(account);

		assetObject.setAttributeValue(
			AccountConstants.ATTRIBUTE_NAME_CUSTOMER_CONTACTS,
			_assetReferenceObjectService.fetchReferenceObjectIds(
				_contactConverter,
				accountUserAccountBucket.getCustomerUserAccounts(),
				UserAccount::getExternalReferenceCode));
		assetObject.setAttributeValue(
			AccountConstants.ATTRIBUTE_NAME_WORKER_CONTACTS,
			_assetReferenceObjectService.fetchReferenceObjectIds(
				_contactConverter,
				accountUserAccountBucket.getWorkerUserAccounts(),
				UserAccount::getExternalReferenceCode));

		assetObject.setAttributeValue(
			AccountConstants.ATTRIBUTE_NAME_ENTITLEMENTS,
			_assetReferenceObjectService.getOrCreateReferenceObjectIds(
				_entitlementConverter, _getEntitlementDefinitions(account),
				EntitlementDefinition::getDisplayName,
				_entitlementConverter::toAssetObject));
		assetObject.setAttributeValue(
			AccountConstants.ATTRIBUTE_NAME_EXTERNAL_LINKS,
			_assetReferenceObjectService.getOrCreateReferenceObjectIds(
				_externalLinkConverter, _getExternalLinkProperties(account),
				Property::getExternalReferenceCode,
				_externalLinkConverter::toAssetObject));
		assetObject.setAttributeValue(
			AccountConstants.ATTRIBUTE_NAME_POSTAL_ADDRESSES,
			_assetReferenceObjectService.getOrCreateReferenceObjectIds(
				_postalAddressConverter,
				ListUtil.fromArray(account.getPostalAddresses()),
				postalAddress -> String.valueOf(postalAddress.getId()),
				_postalAddressConverter::toAssetObject));

		_assetObjectUpsertService.upsert(_accountConverter, assetObject);
	}

	private String _toBusinessEventFieldValuePart(BusinessEvent businessEvent) {
		List<String> lines = new ArrayList<>();

		_addBusinessEventLine(lines, "name", businessEvent.getName());
		_addBusinessEventLine(
			lines, "targetGoLiveDateTime", businessEvent.getPlannedEventDate());
		_addBusinessEventLine(
			lines, "description", businessEvent.getDescription());
		_addBusinessEventLine(lines, "type", businessEvent.getEventTypeName());
		_addBusinessEventLine(
			lines, "currentVersion",
			businessEvent.getCurrentLiferayVersionName());
		_addBusinessEventLine(
			lines, "newVersion", businessEvent.getNewLiferayVersionName());

		return StringUtil.merge(lines, ",\n");
	}

	private String _toBusinessEventsFieldValue(Account account) {
		try {
			List<String> parts = new ArrayList<>();

			List<BusinessEvent> businessEvents =
				_businessEventService.getBusinessEvents(
					account.getExternalReferenceCode());

			for (BusinessEvent businessEvent : businessEvents) {
				String part = _toBusinessEventFieldValuePart(businessEvent);

				if (Validator.isNotNull(part)) {
					parts.add(part);
				}
			}

			return StringUtil.merge(parts, "\n\n");
		}
		catch (Exception exception) {
			_log.error("Unable to get business events", exception);

			return null;
		}
	}

	private static final Log _log = LogFactory.getLog(AccountSyncService.class);

	@Autowired
	private AccountConverter _accountConverter;

	@Autowired
	private AccountService _accountService;

	@Autowired
	private AssetObjectUpsertService _assetObjectUpsertService;

	@Autowired
	private AssetReferenceObjectService _assetReferenceObjectService;

	@Autowired
	private BusinessEventService _businessEventService;

	@Autowired
	private CommerceOrderService _commerceOrderService;

	@Autowired
	private ContactConverter _contactConverter;

	private final List<String> _employeeRoleNames = EmployeeRoles.getNames();

	@Autowired
	private EntitlementConverter _entitlementConverter;

	@Autowired
	private EntitlementDefinitionService _entitlementDefinitionService;

	@Autowired
	private EntitlementService _entitlementService;

	@Autowired
	private ExternalLinkConverter _externalLinkConverter;

	@Autowired
	private OrganizationService _organizationService;

	@Autowired
	private PostalAddressConverter _postalAddressConverter;

	@Autowired
	private ProjectService _projectService;

	@Autowired
	private PropertyService _propertyService;

	@Autowired
	private TeamConverter _teamConverter;

	@Autowired
	private UserAccountService _userAccountService;

	private static class AccountUserAccountBucket {

		public void addCustomerUserAccount(UserAccount userAccount) {
			_customerUserAccounts.add(userAccount);
		}

		public void addWorkerUserAccount(UserAccount userAccount) {
			_workerUserAccounts.add(userAccount);
		}

		public List<UserAccount> getCustomerUserAccounts() {
			return _customerUserAccounts;
		}

		public List<UserAccount> getWorkerUserAccounts() {
			return _workerUserAccounts;
		}

		private final List<UserAccount> _customerUserAccounts =
			new ArrayList<>();
		private final List<UserAccount> _workerUserAccounts = new ArrayList<>();

	}

	private enum EmployeeRoles {

		CUSTOMER_EXPERIENCE_MANAGER(
			"C_CUSTOMER_EXPERIENCE_MANAGER", "Customer Experience Manager",
			"account"),
		LIFERAY_SALES("C_LIFERAY_SALES", "Liferay Sales", "account"),
		PRIMARY_CONTACT("C_PRIMARY_CONTACT", "Primary Contact", "account"),
		SECONDARY_CONTACT(
			"C_SECONDARY_CONTACT", "Secondary Contact", "account"),
		SOLUTION_ARCHITECT(
			"C_SOLUTION_ARCHITECT", "Solution Architect", "account");

		public static List<String> getNames() {
			List<String> names = new ArrayList<>();

			for (EmployeeRoles employeeRole : values()) {
				names.add(employeeRole.getName());
			}

			return names;
		}

		@SuppressWarnings("unused")
		public String getExternalReferenceCode() {
			return _externalReferenceCode;
		}

		public String getName() {
			return _name;
		}

		@SuppressWarnings("unused")
		public String getRoleType() {
			return _roleType;
		}

		private EmployeeRoles(
			String externalReferenceCode, String name, String roleType) {

			_externalReferenceCode = externalReferenceCode;
			_name = name;
			_roleType = roleType;
		}

		private final String _externalReferenceCode;
		private final String _name;
		private final String _roleType;

	}

}