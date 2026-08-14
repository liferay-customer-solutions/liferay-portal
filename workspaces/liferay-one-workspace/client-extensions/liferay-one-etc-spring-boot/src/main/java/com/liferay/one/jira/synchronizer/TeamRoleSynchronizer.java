/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.jira.synchronizer;

import com.liferay.one.jira.constants.TeamRoleConstants;
import com.liferay.one.jira.converter.TeamRoleConverter;
import com.liferay.one.jira.exception.JiraAssetObjectException;
import com.liferay.one.jira.service.JiraAssetService;
import com.liferay.one.util.KeyedLock;
import com.liferay.petra.string.StringBundler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author Drew Brokke
 */
@Component
public class TeamRoleSynchronizer {

	public String getFirstLineSupportTeamRoleObjectId() {
		String objectId = _firstLineSupportTeamRoleObjectId;

		if (objectId != null) {
			return objectId;
		}

		return _keyedLock.withLock(
			TeamRoleConstants.EXTERNAL_KEY_FIRST_LINE_SUPPORT,
			() -> {
				if (_firstLineSupportTeamRoleObjectId != null) {
					return _firstLineSupportTeamRoleObjectId;
				}

				String resolvedObjectId = _resolveOrCreate();

				if (resolvedObjectId == null) {
					throw new JiraAssetObjectException(
						StringBundler.concat(
							"No \"", _teamRoleConverter.getObjectTypeName(),
							"\" asset object exists for external key ",
							TeamRoleConstants.EXTERNAL_KEY_FIRST_LINE_SUPPORT));
				}

				_firstLineSupportTeamRoleObjectId = resolvedObjectId;

				return resolvedObjectId;
			});
	}

	@Async
	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReady() {
		try {
			getFirstLineSupportTeamRoleObjectId();
		}
		catch (Exception exception) {
			_log.error(
				"Unable to sync team roles on application startup", exception);
		}
	}

	@Scheduled(cron = "${liferay.one.jira.team.role.sync.cron}")
	public void syncTeamRoles() {
		String objectId = _jiraAssetService.fetchReferenceObjectId(
			_teamRoleConverter,
			TeamRoleConstants.EXTERNAL_KEY_FIRST_LINE_SUPPORT);

		if (objectId == null) {
			_log.error(
				StringBundler.concat(
					"Unable to find the ",
					TeamRoleConstants.NAME_FIRST_LINE_SUPPORT,
					" team role asset object for external key ",
					TeamRoleConstants.EXTERNAL_KEY_FIRST_LINE_SUPPORT,
					", recreating it"));

			objectId = _keyedLock.withLock(
				TeamRoleConstants.EXTERNAL_KEY_FIRST_LINE_SUPPORT,
				this::_resolveOrCreate);
		}

		_firstLineSupportTeamRoleObjectId = objectId;
	}

	private String _resolveOrCreate() {
		String objectId = _jiraAssetService.fetchReferenceObjectId(
			_teamRoleConverter,
			TeamRoleConstants.EXTERNAL_KEY_FIRST_LINE_SUPPORT);

		if (objectId != null) {
			return objectId;
		}

		_jiraAssetService.upsert(
			_teamRoleConverter,
			_teamRoleConverter.toFirstLineSupportAssetObject());

		return _jiraAssetService.fetchReferenceObjectId(
			_teamRoleConverter,
			TeamRoleConstants.EXTERNAL_KEY_FIRST_LINE_SUPPORT);
	}

	private static final Log _log = LogFactory.getLog(
		TeamRoleSynchronizer.class);

	private volatile String _firstLineSupportTeamRoleObjectId;

	@Autowired
	private JiraAssetService _jiraAssetService;

	@Autowired
	private KeyedLock _keyedLock;

	@Autowired
	private TeamRoleConverter _teamRoleConverter;

}