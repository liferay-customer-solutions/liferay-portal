/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.one.okta.service.OktaService;
import com.liferay.one.permission.AdminPermission;
import com.liferay.one.service.UserAccountService;
import com.liferay.portal.kernel.security.auth.PrincipalException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Ricardo Mariz
 */
public class UserAccountsRestControllerTest {

	@Test
	public void testPostSyncWithOktaChecksAdminPermission() throws Exception {
		UserAccountsRestController userAccountsRestController =
			_createController();

		Mockito.doThrow(
			new PrincipalException()
		).when(
			_adminPermission
		).check(
			null
		);

		Assertions.assertThrows(
			PrincipalException.class,
			() -> userAccountsRestController.postSyncWithOkta(null, _USER_ID));

		Mockito.verifyNoInteractions(_oktaService);
	}

	@Test
	public void testPostSyncWithOktaSyncsContact() throws Exception {
		UserAccountsRestController userAccountsRestController =
			_createController();

		UserAccount userAccount = new UserAccount();

		userAccount.setEmailAddress("jane@example.com");
		userAccount.setExternalReferenceCode("uuid-1234");
		userAccount.setFamilyName("Doe");
		userAccount.setGivenName("Jane");

		Mockito.when(
			_userAccountService.getUserAccount(_USER_ID)
		).thenReturn(
			userAccount
		);

		userAccountsRestController.postSyncWithOkta(null, _USER_ID);

		Mockito.verify(
			_oktaService
		).syncContact(
			"jane@example.com", "Jane", "Doe", "uuid-1234"
		);
	}

	private UserAccountsRestController _createController() {
		UserAccountsRestController userAccountsRestController =
			new UserAccountsRestController();

		ReflectionTestUtils.setField(
			userAccountsRestController, "_adminPermission", _adminPermission);
		ReflectionTestUtils.setField(
			userAccountsRestController, "_oktaService", _oktaService);
		ReflectionTestUtils.setField(
			userAccountsRestController, "_userAccountService",
			_userAccountService);

		return userAccountsRestController;
	}

	private static final long _USER_ID = 42;

	private final AdminPermission _adminPermission = Mockito.mock(
		AdminPermission.class);
	private final OktaService _oktaService = Mockito.mock(OktaService.class);
	private final UserAccountService _userAccountService = Mockito.mock(
		UserAccountService.class);

}