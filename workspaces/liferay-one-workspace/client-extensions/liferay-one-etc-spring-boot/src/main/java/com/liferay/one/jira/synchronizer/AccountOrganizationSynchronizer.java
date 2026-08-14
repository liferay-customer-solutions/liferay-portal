/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.jira.synchronizer;

import com.liferay.one.jira.constants.AccountTeamRoleAssignmentConstants;
import com.liferay.one.jira.constants.TeamRoleConstants;
import com.liferay.one.jira.converter.AccountConverter;
import com.liferay.one.jira.converter.AccountTeamRoleAssignmentConverter;
import com.liferay.one.jira.converter.TeamConverter;
import com.liferay.one.jira.model.JiraAssetObject;
import com.liferay.one.jira.service.JiraAssetService;
import com.liferay.one.util.FindUtil;
import com.liferay.one.util.KeyedLock;
import com.liferay.petra.string.StringBundler;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Drew Brokke
 */
@Component
public class AccountOrganizationSynchronizer {

	public void softDeleteByAccount(String accountExternalKey) {
		_jiraAssetService.softDeleteByAttribute(
			_accountTeamRoleAssignmentConverter,
			AccountTeamRoleAssignmentConstants.
				ATTRIBUTE_NAME_ACCOUNT_EXTERNAL_KEY,
			accountExternalKey);
	}

	public void softDeleteByOrganization(String organizationExternalKey) {
		_jiraAssetService.softDeleteByAttribute(
			_accountTeamRoleAssignmentConverter,
			AccountTeamRoleAssignmentConstants.ATTRIBUTE_NAME_TEAM_EXTERNAL_KEY,
			organizationExternalKey);
	}

	public void syncAssignOrganization(
			String organizationExternalKey, String accountExternalKey)
		throws Exception {

		_keyedLock.withLock(
			accountExternalKey,
			() -> _syncAssignment(
				organizationExternalKey, accountExternalKey, false, null));
	}

	public void syncUnassignOrganization(
			String organizationExternalKey, String accountExternalKey)
		throws Exception {

		_keyedLock.withLock(
			accountExternalKey,
			() -> _syncAssignment(
				organizationExternalKey, accountExternalKey, true, null));
	}

	public void syncUnassignStaleOrganizations(
		String accountExternalKey, Set<String> organizationExternalKeys,
		Date startDate) {

		List<JiraAssetObject> jiraAssetObjects =
			_jiraAssetService.getJiraAssetObjects(
				_accountTeamRoleAssignmentConverter,
				aqlBuilder -> aqlBuilder.andEquals(
					accountExternalKey,
					AccountTeamRoleAssignmentConstants.
						ATTRIBUTE_NAME_ACCOUNT_EXTERNAL_KEY
				).andEquals(
					false,
					AccountTeamRoleAssignmentConstants.ATTRIBUTE_NAME_DELETED
				));

		for (JiraAssetObject jiraAssetObject : jiraAssetObjects) {
			String organizationExternalKey = jiraAssetObject.getAttributeValue(
				AccountTeamRoleAssignmentConstants.
					ATTRIBUTE_NAME_TEAM_EXTERNAL_KEY);

			if (FindUtil.containsIgnoreCase(
					organizationExternalKeys, organizationExternalKey)) {

				continue;
			}

			try {
				_keyedLock.withLock(
					accountExternalKey,
					() -> _syncAssignment(
						organizationExternalKey, accountExternalKey, true,
						(existingJiraAssetObject, newJiraAssetObject) ->
							_jiraAssetService.isUpdatedSince(
								_accountTeamRoleAssignmentConverter, startDate,
								existingJiraAssetObject)));
			}
			catch (Exception exception) {
				_log.error(
					StringBundler.concat(
						"Unable to unassign stale organization ",
						organizationExternalKey, " from account ",
						accountExternalKey),
					exception);
			}
		}
	}

	private void _syncAssignment(
		String organizationExternalKey, String accountExternalKey,
		boolean deleted,
		BiPredicate<JiraAssetObject, JiraAssetObject>
			shouldSkipUpdateBiPredicate) {

		if (_log.isInfoEnabled()) {
			_log.info(
				StringBundler.concat(
					deleted ? "Unassigning" : "Assigning", " account ",
					accountExternalKey, deleted ? " from" : " to",
					" organization ", organizationExternalKey));
		}

		JiraAssetObject jiraAssetObject =
			_accountTeamRoleAssignmentConverter.toAssetObject(
				TeamRoleConstants.EXTERNAL_KEY_FIRST_LINE_SUPPORT,
				organizationExternalKey, accountExternalKey, deleted,
				new Date());

		jiraAssetObject.setAttributeValue(
			AccountTeamRoleAssignmentConstants.ATTRIBUTE_NAME_TEAM_ROLE,
			_teamRoleSynchronizer.getFirstLineSupportTeamRoleObjectId());

		jiraAssetObject.setAttributeValue(
			AccountTeamRoleAssignmentConstants.ATTRIBUTE_NAME_TEAM,
			_jiraAssetService.getReferenceObjectId(
				_teamConverter, organizationExternalKey));
		jiraAssetObject.setAttributeValue(
			AccountTeamRoleAssignmentConstants.ATTRIBUTE_NAME_ACCOUNT,
			_jiraAssetService.getReferenceObjectId(
				_accountConverter, accountExternalKey));

		_jiraAssetService.upsert(
			_accountTeamRoleAssignmentConverter, jiraAssetObject,
			shouldSkipUpdateBiPredicate);
	}

	private static final Log _log = LogFactory.getLog(
		AccountOrganizationSynchronizer.class);

	@Autowired
	private AccountConverter _accountConverter;

	@Autowired
	private AccountTeamRoleAssignmentConverter
		_accountTeamRoleAssignmentConverter;

	@Autowired
	private JiraAssetService _jiraAssetService;

	@Autowired
	private KeyedLock _keyedLock;

	@Autowired
	private TeamConverter _teamConverter;

	@Autowired
	private TeamRoleSynchronizer _teamRoleSynchronizer;

}