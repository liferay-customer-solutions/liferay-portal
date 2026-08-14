/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.jira.synchronizer;

import com.liferay.one.jira.constants.TeamContactRoleAssignmentConstants;
import com.liferay.one.jira.converter.ContactConverter;
import com.liferay.one.jira.converter.ContactRoleConverter;
import com.liferay.one.jira.converter.TeamContactRoleAssignmentConverter;
import com.liferay.one.jira.converter.TeamConverter;
import com.liferay.one.jira.model.JiraAssetObject;
import com.liferay.one.jira.service.JiraAssetService;
import com.liferay.one.util.KeyedLock;
import com.liferay.petra.string.StringBundler;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Drew Brokke
 */
@Component
public class OrganizationUserAccountRoleSynchronizer {

	public void softDeleteByOrganization(String organizationExternalKey) {
		_jiraAssetService.softDeleteByAttribute(
			_teamContactRoleAssignmentConverter,
			TeamContactRoleAssignmentConstants.ATTRIBUTE_NAME_TEAM_EXTERNAL_KEY,
			organizationExternalKey);
	}

	public void softDeleteByUserAccount(String userAccountExternalKey) {
		_jiraAssetService.softDeleteByAttribute(
			_teamContactRoleAssignmentConverter,
			TeamContactRoleAssignmentConstants.
				ATTRIBUTE_NAME_CONTACT_EXTERNAL_KEY,
			userAccountExternalKey);
	}

	public void syncAssignRole(
			String roleExternalKey, String userAccountExternalKey,
			String organizationExternalKey)
		throws Exception {

		_syncAssignment(
			roleExternalKey, userAccountExternalKey, organizationExternalKey,
			false);
	}

	public void syncUnassignRole(
			String roleExternalKey, String userAccountExternalKey,
			String organizationExternalKey)
		throws Exception {

		_syncAssignment(
			roleExternalKey, userAccountExternalKey, organizationExternalKey,
			true);
	}

	private void _syncAssignment(
			String roleExternalKey, String userAccountExternalKey,
			String organizationExternalKey, boolean deleted)
		throws Exception {

		_keyedLock.withLock(
			userAccountExternalKey,
			() -> _syncAssignmentWithinLock(
				roleExternalKey, userAccountExternalKey,
				organizationExternalKey, deleted));
	}

	private void _syncAssignmentWithinLock(
		String roleExternalKey, String userAccountExternalKey,
		String organizationExternalKey, boolean deleted) {

		if (_log.isInfoEnabled()) {
			_log.info(
				StringBundler.concat(
					deleted ? "Unassigning" : "Assigning", " role ",
					roleExternalKey, " for user account ",
					userAccountExternalKey, " on organization ",
					organizationExternalKey));
		}

		JiraAssetObject jiraAssetObject =
			_teamContactRoleAssignmentConverter.toAssetObject(
				roleExternalKey, userAccountExternalKey,
				organizationExternalKey, deleted, new Date());

		jiraAssetObject.setAttributeValue(
			TeamContactRoleAssignmentConstants.ATTRIBUTE_NAME_CONTACT_ROLE,
			_jiraAssetService.getReferenceObjectId(
				_contactRoleConverter, roleExternalKey));
		jiraAssetObject.setAttributeValue(
			TeamContactRoleAssignmentConstants.ATTRIBUTE_NAME_CONTACT,
			_jiraAssetService.getReferenceObjectId(
				_contactConverter, userAccountExternalKey));
		jiraAssetObject.setAttributeValue(
			TeamContactRoleAssignmentConstants.ATTRIBUTE_NAME_TEAM,
			_jiraAssetService.getReferenceObjectId(
				_teamConverter, organizationExternalKey));

		_jiraAssetService.upsert(
			_teamContactRoleAssignmentConverter, jiraAssetObject);
	}

	private static final Log _log = LogFactory.getLog(
		OrganizationUserAccountRoleSynchronizer.class);

	@Autowired
	private ContactConverter _contactConverter;

	@Autowired
	private ContactRoleConverter _contactRoleConverter;

	@Autowired
	private JiraAssetService _jiraAssetService;

	@Autowired
	private KeyedLock _keyedLock;

	@Autowired
	private TeamContactRoleAssignmentConverter
		_teamContactRoleAssignmentConverter;

	@Autowired
	private TeamConverter _teamConverter;

}