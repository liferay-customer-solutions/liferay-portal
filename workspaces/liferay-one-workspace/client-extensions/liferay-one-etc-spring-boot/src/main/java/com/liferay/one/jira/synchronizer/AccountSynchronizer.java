/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.jira.synchronizer;

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
import com.liferay.one.jira.exception.AccountNotFoundException;
import com.liferay.one.jira.model.JiraAssetObject;
import com.liferay.one.jira.service.JiraAssetService;
import com.liferay.one.jira.service.JiraBusinessEventService;
import com.liferay.one.model.EntitlementDefinition;
import com.liferay.one.model.Project;
import com.liferay.one.model.ProjectMembership;
import com.liferay.one.model.Property;
import com.liferay.one.service.AccountService;
import com.liferay.one.service.CommerceOrderService;
import com.liferay.one.service.EntitlementService;
import com.liferay.one.service.OrganizationService;
import com.liferay.one.service.ProjectMembershipService;
import com.liferay.one.service.ProjectService;
import com.liferay.one.service.PropertyService;
import com.liferay.one.service.RoleService;
import com.liferay.one.service.UserAccountService;
import com.liferay.one.util.FindUtil;
import com.liferay.one.util.KeyedLock;
import com.liferay.petra.function.UnsafeConsumer;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Drew Brokke
 */
@Component
public class AccountSynchronizer {

	public void deleteAccount(String externalReferenceCode) {
		_keyedLock.withLock(
			externalReferenceCode,
			() -> {
				if (_log.isInfoEnabled()) {
					_log.info(
						"Deleting account " + externalReferenceCode +
							" from JSM");
				}

				try {
					_accountOrganizationSynchronizer.softDeleteByAccount(
						externalReferenceCode);
				}
				catch (Exception exception) {
					_log.error(
						StringBundler.concat(
							"Unable to soft delete account team role ",
							"assignments for account ", externalReferenceCode),
						exception);
				}

				try {
					_accountUserAccountRoleSynchronizer.softDeleteByAccount(
						externalReferenceCode);
				}
				catch (Exception exception) {
					_log.error(
						StringBundler.concat(
							"Unable to soft delete account contact role ",
							"assignments for account ", externalReferenceCode),
						exception);
				}

				_jiraAssetService.delete(
					_accountConverter, externalReferenceCode);
			});
	}

	public void syncAccount(Account account) throws Exception {
		if (_log.isInfoEnabled()) {
			_log.info(
				"Syncing account " + account.getExternalReferenceCode() +
					" to JSM");
		}

		Date startDate = new Date();

		AccountSyncModel accountSyncModel = _createAccountSyncModel(account);

		_syncAccountAsset(
			account, account.getExternalReferenceCode(), account.getName(),
			jiraAssetObject -> _setAttributeValues(
				accountSyncModel, jiraAssetObject));

		_syncContactRoleAssignments(accountSyncModel, startDate);
		_syncAccountOrganizationAssignments(accountSyncModel, startDate);

		List<UserAccount> userAccountsToSync = new ArrayList<>(
			accountSyncModel.getAccountUserAccounts());

		for (Project project : accountSyncModel.getProjects()) {
			try {
				ProjectSyncModel projectSyncModel = _createProjectSyncModel(
					accountSyncModel, project);

				_syncProject(projectSyncModel);

				userAccountsToSync.addAll(
					projectSyncModel.getCustomerUserAccounts());
				userAccountsToSync.addAll(
					projectSyncModel.getWorkerUserAccounts());
			}
			catch (Exception exception) {
				_log.error(
					"Unable to sync project " +
						project.getExternalReferenceCode(),
					exception);
			}
		}

		_syncUserAccounts(userAccountsToSync);
	}

	public void syncAccountUserAccounts(Account account) throws Exception {
		if (_log.isInfoEnabled()) {
			_log.info(
				"Syncing user accounts for account " +
					account.getExternalReferenceCode() + " to JSM");
		}

		AccountSyncModel accountSyncModel = _createAccountSyncModel(account);

		_syncAccountAsset(
			account, account.getExternalReferenceCode(), account.getName(),
			jiraAssetObject -> {
				jiraAssetObject.setAttributeValue(
					AccountConstants.ATTRIBUTE_NAME_CUSTOMER_CONTACTS,
					_toContactObjectIds(
						accountSyncModel.getCustomerUserAccounts()));
				jiraAssetObject.setAttributeValue(
					AccountConstants.ATTRIBUTE_NAME_WORKER_CONTACTS,
					_toContactObjectIds(
						accountSyncModel.getWorkerUserAccounts()));
			});
	}

	public void syncProject(Project project) throws Exception {
		if (_log.isInfoEnabled()) {
			_log.info(
				"Syncing project " + project.getExternalReferenceCode() +
					" to JSM");
		}

		String accountExternalReferenceCode =
			project.getAccountExternalReferenceCode();

		if (Validator.isNull(accountExternalReferenceCode)) {
			throw new AccountNotFoundException(
				"Project " + project.getExternalReferenceCode() +
					" has no account external reference code");
		}

		AccountSyncModel accountSyncModel = _createAccountSyncModel(
			_accountService.getAccount(accountExternalReferenceCode));

		ProjectSyncModel projectSyncModel = _createProjectSyncModel(
			accountSyncModel, project);

		_syncProject(projectSyncModel);

		List<UserAccount> userAccounts = new ArrayList<>(
			projectSyncModel.getCustomerUserAccounts());

		userAccounts.addAll(projectSyncModel.getWorkerUserAccounts());

		_syncUserAccounts(userAccounts);
	}

	private AccountSyncModel _createAccountSyncModel(Account account) {
		return new AccountSyncModel(
			account, _commerceOrderService, _entitlementService,
			_externalLinkConverter, _jiraBusinessEventService,
			_organizationService, _projectService, _propertyService,
			_roleService, _userAccountService);
	}

	private ProjectSyncModel _createProjectSyncModel(
		AccountSyncModel accountSyncModel, Project project) {

		return new ProjectSyncModel(
			accountSyncModel, _entitlementService, project,
			_projectMembershipService, _userAccountService);
	}

	private void _setAttributeValues(
			AccountSyncModel accountSyncModel, JiraAssetObject jiraAssetObject)
		throws Exception {

		jiraAssetObject.setAttributeValue(
			AccountConstants.ATTRIBUTE_NAME_ASSIGNED_TEAMS,
			_jiraAssetService.getOrCreateReferenceObjectIds(
				_teamConverter, accountSyncModel.getAccountOrganizations(),
				Organization::getExternalReferenceCode,
				_teamConverter::toAssetObject));
		jiraAssetObject.setAttributeValue(
			AccountConstants.ATTRIBUTE_NAME_BUSINESS_EVENTS,
			accountSyncModel.getBusinessEventsFieldValue());
		jiraAssetObject.setAttributeValue(
			AccountConstants.ATTRIBUTE_NAME_CUSTOMER_CONTACTS,
			_toContactObjectIds(accountSyncModel.getCustomerUserAccounts()));
		jiraAssetObject.setAttributeValue(
			AccountConstants.ATTRIBUTE_NAME_ENTITLEMENTS,
			_toEntitlementObjectIds(
				accountSyncModel.getActiveEntitlementDefinitions()));
		jiraAssetObject.setAttributeValue(
			AccountConstants.ATTRIBUTE_NAME_EXTERNAL_LINKS,
			_jiraAssetService.getOrCreateReferenceObjectIds(
				_externalLinkConverter,
				accountSyncModel.getExternalLinkProperties(),
				Property::getExternalReferenceCode,
				_externalLinkConverter::toAssetObject));
		jiraAssetObject.setAttributeValue(
			AccountConstants.ATTRIBUTE_NAME_LANGUAGE,
			accountSyncModel.getSupportLanguage());
		jiraAssetObject.setAttributeValue(
			AccountConstants.ATTRIBUTE_NAME_POSTAL_ADDRESSES,
			_jiraAssetService.getOrCreateReferenceObjectIds(
				_postalAddressConverter, accountSyncModel.getPostalAddresses(),
				postalAddress -> String.valueOf(postalAddress.getId()),
				_postalAddressConverter::toAssetObject));
		jiraAssetObject.setAttributeValue(
			AccountConstants.ATTRIBUTE_NAME_SUPPORT_REGION,
			accountSyncModel.getSupportRegion());
		jiraAssetObject.setAttributeValue(
			AccountConstants.ATTRIBUTE_NAME_WORKER_CONTACTS,
			_toContactObjectIds(accountSyncModel.getWorkerUserAccounts()));
	}

	private void _setAttributeValues(
			JiraAssetObject jiraAssetObject, ProjectSyncModel projectSyncModel)
		throws Exception {

		_setAttributeValues(
			projectSyncModel.getAccountSyncModel(), jiraAssetObject);

		jiraAssetObject.setAttributeValue(
			AccountConstants.ATTRIBUTE_NAME_CUSTOMER_CONTACTS,
			_toContactObjectIds(projectSyncModel.getCustomerUserAccounts()));
		jiraAssetObject.setAttributeValue(
			AccountConstants.ATTRIBUTE_NAME_ENTITLEMENTS,
			_toEntitlementObjectIds(
				projectSyncModel.getActiveEntitlementDefinitions()));
		jiraAssetObject.setAttributeValue(
			AccountConstants.ATTRIBUTE_NAME_WORKER_CONTACTS,
			_toContactObjectIds(projectSyncModel.getWorkerUserAccounts()));
	}

	private void _syncAccountAsset(
			Account account, String externalKey, String name,
			UnsafeConsumer<JiraAssetObject, Exception> unsafeConsumer)
		throws Exception {

		JiraAssetObject assetObject = _accountConverter.toAssetObject(
			account, externalKey, name);

		unsafeConsumer.accept(assetObject);

		_keyedLock.withLock(
			externalKey,
			() -> _jiraAssetService.upsert(_accountConverter, assetObject));
	}

	private void _syncAccountOrganizationAssignments(
			AccountSyncModel accountSyncModel, Date startDate)
		throws Exception {

		Set<String> organizationExternalKeys = new LinkedHashSet<>();

		for (Organization organization :
				accountSyncModel.getAccountOrganizations()) {

			organizationExternalKeys.add(
				organization.getExternalReferenceCode());

			try {
				_accountOrganizationSynchronizer.syncAssignOrganization(
					organization.getExternalReferenceCode(),
					accountSyncModel.getExternalReferenceCode());
			}
			catch (Exception exception) {
				_log.error(
					StringBundler.concat(
						"Unable to sync account organization assignment for ",
						"organization ",
						organization.getExternalReferenceCode()),
					exception);
			}
		}

		try {
			_accountOrganizationSynchronizer.syncUnassignStaleOrganizations(
				accountSyncModel.getExternalReferenceCode(),
				organizationExternalKeys, startDate);
		}
		catch (Exception exception) {
			_log.error(
				"Unable to unassign stale organizations from account " +
					accountSyncModel.getExternalReferenceCode(),
				exception);
		}
	}

	private void _syncContactRoleAssignments(
			AccountSyncModel accountSyncModel, Date startDate)
		throws Exception {

		Map<String, Set<String>> roleExternalKeysByUserAccountExternalKey =
			new LinkedHashMap<>();

		for (UserAccount accountUserAccount :
				accountSyncModel.getAccountUserAccounts()) {

			AccountBrief accountBrief = FindUtil.findFirst(
				accountUserAccount.getAccountBriefs(),
				accountBrief1 -> Objects.equals(
					accountSyncModel.getExternalReferenceCode(),
					accountBrief1.getExternalReferenceCode()));

			if (accountBrief == null) {
				continue;
			}

			RoleBrief[] roleBriefs = accountBrief.getRoleBriefs();

			if (roleBriefs == null) {
				continue;
			}

			Set<String> roleExternalKeys = new LinkedHashSet<>();

			roleExternalKeysByUserAccountExternalKey.put(
				accountUserAccount.getExternalReferenceCode(),
				roleExternalKeys);

			for (RoleBrief roleBrief : roleBriefs) {
				roleExternalKeys.add(roleBrief.getExternalReferenceCode());

				try {
					_accountUserAccountRoleSynchronizer.syncAssignRole(
						roleBrief.getExternalReferenceCode(),
						accountUserAccount.getExternalReferenceCode(),
						accountSyncModel.getExternalReferenceCode());
				}
				catch (Exception exception) {
					_log.error(
						StringBundler.concat(
							"Unable to sync account contact role assignment ",
							"for role ", roleBrief.getExternalReferenceCode()),
						exception);
				}
			}
		}

		try {
			_accountUserAccountRoleSynchronizer.syncUnassignStaleRoles(
				accountSyncModel.getExternalReferenceCode(),
				roleExternalKeysByUserAccountExternalKey, startDate);
		}
		catch (Exception exception) {
			_log.error(
				"Unable to unassign stale contact roles for account " +
					accountSyncModel.getExternalReferenceCode(),
				exception);
		}
	}

	private void _syncProject(ProjectSyncModel projectSyncModel)
		throws Exception {

		AccountSyncModel accountSyncModel =
			projectSyncModel.getAccountSyncModel();
		Project project = projectSyncModel.getProject();

		_syncAccountAsset(
			accountSyncModel.getAccount(), project.getExternalReferenceCode(),
			project.getName(),
			jiraAssetObject -> _setAttributeValues(
				jiraAssetObject, projectSyncModel));

		_syncProjectMemberships(projectSyncModel);
	}

	private void _syncProjectMemberships(ProjectSyncModel projectSyncModel)
		throws Exception {

		Project project = projectSyncModel.getProject();

		for (ProjectMembership projectMembership :
				projectSyncModel.getProjectMemberships()) {

			try {
				UserAccount userAccount = _userAccountService.getUserAccount(
					projectMembership.getUserId());

				_accountUserAccountRoleSynchronizer.syncAssignRole(
					projectMembership.getRoleExternalReferenceCode(),
					userAccount.getExternalReferenceCode(),
					project.getExternalReferenceCode());
			}
			catch (Exception exception) {
				_log.error(
					"Unable to sync project membership " +
						projectMembership.getExternalReferenceCode(),
					exception);
			}
		}
	}

	private void _syncUserAccounts(Collection<UserAccount> userAccounts) {
		List<String> seenUserAccountExternalKeys = new ArrayList<>();

		int failureCount = 0;

		for (UserAccount userAccount : userAccounts) {
			if (seenUserAccountExternalKeys.contains(
					userAccount.getExternalReferenceCode())) {

				continue;
			}

			if (_log.isInfoEnabled()) {
				_log.info(
					"Syncing user account " +
						userAccount.getExternalReferenceCode() + " to JSM");
			}

			seenUserAccountExternalKeys.add(
				userAccount.getExternalReferenceCode());

			try {
				_userAccountSynchronizer.syncUserAccount(userAccount);
			}
			catch (Exception exception) {
				failureCount++;

				_log.error(
					"Unable to sync user account " +
						userAccount.getExternalReferenceCode() + " to JSM",
					exception);
			}
		}

		if (failureCount > 0) {
			_log.error(
				StringBundler.concat(
					"Unable to sync ", failureCount, " of ",
					userAccounts.size(), " user accounts to JSM"));
		}
	}

	private List<String> _toContactObjectIds(List<UserAccount> userAccounts) {
		return _jiraAssetService.getOrCreateReferenceObjectIds(
			_contactConverter, userAccounts,
			UserAccount::getExternalReferenceCode,
			_contactConverter::toAssetObject);
	}

	private List<String> _toEntitlementObjectIds(
		List<EntitlementDefinition> entitlementDefinitions) {

		return _jiraAssetService.getOrCreateReferenceObjectIds(
			_entitlementConverter, entitlementDefinitions,
			EntitlementDefinition::getDisplayName,
			_entitlementConverter::toAssetObject);
	}

	private static final Log _log = LogFactory.getLog(
		AccountSynchronizer.class);

	@Autowired
	private AccountConverter _accountConverter;

	@Autowired
	private AccountOrganizationSynchronizer _accountOrganizationSynchronizer;

	@Autowired
	private AccountService _accountService;

	@Autowired
	private AccountUserAccountRoleSynchronizer
		_accountUserAccountRoleSynchronizer;

	@Autowired
	private CommerceOrderService _commerceOrderService;

	@Autowired
	private ContactConverter _contactConverter;

	@Autowired
	private EntitlementConverter _entitlementConverter;

	@Autowired
	private EntitlementService _entitlementService;

	@Autowired
	private ExternalLinkConverter _externalLinkConverter;

	@Autowired
	private JiraAssetService _jiraAssetService;

	@Autowired
	private JiraBusinessEventService _jiraBusinessEventService;

	@Autowired
	private KeyedLock _keyedLock;

	@Autowired
	private OrganizationService _organizationService;

	@Autowired
	private PostalAddressConverter _postalAddressConverter;

	@Autowired
	private ProjectMembershipService _projectMembershipService;

	@Autowired
	private ProjectService _projectService;

	@Autowired
	private PropertyService _propertyService;

	@Autowired
	private RoleService _roleService;

	@Autowired
	private TeamConverter _teamConverter;

	@Autowired
	private UserAccountService _userAccountService;

	@Autowired
	private UserAccountSynchronizer _userAccountSynchronizer;

}