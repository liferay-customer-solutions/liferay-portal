/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.jira.synchronizer;

import com.liferay.headless.admin.user.client.dto.v1_0.AccountBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.OrganizationBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.Phone;
import com.liferay.headless.admin.user.client.dto.v1_0.RoleBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccountContactInformation;
import com.liferay.one.jira.constants.ContactConstants;
import com.liferay.one.jira.converter.AccountConverter;
import com.liferay.one.jira.converter.ContactConverter;
import com.liferay.one.jira.converter.ContactRoleConverter;
import com.liferay.one.jira.converter.EntitlementConverter;
import com.liferay.one.jira.converter.ExternalLinkConverter;
import com.liferay.one.jira.converter.PhoneConverter;
import com.liferay.one.jira.converter.TeamConverter;
import com.liferay.one.jira.model.JiraAssetObject;
import com.liferay.one.jira.service.JiraAssetService;
import com.liferay.one.model.EntitlementDefinition;
import com.liferay.one.model.Property;
import com.liferay.one.service.EntitlementService;
import com.liferay.one.service.PropertyService;
import com.liferay.one.util.KeyedLock;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.ListUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Drew Brokke
 */
@Component
public class UserAccountSynchronizer {

	public void deleteUserAccount(String externalReferenceCode) {
		_keyedLock.withLock(
			externalReferenceCode,
			() -> {
				if (_log.isInfoEnabled()) {
					_log.info(
						"Deleting user account " + externalReferenceCode +
							" from JSM");
				}

				try {
					_accountUserAccountRoleSynchronizer.softDeleteByUserAccount(
						externalReferenceCode);
				}
				catch (Exception exception) {
					_log.error(
						StringBundler.concat(
							"Unable to soft delete account contact role ",
							"assignments for user account ",
							externalReferenceCode),
						exception);
				}

				try {
					_organizationUserAccountRoleSynchronizer.
						softDeleteByUserAccount(externalReferenceCode);
				}
				catch (Exception exception) {
					_log.error(
						StringBundler.concat(
							"Unable to soft delete team contact role ",
							"assignments for user account ",
							externalReferenceCode),
						exception);
				}

				_jiraAssetService.delete(
					_contactConverter, externalReferenceCode);
			});
	}

	public void syncUserAccount(UserAccount userAccount) throws Exception {
		_keyedLock.withLock(
			userAccount.getExternalReferenceCode(),
			() -> _syncUserAccount(userAccount));
	}

	public void syncUserAccountAccounts(UserAccount userAccount) {
		if (_log.isInfoEnabled()) {
			_log.info(
				"Syncing accounts for user account " +
					userAccount.getExternalReferenceCode() + " to JSM");
		}

		JiraAssetObject jiraAssetObject = _contactConverter.toAssetObject(
			userAccount);

		jiraAssetObject.setAttributeValue(
			ContactConstants.ATTRIBUTE_NAME_ACCOUNT,
			_jiraAssetService.fetchReferenceObjectIds(
				_accountConverter,
				ListUtil.fromArray(userAccount.getAccountBriefs()),
				AccountBrief::getExternalReferenceCode));

		_keyedLock.withLock(
			userAccount.getExternalReferenceCode(),
			() -> _jiraAssetService.upsert(_contactConverter, jiraAssetObject));
	}

	public void syncUserAccountOrganizations(UserAccount userAccount) {
		if (_log.isInfoEnabled()) {
			_log.info(
				"Syncing organizations for user account " +
					userAccount.getExternalReferenceCode() + " to JSM");
		}

		JiraAssetObject jiraAssetObject = _contactConverter.toAssetObject(
			userAccount);

		jiraAssetObject.setAttributeValue(
			ContactConstants.ATTRIBUTE_NAME_TEAMS,
			_jiraAssetService.fetchReferenceObjectIds(
				_teamConverter,
				ListUtil.fromArray(userAccount.getOrganizationBriefs()),
				OrganizationBrief::getExternalReferenceCode));

		_keyedLock.withLock(
			userAccount.getExternalReferenceCode(),
			() -> _jiraAssetService.upsert(_contactConverter, jiraAssetObject));
	}

	public void syncUserAccountRoles(UserAccount userAccount) {
		if (_log.isInfoEnabled()) {
			_log.info(
				"Syncing roles for user account " +
					userAccount.getExternalReferenceCode() + " to JSM");
		}

		JiraAssetObject jiraAssetObject = _contactConverter.toAssetObject(
			userAccount);

		jiraAssetObject.setAttributeValue(
			ContactConstants.ATTRIBUTE_NAME_CONTACT_ROLES,
			_jiraAssetService.fetchReferenceObjectIds(
				_contactRoleConverter, _getRoleBriefs(userAccount),
				RoleBrief::getExternalReferenceCode));

		_keyedLock.withLock(
			userAccount.getExternalReferenceCode(),
			() -> _jiraAssetService.upsert(_contactConverter, jiraAssetObject));
	}

	private List<EntitlementDefinition> _getEntitlementDefinitions(
			List<AccountBrief> accountBriefs)
		throws Exception {

		List<EntitlementDefinition> entitlementDefinitions = new ArrayList<>();

		for (AccountBrief accountBrief : accountBriefs) {
			entitlementDefinitions.addAll(
				_entitlementService.getActiveEntitlementDefinitions(
					accountBrief.getId()));
		}

		return entitlementDefinitions;
	}

	private List<Property> _getExternalLinkProperties(UserAccount userAccount)
		throws Exception {

		List<Property> externalLinkProperties = new ArrayList<>();

		List<Property> properties = _propertyService.getUserAccountProperties(
			userAccount.getId());

		for (Property property : properties) {
			if (_externalLinkConverter.isExternalLinkProperty(property)) {
				externalLinkProperties.add(property);
			}
		}

		return externalLinkProperties;
	}

	private List<RoleBrief> _getRoleBriefs(UserAccount userAccount) {
		List<RoleBrief> roleBriefs = new ArrayList<>();

		for (AccountBrief accountBrief :
				ListUtil.fromArray(userAccount.getAccountBriefs())) {

			RoleBrief[] accountRoleBriefs = accountBrief.getRoleBriefs();

			if (accountRoleBriefs != null) {
				Collections.addAll(roleBriefs, accountRoleBriefs);
			}
		}

		for (OrganizationBrief organizationBrief :
				ListUtil.fromArray(userAccount.getOrganizationBriefs())) {

			RoleBrief[] organizationRoleBriefs =
				organizationBrief.getRoleBriefs();

			if (organizationRoleBriefs != null) {
				Collections.addAll(roleBriefs, organizationRoleBriefs);
			}
		}

		return roleBriefs;
	}

	private List<Phone> _getTelephones(UserAccount userAccount) {
		UserAccountContactInformation userAccountContactInformation =
			userAccount.getUserAccountContactInformation();

		if (userAccountContactInformation == null) {
			return Collections.emptyList();
		}

		return ListUtil.fromArray(
			userAccountContactInformation.getTelephones());
	}

	private void _syncContactRoleAssignments(
		UserAccount userAccount, List<AccountBrief> accountBriefs) {

		for (AccountBrief accountBrief : accountBriefs) {
			RoleBrief[] roleBriefs = accountBrief.getRoleBriefs();

			if (roleBriefs == null) {
				continue;
			}

			for (RoleBrief roleBrief : roleBriefs) {
				try {
					_accountUserAccountRoleSynchronizer.syncAssignRole(
						roleBrief.getExternalReferenceCode(),
						userAccount.getExternalReferenceCode(),
						accountBrief.getExternalReferenceCode());
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
	}

	private void _syncOrganizationRoleAssignments(
		UserAccount userAccount, List<OrganizationBrief> organizationBriefs) {

		for (OrganizationBrief organizationBrief : organizationBriefs) {
			RoleBrief[] roleBriefs = organizationBrief.getRoleBriefs();

			if (roleBriefs == null) {
				continue;
			}

			for (RoleBrief roleBrief : roleBriefs) {
				try {
					_organizationUserAccountRoleSynchronizer.syncAssignRole(
						roleBrief.getExternalReferenceCode(),
						userAccount.getExternalReferenceCode(),
						organizationBrief.getExternalReferenceCode());
				}
				catch (Exception exception) {
					_log.error(
						StringBundler.concat(
							"Unable to sync organization contact role ",
							"assignment for role ",
							roleBrief.getExternalReferenceCode()),
						exception);
				}
			}
		}
	}

	private void _syncUserAccount(UserAccount userAccount) throws Exception {
		if (_log.isInfoEnabled()) {
			_log.info(
				"Syncing user account " +
					userAccount.getExternalReferenceCode() + " to JSM");
		}

		List<AccountBrief> accountBriefs = ListUtil.fromArray(
			userAccount.getAccountBriefs());
		List<OrganizationBrief> organizationBriefs = ListUtil.fromArray(
			userAccount.getOrganizationBriefs());

		JiraAssetObject jiraAssetObject = _contactConverter.toAssetObject(
			userAccount);

		jiraAssetObject.setAttributeValue(
			ContactConstants.ATTRIBUTE_NAME_ACCOUNT,
			_jiraAssetService.fetchReferenceObjectIds(
				_accountConverter, accountBriefs,
				AccountBrief::getExternalReferenceCode));
		jiraAssetObject.setAttributeValue(
			ContactConstants.ATTRIBUTE_NAME_CONTACT_ROLES,
			_jiraAssetService.fetchReferenceObjectIds(
				_contactRoleConverter, _getRoleBriefs(userAccount),
				RoleBrief::getExternalReferenceCode));
		jiraAssetObject.setAttributeValue(
			ContactConstants.ATTRIBUTE_NAME_ENTITLEMENTS,
			_jiraAssetService.getOrCreateReferenceObjectIds(
				_entitlementConverter,
				_getEntitlementDefinitions(accountBriefs),
				EntitlementDefinition::getDisplayName,
				_entitlementConverter::toAssetObject));
		jiraAssetObject.setAttributeValue(
			ContactConstants.ATTRIBUTE_NAME_EXTERNAL_LINKS,
			_jiraAssetService.getOrCreateReferenceObjectIds(
				_externalLinkConverter, _getExternalLinkProperties(userAccount),
				Property::getExternalReferenceCode,
				_externalLinkConverter::toAssetObject));
		jiraAssetObject.setAttributeValue(
			ContactConstants.ATTRIBUTE_NAME_TEAMS,
			_jiraAssetService.fetchReferenceObjectIds(
				_teamConverter, organizationBriefs,
				OrganizationBrief::getExternalReferenceCode));
		jiraAssetObject.setAttributeValue(
			ContactConstants.ATTRIBUTE_NAME_PHONES,
			_jiraAssetService.getOrCreateReferenceObjectIds(
				_phoneConverter, _getTelephones(userAccount),
				Phone::getPhoneNumber, _phoneConverter::toAssetObject));

		_jiraAssetService.upsert(_contactConverter, jiraAssetObject);

		_syncContactRoleAssignments(userAccount, accountBriefs);
		_syncOrganizationRoleAssignments(userAccount, organizationBriefs);
	}

	private static final Log _log = LogFactory.getLog(
		UserAccountSynchronizer.class);

	@Autowired
	private AccountConverter _accountConverter;

	@Autowired
	private AccountUserAccountRoleSynchronizer
		_accountUserAccountRoleSynchronizer;

	@Autowired
	private ContactConverter _contactConverter;

	@Autowired
	private ContactRoleConverter _contactRoleConverter;

	@Autowired
	private EntitlementConverter _entitlementConverter;

	@Autowired
	private EntitlementService _entitlementService;

	@Autowired
	private ExternalLinkConverter _externalLinkConverter;

	@Autowired
	private JiraAssetService _jiraAssetService;

	@Autowired
	private KeyedLock _keyedLock;

	@Autowired
	private OrganizationUserAccountRoleSynchronizer
		_organizationUserAccountRoleSynchronizer;

	@Autowired
	private PhoneConverter _phoneConverter;

	@Autowired
	private PropertyService _propertyService;

	@Autowired
	private TeamConverter _teamConverter;

}