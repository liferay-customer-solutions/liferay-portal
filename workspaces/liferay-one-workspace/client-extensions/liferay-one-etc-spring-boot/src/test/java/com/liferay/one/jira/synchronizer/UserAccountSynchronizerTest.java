/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.jira.synchronizer;

import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
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
import com.liferay.one.service.EntitlementService;
import com.liferay.one.service.PropertyService;
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
public class UserAccountSynchronizerTest {

	@BeforeEach
	public void setUp() throws Exception {
		_userAccountSynchronizer = new UserAccountSynchronizer();

		_accountUserAccountRoleSynchronizer = Mockito.mock(
			AccountUserAccountRoleSynchronizer.class);
		_contactConverter = Mockito.mock(ContactConverter.class);
		_jiraAssetService = Mockito.mock(JiraAssetService.class);
		_organizationUserAccountRoleSynchronizer = Mockito.mock(
			OrganizationUserAccountRoleSynchronizer.class);

		PropertyService propertyService = Mockito.mock(PropertyService.class);

		Mockito.when(
			propertyService.getUserAccountProperties(Mockito.anyLong())
		).thenReturn(
			Collections.emptyList()
		);

		_jiraAssetObject = Mockito.mock(JiraAssetObject.class);

		Mockito.when(
			_contactConverter.toAssetObject(Mockito.any(UserAccount.class))
		).thenReturn(
			_jiraAssetObject
		);

		ReflectionTestUtils.setField(
			_userAccountSynchronizer, "_accountConverter",
			Mockito.mock(AccountConverter.class));
		ReflectionTestUtils.setField(
			_userAccountSynchronizer, "_accountUserAccountRoleSynchronizer",
			_accountUserAccountRoleSynchronizer);
		ReflectionTestUtils.setField(
			_userAccountSynchronizer, "_contactConverter", _contactConverter);
		ReflectionTestUtils.setField(
			_userAccountSynchronizer, "_contactRoleConverter",
			Mockito.mock(ContactRoleConverter.class));
		ReflectionTestUtils.setField(
			_userAccountSynchronizer, "_entitlementConverter",
			Mockito.mock(EntitlementConverter.class));
		ReflectionTestUtils.setField(
			_userAccountSynchronizer, "_entitlementService",
			Mockito.mock(EntitlementService.class));
		ReflectionTestUtils.setField(
			_userAccountSynchronizer, "_externalLinkConverter",
			Mockito.mock(ExternalLinkConverter.class));
		ReflectionTestUtils.setField(
			_userAccountSynchronizer, "_jiraAssetService", _jiraAssetService);
		ReflectionTestUtils.setField(
			_userAccountSynchronizer, "_keyedLock", new KeyedLock());
		ReflectionTestUtils.setField(
			_userAccountSynchronizer,
			"_organizationUserAccountRoleSynchronizer",
			_organizationUserAccountRoleSynchronizer);
		ReflectionTestUtils.setField(
			_userAccountSynchronizer, "_phoneConverter",
			Mockito.mock(PhoneConverter.class));
		ReflectionTestUtils.setField(
			_userAccountSynchronizer, "_propertyService", propertyService);
		ReflectionTestUtils.setField(
			_userAccountSynchronizer, "_teamConverter",
			Mockito.mock(TeamConverter.class));
	}

	@Test
	public void testDeleteUserAccountContinuesWhenSoftDeleteFails() {
		Mockito.doThrow(
			new RuntimeException()
		).when(
			_accountUserAccountRoleSynchronizer
		).softDeleteByUserAccount(
			Mockito.any()
		);

		Mockito.doThrow(
			new RuntimeException()
		).when(
			_organizationUserAccountRoleSynchronizer
		).softDeleteByUserAccount(
			Mockito.any()
		);

		_userAccountSynchronizer.deleteUserAccount(_EXTERNAL_REFERENCE_CODE);

		Mockito.verify(
			_organizationUserAccountRoleSynchronizer
		).softDeleteByUserAccount(
			_EXTERNAL_REFERENCE_CODE
		);

		Mockito.verify(
			_jiraAssetService
		).delete(
			Mockito.any(), Mockito.eq(_EXTERNAL_REFERENCE_CODE)
		);
	}

	@Test
	public void testDeleteUserAccountSoftDeletesAssignmentsFirst() {
		_userAccountSynchronizer.deleteUserAccount(_EXTERNAL_REFERENCE_CODE);

		InOrder inOrder = Mockito.inOrder(
			_accountUserAccountRoleSynchronizer, _jiraAssetService,
			_organizationUserAccountRoleSynchronizer);

		inOrder.verify(
			_accountUserAccountRoleSynchronizer
		).softDeleteByUserAccount(
			_EXTERNAL_REFERENCE_CODE
		);

		inOrder.verify(
			_organizationUserAccountRoleSynchronizer
		).softDeleteByUserAccount(
			_EXTERNAL_REFERENCE_CODE
		);

		inOrder.verify(
			_jiraAssetService
		).delete(
			Mockito.any(), Mockito.eq(_EXTERNAL_REFERENCE_CODE)
		);
	}

	@Test
	public void testDeleteUserAccountWaitsForSyncUserAccount()
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

		UserAccount userAccount = _createUserAccount();

		lockSerializationTestHelper.assertSerialized(
			() -> _userAccountSynchronizer.syncUserAccount(userAccount),
			() -> _userAccountSynchronizer.deleteUserAccount(
				_EXTERNAL_REFERENCE_CODE),
			"upsert", "delete");
	}

	@Test
	public void testSyncUserAccountAccountsUpsertsAccountReferences() {
		_assertUpsertsAttribute(
			ContactConstants.ATTRIBUTE_NAME_ACCOUNT,
			() -> _userAccountSynchronizer.syncUserAccountAccounts(
				_createUserAccount()));
	}

	@Test
	public void testSyncUserAccountOrganizationsUpsertsOrganizationReferences() {
		_assertUpsertsAttribute(
			ContactConstants.ATTRIBUTE_NAME_TEAMS,
			() -> _userAccountSynchronizer.syncUserAccountOrganizations(
				_createUserAccount()));
	}

	@Test
	public void testSyncUserAccountRolesUpsertsRoleReferences() {
		_assertUpsertsAttribute(
			ContactConstants.ATTRIBUTE_NAME_CONTACT_ROLES,
			() -> _userAccountSynchronizer.syncUserAccountRoles(
				_createUserAccount()));
	}

	private void _assertUpsertsAttribute(
		String attributeName, Runnable runnable) {

		runnable.run();

		Mockito.verify(
			_jiraAssetObject
		).setAttributeValue(
			Mockito.eq(attributeName), Mockito.any()
		);

		Mockito.verify(
			_jiraAssetService
		).upsert(
			Mockito.any(), Mockito.eq(_jiraAssetObject)
		);
	}

	private UserAccount _createUserAccount() {
		UserAccount userAccount = new UserAccount();

		userAccount.setExternalReferenceCode(_EXTERNAL_REFERENCE_CODE);
		userAccount.setId(1L);

		return userAccount;
	}

	private static final String _EXTERNAL_REFERENCE_CODE =
		"test-external-reference-code";

	private AccountUserAccountRoleSynchronizer
		_accountUserAccountRoleSynchronizer;
	private ContactConverter _contactConverter;
	private JiraAssetObject _jiraAssetObject;
	private JiraAssetService _jiraAssetService;
	private OrganizationUserAccountRoleSynchronizer
		_organizationUserAccountRoleSynchronizer;
	private UserAccountSynchronizer _userAccountSynchronizer;

}