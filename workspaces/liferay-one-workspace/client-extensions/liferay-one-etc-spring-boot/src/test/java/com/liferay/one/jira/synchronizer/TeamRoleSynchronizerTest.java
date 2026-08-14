/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.jira.synchronizer;

import com.liferay.one.jira.constants.TeamRoleConstants;
import com.liferay.one.jira.converter.TeamRoleConverter;
import com.liferay.one.jira.model.JiraAssetObject;
import com.liferay.one.jira.service.JiraAssetService;
import com.liferay.one.util.KeyedLock;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Drew Brokke
 */
public class TeamRoleSynchronizerTest {

	@BeforeEach
	public void setUp() throws Exception {
		_teamRoleSynchronizer = new TeamRoleSynchronizer();

		_jiraAssetService = Mockito.mock(JiraAssetService.class);

		_teamRoleConverter = Mockito.mock(TeamRoleConverter.class);

		Mockito.when(
			_teamRoleConverter.toFirstLineSupportAssetObject()
		).thenReturn(
			_jiraAssetObject
		);

		ReflectionTestUtils.setField(
			_teamRoleSynchronizer, "_jiraAssetService", _jiraAssetService);
		ReflectionTestUtils.setField(
			_teamRoleSynchronizer, "_keyedLock", new KeyedLock());
		ReflectionTestUtils.setField(
			_teamRoleSynchronizer, "_teamRoleConverter", _teamRoleConverter);
	}

	@Test
	public void testGetFirstLineSupportTeamRoleObjectIdCachesObjectId() {
		_mockFetchReferenceObjectId("12345");

		Assertions.assertEquals(
			"12345",
			_teamRoleSynchronizer.getFirstLineSupportTeamRoleObjectId());
		Assertions.assertEquals(
			"12345",
			_teamRoleSynchronizer.getFirstLineSupportTeamRoleObjectId());

		Mockito.verify(
			_jiraAssetService, Mockito.times(1)
		).fetchReferenceObjectId(
			Mockito.any(), Mockito.any()
		);

		Mockito.verify(
			_jiraAssetService, Mockito.never()
		).upsert(
			Mockito.any(), Mockito.any()
		);
	}

	@Test
	public void testGetFirstLineSupportTeamRoleObjectIdCreatesMissingObject() {
		_mockFetchReferenceObjectId(null, "12345");

		Assertions.assertEquals(
			"12345",
			_teamRoleSynchronizer.getFirstLineSupportTeamRoleObjectId());

		Mockito.verify(
			_jiraAssetService
		).upsert(
			_teamRoleConverter, _jiraAssetObject
		);
	}

	@Test
	public void testOnApplicationReadyCachesObjectId() {
		_mockFetchReferenceObjectId("12345");

		_teamRoleSynchronizer.onApplicationReady();

		Assertions.assertEquals(
			"12345",
			_teamRoleSynchronizer.getFirstLineSupportTeamRoleObjectId());

		Mockito.verify(
			_jiraAssetService, Mockito.times(1)
		).fetchReferenceObjectId(
			Mockito.any(), Mockito.any()
		);
	}

	@Test
	public void testSyncTeamRolesRecreatesMissingObject() {
		_mockFetchReferenceObjectId(null, null, "12345");

		_teamRoleSynchronizer.syncTeamRoles();

		Mockito.verify(
			_jiraAssetService
		).upsert(
			_teamRoleConverter, _jiraAssetObject
		);

		Assertions.assertEquals(
			"12345",
			_teamRoleSynchronizer.getFirstLineSupportTeamRoleObjectId());
	}

	@Test
	public void testSyncTeamRolesRefreshesCachedObjectId() {
		_mockFetchReferenceObjectId("12345", "67890");

		Assertions.assertEquals(
			"12345",
			_teamRoleSynchronizer.getFirstLineSupportTeamRoleObjectId());

		_teamRoleSynchronizer.syncTeamRoles();

		Assertions.assertEquals(
			"67890",
			_teamRoleSynchronizer.getFirstLineSupportTeamRoleObjectId());

		Mockito.verify(
			_jiraAssetService, Mockito.never()
		).upsert(
			Mockito.any(), Mockito.any()
		);
	}

	private void _mockFetchReferenceObjectId(
		String objectId, String... objectIds) {

		Mockito.when(
			_jiraAssetService.fetchReferenceObjectId(
				_teamRoleConverter,
				TeamRoleConstants.EXTERNAL_KEY_FIRST_LINE_SUPPORT)
		).thenReturn(
			objectId, objectIds
		);
	}

	private final JiraAssetObject _jiraAssetObject = Mockito.mock(
		JiraAssetObject.class);
	private JiraAssetService _jiraAssetService;
	private TeamRoleConverter _teamRoleConverter;
	private TeamRoleSynchronizer _teamRoleSynchronizer;

}