/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.jira.synchronizer;

import com.liferay.headless.admin.user.client.dto.v1_0.AccountBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.Organization;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.one.jira.constants.TeamConstants;
import com.liferay.one.jira.converter.AccountConverter;
import com.liferay.one.jira.converter.ContactConverter;
import com.liferay.one.jira.converter.ExternalLinkConverter;
import com.liferay.one.jira.converter.TeamConverter;
import com.liferay.one.jira.model.JiraAssetObject;
import com.liferay.one.jira.service.JiraAssetService;
import com.liferay.one.model.Property;
import com.liferay.one.service.PropertyService;
import com.liferay.one.service.UserAccountService;
import com.liferay.one.util.KeyedLock;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.GetterUtil;
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
public class OrganizationSynchronizer {

	public void deleteOrganization(String externalReferenceCode) {
		if (_log.isInfoEnabled()) {
			_log.info(
				"Deleting organization " + externalReferenceCode + " from JSM");
		}

		_keyedLock.withLock(
			externalReferenceCode,
			() -> {
				try {
					_accountOrganizationSynchronizer.softDeleteByOrganization(
						externalReferenceCode);
				}
				catch (Exception exception) {
					_log.error(
						StringBundler.concat(
							"Unable to soft delete account team role ",
							"assignments for organization ",
							externalReferenceCode),
						exception);
				}

				try {
					_organizationUserAccountRoleSynchronizer.
						softDeleteByOrganization(externalReferenceCode);
				}
				catch (Exception exception) {
					_log.error(
						StringBundler.concat(
							"Unable to soft delete team contact role ",
							"assignments for organization ",
							externalReferenceCode),
						exception);
				}

				_jiraAssetService.delete(_teamConverter, externalReferenceCode);
			});
	}

	public void syncOrganization(Organization organization) throws Exception {
		if (_log.isInfoEnabled()) {
			_log.info(
				"Syncing organization " +
					organization.getExternalReferenceCode() + " to JSM");
		}

		List<AccountBrief> accountBriefs = ListUtil.fromArray(
			organization.getAccountBriefs());

		JiraAssetObject jiraAssetObject = _teamConverter.toAssetObject(
			organization);

		if (!accountBriefs.isEmpty()) {
			AccountBrief accountBrief = accountBriefs.get(0);

			jiraAssetObject.setAttributeValue(
				TeamConstants.ATTRIBUTE_NAME_ACCOUNT,
				_jiraAssetService.getReferenceObjectId(
					_accountConverter,
					accountBrief.getExternalReferenceCode()));
		}

		List<String> teamRoleObjectIds = Collections.emptyList();

		if (!accountBriefs.isEmpty()) {
			teamRoleObjectIds = Collections.singletonList(
				_teamRoleSynchronizer.getFirstLineSupportTeamRoleObjectId());
		}

		jiraAssetObject.setAttributeValue(
			TeamConstants.ATTRIBUTE_NAME_TEAM_ROLES, teamRoleObjectIds);
		jiraAssetObject.setAttributeValue(
			TeamConstants.ATTRIBUTE_NAME_EXTERNAL_LINKS,
			_jiraAssetService.getOrCreateReferenceObjectIds(
				_externalLinkConverter,
				_getExternalLinkProperties(organization),
				Property::getExternalReferenceCode,
				_externalLinkConverter::toAssetObject));
		jiraAssetObject.setAttributeValue(
			TeamConstants.ATTRIBUTE_NAME_CONTACTS,
			_jiraAssetService.fetchReferenceObjectIds(
				_contactConverter,
				_userAccountService.getOrganizationUserAccounts(
					GetterUtil.getLong(organization.getId())),
				UserAccount::getExternalReferenceCode));

		_keyedLock.withLock(
			organization.getExternalReferenceCode(),
			() -> _jiraAssetService.upsert(_teamConverter, jiraAssetObject));

		_syncOrganizationAssignments(organization, accountBriefs);
	}

	public void syncOrganizationUserAccounts(Organization organization)
		throws Exception {

		if (_log.isInfoEnabled()) {
			_log.info(
				"Syncing user accounts for organization " +
					organization.getExternalReferenceCode() + " to JSM");
		}

		JiraAssetObject jiraAssetObject = _teamConverter.toAssetObject(
			organization);

		jiraAssetObject.setAttributeValue(
			TeamConstants.ATTRIBUTE_NAME_CONTACTS,
			_jiraAssetService.fetchReferenceObjectIds(
				_contactConverter,
				_userAccountService.getOrganizationUserAccounts(
					GetterUtil.getLong(organization.getId())),
				UserAccount::getExternalReferenceCode));

		_keyedLock.withLock(
			organization.getExternalReferenceCode(),
			() -> _jiraAssetService.upsert(_teamConverter, jiraAssetObject));
	}

	private List<Property> _getExternalLinkProperties(Organization organization)
		throws Exception {

		List<Property> externalLinkProperties = new ArrayList<>();

		List<Property> properties = _propertyService.getOrganizationProperties(
			GetterUtil.getLong(organization.getId()));

		for (Property property : properties) {
			if (_externalLinkConverter.isExternalLinkProperty(property)) {
				externalLinkProperties.add(property);
			}
		}

		return externalLinkProperties;
	}

	private void _syncOrganizationAssignments(
		Organization organization, List<AccountBrief> accountBriefs) {

		for (AccountBrief accountBrief : accountBriefs) {
			try {
				_accountOrganizationSynchronizer.syncAssignOrganization(
					organization.getExternalReferenceCode(),
					accountBrief.getExternalReferenceCode());
			}
			catch (Exception exception) {
				_log.error(
					StringBundler.concat(
						"Unable to sync account organization assignment for ",
						"account ", accountBrief.getExternalReferenceCode()),
					exception);
			}
		}
	}

	private static final Log _log = LogFactory.getLog(
		OrganizationSynchronizer.class);

	@Autowired
	private AccountConverter _accountConverter;

	@Autowired
	private AccountOrganizationSynchronizer _accountOrganizationSynchronizer;

	@Autowired
	private ContactConverter _contactConverter;

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
	private PropertyService _propertyService;

	@Autowired
	private TeamConverter _teamConverter;

	@Autowired
	private TeamRoleSynchronizer _teamRoleSynchronizer;

	@Autowired
	private UserAccountService _userAccountService;

}