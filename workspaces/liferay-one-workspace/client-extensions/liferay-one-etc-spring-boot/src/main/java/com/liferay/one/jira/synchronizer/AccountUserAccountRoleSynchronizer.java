/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.jira.synchronizer;

import com.liferay.one.jira.constants.AccountContactRoleAssignmentConstants;
import com.liferay.one.jira.converter.AccountContactRoleAssignmentConverter;
import com.liferay.one.jira.converter.AccountConverter;
import com.liferay.one.jira.converter.ContactConverter;
import com.liferay.one.jira.converter.ContactRoleConverter;
import com.liferay.one.jira.model.JiraAssetObject;
import com.liferay.one.jira.service.JiraAssetService;
import com.liferay.one.jira.util.JiraSyncLock;
import com.liferay.one.util.FindUtil;
import com.liferay.petra.string.StringBundler;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
public class AccountUserAccountRoleSynchronizer {

	public void softDeleteByAccount(String accountExternalKey) {
		_jiraAssetService.softDeleteByAttribute(
			_accountContactRoleAssignmentConverter,
			AccountContactRoleAssignmentConstants.
				ATTRIBUTE_NAME_ACCOUNT_EXTERNAL_KEY,
			accountExternalKey);
	}

	public void softDeleteByUserAccount(String userAccountExternalKey) {
		_jiraAssetService.softDeleteByAttribute(
			_accountContactRoleAssignmentConverter,
			AccountContactRoleAssignmentConstants.
				ATTRIBUTE_NAME_CONTACT_EXTERNAL_KEY,
			userAccountExternalKey);
	}

	public void syncAssignRole(
			String roleExternalKey, String userAccountExternalKey,
			String accountExternalKey)
		throws Exception {

		_syncAssignment(
			roleExternalKey, userAccountExternalKey, accountExternalKey, false,
			null);
	}

	public void syncUnassignRole(
			String roleExternalKey, String userAccountExternalKey,
			String accountExternalKey)
		throws Exception {

		_syncAssignment(
			roleExternalKey, userAccountExternalKey, accountExternalKey, true,
			null);
	}

	public void syncUnassignStaleRoles(
		String accountExternalKey,
		Map<String, Set<String>> roleExternalKeysByUserAccountExternalKey,
		Date startDate) {

		Set<String> names = new LinkedHashSet<>();

		for (Map.Entry<String, Set<String>> entry :
				roleExternalKeysByUserAccountExternalKey.entrySet()) {

			for (String roleExternalKey : entry.getValue()) {
				names.add(
					_accountContactRoleAssignmentConverter.getName(
						accountExternalKey, entry.getKey(), roleExternalKey));
			}
		}

		List<JiraAssetObject> jiraAssetObjects =
			_jiraAssetService.getJiraAssetObjects(
				_accountContactRoleAssignmentConverter,
				aqlBuilder -> aqlBuilder.andEquals(
					accountExternalKey,
					AccountContactRoleAssignmentConstants.
						ATTRIBUTE_NAME_ACCOUNT_EXTERNAL_KEY
				).andEquals(
					false,
					AccountContactRoleAssignmentConstants.ATTRIBUTE_NAME_DELETED
				));

		for (JiraAssetObject jiraAssetObject : jiraAssetObjects) {
			String name = jiraAssetObject.getAttributeValue(
				AccountContactRoleAssignmentConstants.ATTRIBUTE_NAME_NAME);

			if (FindUtil.containsIgnoreCase(names, name)) {
				continue;
			}

			String roleExternalKey = jiraAssetObject.getAttributeValue(
				AccountContactRoleAssignmentConstants.
					ATTRIBUTE_NAME_CONTACT_ROLE_EXTERNAL_KEY);
			String userAccountExternalKey = jiraAssetObject.getAttributeValue(
				AccountContactRoleAssignmentConstants.
					ATTRIBUTE_NAME_CONTACT_EXTERNAL_KEY);

			try {
				_syncAssignment(
					roleExternalKey, userAccountExternalKey, accountExternalKey,
					true,
					(existingJiraAssetObject, newJiraAssetObject) ->
						_jiraAssetService.isUpdatedSince(
							_accountContactRoleAssignmentConverter, startDate,
							existingJiraAssetObject));
			}
			catch (Exception exception) {
				_log.error(
					StringBundler.concat(
						"Unable to unassign stale role ", roleExternalKey,
						" for user account ", userAccountExternalKey,
						" on account ", accountExternalKey),
					exception);
			}
		}
	}

	private void _syncAssignment(
			String roleExternalKey, String userAccountExternalKey,
			String accountExternalKey, boolean deleted,
			BiPredicate<JiraAssetObject, JiraAssetObject>
				shouldSkipUpdateBiPredicate)
		throws Exception {

		_jiraSyncLock.withLock(
			userAccountExternalKey,
			() -> _syncAssignmentWithinLock(
				roleExternalKey, userAccountExternalKey, accountExternalKey,
				deleted, shouldSkipUpdateBiPredicate));
	}

	private void _syncAssignmentWithinLock(
		String roleExternalKey, String userAccountExternalKey,
		String accountExternalKey, boolean deleted,
		BiPredicate<JiraAssetObject, JiraAssetObject>
			shouldSkipUpdateBiPredicate) {

		if (_log.isInfoEnabled()) {
			_log.info(
				StringBundler.concat(
					deleted ? "Unassigning" : "Assigning", " role ",
					roleExternalKey, " for user account ",
					userAccountExternalKey, " on account ",
					accountExternalKey));
		}

		JiraAssetObject jiraAssetObject =
			_accountContactRoleAssignmentConverter.toAssetObject(
				roleExternalKey, userAccountExternalKey, accountExternalKey,
				deleted, new Date());

		jiraAssetObject.setAttributeValue(
			AccountContactRoleAssignmentConstants.ATTRIBUTE_NAME_CONTACT_ROLE,
			_jiraAssetService.getReferenceObjectId(
				_contactRoleConverter, roleExternalKey));
		jiraAssetObject.setAttributeValue(
			AccountContactRoleAssignmentConstants.ATTRIBUTE_NAME_CONTACT,
			_jiraAssetService.getReferenceObjectId(
				_contactConverter, userAccountExternalKey));
		jiraAssetObject.setAttributeValue(
			AccountContactRoleAssignmentConstants.ATTRIBUTE_NAME_ACCOUNT,
			_jiraAssetService.getReferenceObjectId(
				_accountConverter, accountExternalKey));

		_jiraAssetService.upsert(
			_accountContactRoleAssignmentConverter, jiraAssetObject,
			shouldSkipUpdateBiPredicate);
	}

	private static final Log _log = LogFactory.getLog(
		AccountUserAccountRoleSynchronizer.class);

	@Autowired
	private AccountContactRoleAssignmentConverter
		_accountContactRoleAssignmentConverter;

	@Autowired
	private AccountConverter _accountConverter;

	@Autowired
	private ContactConverter _contactConverter;

	@Autowired
	private ContactRoleConverter _contactRoleConverter;

	@Autowired
	private JiraAssetService _jiraAssetService;

	@Autowired
	private JiraSyncLock _jiraSyncLock;

}