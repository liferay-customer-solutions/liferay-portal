/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.jira.synchronizer;

import com.liferay.headless.admin.user.client.dto.v1_0.Organization;
import com.liferay.one.jira.constants.TeamConstants;
import com.liferay.one.jira.converter.AccountConverter;
import com.liferay.one.jira.converter.ContactConverter;
import com.liferay.one.jira.converter.ExternalLinkConverter;
import com.liferay.one.jira.converter.TeamConverter;
import com.liferay.one.jira.model.JiraAssetObject;
import com.liferay.one.jira.service.JiraAssetService;
import com.liferay.one.service.PropertyService;
import com.liferay.one.service.UserAccountService;
import com.liferay.one.util.KeyedLock;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.InOrder;
import org.mockito.Mockito;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Drew Brokke
 */
public class OrganizationSynchronizerTest {

	@BeforeEach
	public void setUp() throws Exception {
		_organizationSynchronizer = new OrganizationSynchronizer();

		_jiraAssetService = Mockito.mock(JiraAssetService.class);

		PropertyService propertyService = Mockito.mock(PropertyService.class);

		Mockito.when(
			propertyService.getOrganizationProperties(Mockito.anyLong())
		).thenReturn(
			Collections.emptyList()
		);

		_jiraAssetObject = Mockito.mock(JiraAssetObject.class);

		TeamConverter teamConverter = Mockito.mock(TeamConverter.class);

		Mockito.when(
			teamConverter.toAssetObject(Mockito.any(Organization.class))
		).thenReturn(
			_jiraAssetObject
		);

		UserAccountService userAccountService = Mockito.mock(
			UserAccountService.class);

		Mockito.when(
			userAccountService.getOrganizationUserAccounts(Mockito.anyLong())
		).thenReturn(
			Collections.emptyList()
		);

		_accountOrganizationSynchronizer = Mockito.mock(
			AccountOrganizationSynchronizer.class);
		_organizationUserAccountRoleSynchronizer = Mockito.mock(
			OrganizationUserAccountRoleSynchronizer.class);

		ReflectionTestUtils.setField(
			_organizationSynchronizer, "_accountConverter",
			Mockito.mock(AccountConverter.class));
		ReflectionTestUtils.setField(
			_organizationSynchronizer, "_accountOrganizationSynchronizer",
			_accountOrganizationSynchronizer);
		ReflectionTestUtils.setField(
			_organizationSynchronizer, "_contactConverter",
			Mockito.mock(ContactConverter.class));
		ReflectionTestUtils.setField(
			_organizationSynchronizer, "_externalLinkConverter",
			Mockito.mock(ExternalLinkConverter.class));
		ReflectionTestUtils.setField(
			_organizationSynchronizer, "_jiraAssetService", _jiraAssetService);
		ReflectionTestUtils.setField(
			_organizationSynchronizer, "_keyedLock", new KeyedLock());
		ReflectionTestUtils.setField(
			_organizationSynchronizer,
			"_organizationUserAccountRoleSynchronizer",
			_organizationUserAccountRoleSynchronizer);
		ReflectionTestUtils.setField(
			_organizationSynchronizer, "_propertyService", propertyService);
		ReflectionTestUtils.setField(
			_organizationSynchronizer, "_teamConverter", teamConverter);
		ReflectionTestUtils.setField(
			_organizationSynchronizer, "_teamRoleSynchronizer",
			Mockito.mock(TeamRoleSynchronizer.class));
		ReflectionTestUtils.setField(
			_organizationSynchronizer, "_userAccountService",
			userAccountService);
	}

	@Test
	public void testDeleteOrganizationContinuesWhenSoftDeleteFails() {
		Mockito.doThrow(
			new RuntimeException()
		).when(
			_accountOrganizationSynchronizer
		).softDeleteByOrganization(
			Mockito.any()
		);

		Mockito.doThrow(
			new RuntimeException()
		).when(
			_organizationUserAccountRoleSynchronizer
		).softDeleteByOrganization(
			Mockito.any()
		);

		_organizationSynchronizer.deleteOrganization(_EXTERNAL_REFERENCE_CODE);

		Mockito.verify(
			_organizationUserAccountRoleSynchronizer
		).softDeleteByOrganization(
			_EXTERNAL_REFERENCE_CODE
		);

		Mockito.verify(
			_jiraAssetService
		).delete(
			Mockito.any(), Mockito.eq(_EXTERNAL_REFERENCE_CODE)
		);
	}

	@Test
	public void testDeleteOrganizationSoftDeletesAssignmentsFirst() {
		_organizationSynchronizer.deleteOrganization(_EXTERNAL_REFERENCE_CODE);

		InOrder inOrder = Mockito.inOrder(
			_accountOrganizationSynchronizer, _jiraAssetService,
			_organizationUserAccountRoleSynchronizer);

		inOrder.verify(
			_accountOrganizationSynchronizer
		).softDeleteByOrganization(
			_EXTERNAL_REFERENCE_CODE
		);

		inOrder.verify(
			_organizationUserAccountRoleSynchronizer
		).softDeleteByOrganization(
			_EXTERNAL_REFERENCE_CODE
		);

		inOrder.verify(
			_jiraAssetService
		).delete(
			Mockito.any(), Mockito.eq(_EXTERNAL_REFERENCE_CODE)
		);
	}

	@Test
	public void testDeleteOrganizationWaitsForSyncOrganization()
		throws Exception {

		LockSerializationTestHelper lockSerializationTestHelper =
			new LockSerializationTestHelper();

		Mockito.doAnswer(
			lockSerializationTestHelper.block("upsert")
		).when(
			_jiraAssetService
		).upsert(
			Mockito.any(), Mockito.any()
		);

		Mockito.doAnswer(
			lockSerializationTestHelper.record("delete")
		).when(
			_jiraAssetService
		).delete(
			Mockito.any(), Mockito.any()
		);

		Organization organization = new Organization();

		organization.setExternalReferenceCode(_EXTERNAL_REFERENCE_CODE);

		lockSerializationTestHelper.assertSerialized(
			() -> _organizationSynchronizer.syncOrganization(organization),
			() -> _organizationSynchronizer.deleteOrganization(
				_EXTERNAL_REFERENCE_CODE),
			"upsert", "delete");
	}

	@Test
	public void testSyncOrganizationUserAccountsUpsertsUserAccountReferences()
		throws Exception {

		Organization organization = new Organization();

		organization.setExternalReferenceCode(_EXTERNAL_REFERENCE_CODE);
		organization.setId("1");

		_organizationSynchronizer.syncOrganizationUserAccounts(organization);

		Mockito.verify(
			_jiraAssetObject
		).setAttributeValue(
			Mockito.eq(TeamConstants.ATTRIBUTE_NAME_CONTACTS), Mockito.any()
		);

		Mockito.verify(
			_jiraAssetService
		).upsert(
			Mockito.any(), Mockito.eq(_jiraAssetObject)
		);
	}

	private static final String _EXTERNAL_REFERENCE_CODE =
		"test-external-reference-code";

	private AccountOrganizationSynchronizer _accountOrganizationSynchronizer;
	private JiraAssetObject _jiraAssetObject;
	private JiraAssetService _jiraAssetService;
	private OrganizationSynchronizer _organizationSynchronizer;
	private OrganizationUserAccountRoleSynchronizer
		_organizationUserAccountRoleSynchronizer;

}