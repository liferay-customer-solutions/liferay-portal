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
import com.liferay.one.jira.util.AQLUtil;
import com.liferay.one.util.KeyedLock;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.StringUtil;

import java.util.Collections;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Drew Brokke
 */
public class AccountUserAccountRoleSynchronizerTest {

	@BeforeEach
	public void setUp() throws Exception {
		_accountUserAccountRoleSynchronizer =
			new AccountUserAccountRoleSynchronizer();

		_jiraAssetService = Mockito.mock(JiraAssetService.class);

		_accountContactRoleAssignmentConverter = Mockito.mock(
			AccountContactRoleAssignmentConverter.class);

		Mockito.when(
			_accountContactRoleAssignmentConverter.getName(
				Mockito.any(), Mockito.any(), Mockito.any())
		).thenAnswer(
			invocation -> StringBundler.concat(
				invocation.getArgument(2), ";", invocation.getArgument(1), ";",
				invocation.getArgument(0))
		);

		Mockito.when(
			_accountContactRoleAssignmentConverter.toAssetObject(
				Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.anyBoolean(), Mockito.any())
		).thenReturn(
			Mockito.mock(JiraAssetObject.class)
		);

		ReflectionTestUtils.setField(
			_accountUserAccountRoleSynchronizer,
			"_accountContactRoleAssignmentConverter",
			_accountContactRoleAssignmentConverter);

		ReflectionTestUtils.setField(
			_accountUserAccountRoleSynchronizer, "_accountConverter",
			Mockito.mock(AccountConverter.class));
		ReflectionTestUtils.setField(
			_accountUserAccountRoleSynchronizer, "_contactConverter",
			Mockito.mock(ContactConverter.class));
		ReflectionTestUtils.setField(
			_accountUserAccountRoleSynchronizer, "_contactRoleConverter",
			Mockito.mock(ContactRoleConverter.class));
		ReflectionTestUtils.setField(
			_accountUserAccountRoleSynchronizer, "_jiraAssetService",
			_jiraAssetService);
		ReflectionTestUtils.setField(
			_accountUserAccountRoleSynchronizer, "_keyedLock", new KeyedLock());
	}

	@Test
	public void testSoftDeleteByAccount() {
		_accountUserAccountRoleSynchronizer.softDeleteByAccount("account-erc");

		Mockito.verify(
			_jiraAssetService
		).softDeleteByAttribute(
			_accountContactRoleAssignmentConverter,
			AccountContactRoleAssignmentConstants.
				ATTRIBUTE_NAME_ACCOUNT_EXTERNAL_KEY,
			"account-erc"
		);
	}

	@Test
	public void testSoftDeleteByUserAccount() {
		_accountUserAccountRoleSynchronizer.softDeleteByUserAccount(
			"user-account-erc");

		Mockito.verify(
			_jiraAssetService
		).softDeleteByAttribute(
			_accountContactRoleAssignmentConverter,
			AccountContactRoleAssignmentConstants.
				ATTRIBUTE_NAME_CONTACT_EXTERNAL_KEY,
			"user-account-erc"
		);
	}

	@Test
	public void testSyncUnassignStaleRolesBaseAQL() throws Exception {
		AtomicReference<String> aqlAtomicReference = _captureAQL();

		_accountUserAccountRoleSynchronizer.syncUnassignStaleRoles(
			_ACCOUNT_EXTERNAL_KEY, Collections.emptyMap(), new Date());

		Assertions.assertEquals(
			"base AND \"Account External Key\" = \"account-erc\" AND " +
				"\"Deleted\" = false",
			aqlAtomicReference.get());
	}

	@Test
	public void testSyncUnassignStaleRolesSkipsCaseDifferingCurrentAssignments()
		throws Exception {

		_assertSkipped(StringUtil.toUpperCase(_NAME));
	}

	@Test
	public void testSyncUnassignStaleRolesSkipsCurrentAssignments()
		throws Exception {

		_assertSkipped(_NAME);
	}

	@Test
	public void testSyncUnassignStaleRolesSkipsFreshAssignments()
		throws Exception {

		JiraAssetObject jiraAssetObject = _mockAssignment(_ROLE_EXTERNAL_KEY);

		Mockito.when(
			_jiraAssetService.getJiraAssetObjects(Mockito.any(), Mockito.any())
		).thenReturn(
			Collections.singletonList(jiraAssetObject)
		);

		Date startDate = new Date();

		_accountUserAccountRoleSynchronizer.syncUnassignStaleRoles(
			_ACCOUNT_EXTERNAL_KEY, Collections.emptyMap(), startDate);

		ArgumentCaptor<BiPredicate<JiraAssetObject, JiraAssetObject>>
			biPredicateArgumentCaptor = ArgumentCaptor.forClass(
				BiPredicate.class);

		Mockito.verify(
			_jiraAssetService
		).upsert(
			Mockito.eq(_accountContactRoleAssignmentConverter), Mockito.any(),
			biPredicateArgumentCaptor.capture()
		);

		JiraAssetObject existingJiraAssetObject = Mockito.mock(
			JiraAssetObject.class);

		Mockito.when(
			_jiraAssetService.isUpdatedSince(
				_accountContactRoleAssignmentConverter, startDate,
				existingJiraAssetObject)
		).thenReturn(
			true
		);

		BiPredicate<JiraAssetObject, JiraAssetObject> biPredicate =
			biPredicateArgumentCaptor.getValue();

		Assertions.assertTrue(
			biPredicate.test(
				existingJiraAssetObject, Mockito.mock(JiraAssetObject.class)));
	}

	@Test
	public void testSyncUnassignStaleRolesSoftDeletesStaleAssignments()
		throws Exception {

		JiraAssetObject jiraAssetObject = _mockAssignment(_ROLE_EXTERNAL_KEY);

		Mockito.when(
			_jiraAssetService.getJiraAssetObjects(Mockito.any(), Mockito.any())
		).thenReturn(
			Collections.singletonList(jiraAssetObject)
		);

		_accountUserAccountRoleSynchronizer.syncUnassignStaleRoles(
			_ACCOUNT_EXTERNAL_KEY,
			Collections.singletonMap(
				_USER_ACCOUNT_EXTERNAL_KEY,
				Collections.singleton("other-role-erc")),
			new Date());

		Mockito.verify(
			_accountContactRoleAssignmentConverter
		).toAssetObject(
			Mockito.eq(_ROLE_EXTERNAL_KEY),
			Mockito.eq(_USER_ACCOUNT_EXTERNAL_KEY),
			Mockito.eq(_ACCOUNT_EXTERNAL_KEY), Mockito.eq(true), Mockito.any()
		);

		Mockito.verify(
			_jiraAssetService
		).upsert(
			Mockito.eq(_accountContactRoleAssignmentConverter), Mockito.any(),
			Mockito.any()
		);
	}

	private void _assertSkipped(String name) {
		JiraAssetObject jiraAssetObject = _mockAssignment(_ROLE_EXTERNAL_KEY);

		Mockito.when(
			jiraAssetObject.getAttributeValue(
				AccountContactRoleAssignmentConstants.ATTRIBUTE_NAME_NAME)
		).thenReturn(
			name
		);

		Mockito.when(
			_jiraAssetService.getJiraAssetObjects(Mockito.any(), Mockito.any())
		).thenReturn(
			Collections.singletonList(jiraAssetObject)
		);

		_accountUserAccountRoleSynchronizer.syncUnassignStaleRoles(
			_ACCOUNT_EXTERNAL_KEY,
			Collections.singletonMap(
				_USER_ACCOUNT_EXTERNAL_KEY,
				Collections.singleton(_ROLE_EXTERNAL_KEY)),
			new Date());

		Mockito.verify(
			_jiraAssetService, Mockito.never()
		).upsert(
			Mockito.any(), Mockito.any(), Mockito.any()
		);
	}

	private AtomicReference<String> _captureAQL() {
		AtomicReference<String> aqlAtomicReference = new AtomicReference<>();

		Mockito.when(
			_jiraAssetService.getJiraAssetObjects(Mockito.any(), Mockito.any())
		).thenAnswer(
			invocation -> {
				Consumer<AQLUtil.Builder> consumer = invocation.getArgument(1);

				AQLUtil.Builder aqlBuilder = AQLUtil.builder("base");

				consumer.accept(aqlBuilder);

				aqlAtomicReference.set(aqlBuilder.build());

				return Collections.emptyList();
			}
		);

		return aqlAtomicReference;
	}

	private JiraAssetObject _mockAssignment(String roleExternalKey) {
		JiraAssetObject jiraAssetObject = Mockito.mock(JiraAssetObject.class);

		Mockito.when(
			jiraAssetObject.getAttributeValue(
				AccountContactRoleAssignmentConstants.
					ATTRIBUTE_NAME_CONTACT_EXTERNAL_KEY)
		).thenReturn(
			_USER_ACCOUNT_EXTERNAL_KEY
		);

		Mockito.when(
			jiraAssetObject.getAttributeValue(
				AccountContactRoleAssignmentConstants.
					ATTRIBUTE_NAME_CONTACT_ROLE_EXTERNAL_KEY)
		).thenReturn(
			roleExternalKey
		);

		return jiraAssetObject;
	}

	private static final String _ACCOUNT_EXTERNAL_KEY = "account-erc";

	private static final String _NAME = "role-erc;user-account-erc;account-erc";

	private static final String _ROLE_EXTERNAL_KEY = "role-erc";

	private static final String _USER_ACCOUNT_EXTERNAL_KEY = "user-account-erc";

	private AccountContactRoleAssignmentConverter
		_accountContactRoleAssignmentConverter;
	private AccountUserAccountRoleSynchronizer
		_accountUserAccountRoleSynchronizer;
	private JiraAssetService _jiraAssetService;

}