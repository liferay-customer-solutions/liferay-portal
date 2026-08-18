/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.admin.user.client.custom.field.CustomField;
import com.liferay.headless.admin.user.client.custom.field.CustomValue;
import com.liferay.headless.admin.user.client.dto.v1_0.Account;
import com.liferay.headless.admin.user.client.dto.v1_0.AccountBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.RoleBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.one.constants.EntitlementConstants;
import com.liferay.one.constants.OpportunityConstants;
import com.liferay.one.constants.RoleConstants;
import com.liferay.one.constants.SupportRegionConstants;
import com.liferay.one.model.AccountSupportInfo;
import com.liferay.one.model.Project;
import com.liferay.one.model.ProjectMembership;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import org.springframework.context.MessageSource;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Felipe Franca
 */
public class ProvisioningEmailServiceTest {

	@BeforeEach
	public void setUp() throws Exception {
		_provisioningEmailService = Mockito.spy(new ProvisioningEmailService());

		_accountService = Mockito.mock(AccountService.class);
		_commerceOrderService = Mockito.mock(CommerceOrderService.class);
		_entitlementService = Mockito.mock(EntitlementService.class);
		_messageSource = Mockito.mock(MessageSource.class);
		_notificationQueueEntryService = Mockito.mock(
			NotificationQueueEntryService.class);
		_notificationTemplateService = Mockito.mock(
			NotificationTemplateService.class);
		_projectMembershipService = Mockito.mock(
			ProjectMembershipService.class);
		_projectService = Mockito.mock(ProjectService.class);
		_userAccountService = Mockito.mock(UserAccountService.class);

		JSONObject templateJSONObject = new JSONObject(
		).put(
			"body", "Body"
		).put(
			"subject", "Subject"
		);

		Mockito.when(
			_notificationTemplateService.getAndProcessTemplateJSONObject(
				Mockito.any(), Mockito.any(), Mockito.any())
		).thenReturn(
			templateJSONObject
		);

		Mockito.when(
			_commerceOrderService.getAccountSupportInfo(
				Mockito.anyLong(), Mockito.any())
		).thenReturn(
			new AccountSupportInfo(
				"en_US", SupportRegionConstants.UNITED_STATES)
		);

		ReflectionTestUtils.setField(
			_provisioningEmailService, "_accountService", _accountService);
		ReflectionTestUtils.setField(
			_provisioningEmailService, "_commerceOrderService",
			_commerceOrderService);
		ReflectionTestUtils.setField(
			_provisioningEmailService, "_emailAddressAustralia",
			"australia@example.com");
		ReflectionTestUtils.setField(
			_provisioningEmailService, "_emailAddressBrazil",
			"brazil@example.com");
		ReflectionTestUtils.setField(
			_provisioningEmailService, "_emailAddressChina",
			"china@example.com");
		ReflectionTestUtils.setField(
			_provisioningEmailService, "_emailAddressGlobal",
			_EMAIL_ADDRESS_GLOBAL);
		ReflectionTestUtils.setField(
			_provisioningEmailService, "_emailAddressHungary",
			"hungary@example.com");
		ReflectionTestUtils.setField(
			_provisioningEmailService, "_emailAddressIndia",
			"india@example.com");
		ReflectionTestUtils.setField(
			_provisioningEmailService, "_emailAddressJapan",
			"japan@example.com");
		ReflectionTestUtils.setField(
			_provisioningEmailService, "_emailAddressSpain",
			"spain@example.com");
		ReflectionTestUtils.setField(
			_provisioningEmailService, "_emailAddressUS", _EMAIL_ADDRESS_US);
		ReflectionTestUtils.setField(
			_provisioningEmailService, "_entitlementService",
			_entitlementService);
		ReflectionTestUtils.setField(
			_provisioningEmailService, "_messageSource", _messageSource);
		ReflectionTestUtils.setField(
			_provisioningEmailService, "_notificationQueueEntryService",
			_notificationQueueEntryService);
		ReflectionTestUtils.setField(
			_provisioningEmailService, "_notificationTemplateService",
			_notificationTemplateService);
		ReflectionTestUtils.setField(
			_provisioningEmailService, "_partnerUserUpdateRecipient",
			_PARTNER_USER_UPDATE_RECIPIENT);
		ReflectionTestUtils.setField(
			_provisioningEmailService, "_portalURL", "https://one.liferay.com");
		ReflectionTestUtils.setField(
			_provisioningEmailService, "_projectMembershipService",
			_projectMembershipService);
		ReflectionTestUtils.setField(
			_provisioningEmailService, "_projectService", _projectService);
		ReflectionTestUtils.setField(
			_provisioningEmailService, "_userAccountService",
			_userAccountService);
	}

	@Test
	public void testSendAssignedWelcomeEmailFallsBackToDefaultLanguageForUnsupportedId()
		throws Exception {

		Account account = _createAccount(_ACCOUNT_ID);

		UserAccount userAccount = _createVerifiedUserAccount(_USER_ID);

		userAccount.setLanguageId("de_DE");

		Mockito.when(
			_userAccountService.getUserAccount(_USER_ID)
		).thenReturn(
			userAccount
		);

		Mockito.when(
			_entitlementService.hasEntitlement(
				_ACCOUNT_ID, EntitlementConstants.NAMES_SLAS)
		).thenReturn(
			true
		);

		_provisioningEmailService.sendAssignedWelcomeEmail(account, _USER_ID);

		Mockito.verify(
			_notificationTemplateService
		).getAndProcessTemplateJSONObject(
			Mockito.eq("PROVISIONING-WELCOME"), Mockito.eq("en_US"),
			Mockito.any()
		);
	}

	@Test
	public void testSendAssignedWelcomeEmailIncludesSingleProjectInvitation()
		throws Exception {

		Account account = _createAccount(_ACCOUNT_ID);

		UserAccount userAccount = _createVerifiedUserAccount(_USER_ID);

		Mockito.when(
			_userAccountService.getUserAccount(_USER_ID)
		).thenReturn(
			userAccount
		);

		Mockito.when(
			_entitlementService.hasEntitlement(
				_ACCOUNT_ID, EntitlementConstants.NAMES_SLAS)
		).thenReturn(
			true
		);

		Mockito.when(
			_projectMembershipService.getProjectMemberships(
				_ACCOUNT_ID, _USER_ID)
		).thenReturn(
			List.of(_createProjectMembership("PROJECT-ERC-1"))
		);

		Mockito.when(
			_projectService.fetchProject("PROJECT-ERC-1")
		).thenReturn(
			_createProject("PROJECT-ERC-1", "My Project")
		);

		_provisioningEmailService.sendAssignedWelcomeEmail(account, _USER_ID);

		ArgumentCaptor<Map<String, String>> placeholdersArgumentCaptor =
			ArgumentCaptor.forClass(Map.class);

		Mockito.verify(
			_notificationTemplateService
		).getAndProcessTemplateJSONObject(
			Mockito.eq("PROVISIONING-WELCOME"), Mockito.any(),
			placeholdersArgumentCaptor.capture()
		);

		Map<String, String> placeholders =
			placeholdersArgumentCaptor.getValue();

		Assertions.assertEquals(
			"PROJECT-ERC-1", placeholders.get("PROJECT_KEY"));
		Assertions.assertTrue(
			placeholders.get(
				"PROJECT_NAME_SUFFIX"
			).contains(
				"My Project"
			));
	}

	@Test
	public void testSendAssignedWelcomeEmailsIsolatesPerUserFailures()
		throws Exception {

		Account account = _createAccount(_ACCOUNT_ID);

		Mockito.when(
			_userAccountService.getUserAccount(_USER_ID)
		).thenThrow(
			new RuntimeException("Unable to fetch user account")
		);

		UserAccount secondUserAccount = _createVerifiedUserAccount(
			_SECOND_USER_ID);

		secondUserAccount.setEmailAddress("second@example.com");

		Mockito.when(
			_userAccountService.getUserAccount(_SECOND_USER_ID)
		).thenReturn(
			secondUserAccount
		);

		Mockito.when(
			_entitlementService.hasEntitlement(
				_ACCOUNT_ID, EntitlementConstants.NAMES_SLAS)
		).thenReturn(
			true
		);

		_provisioningEmailService.sendAssignedWelcomeEmails(
			account, List.of(_USER_ID, _SECOND_USER_ID));

		Mockito.verify(
			_notificationQueueEntryService
		).addNotificationQueueEntry(
			Mockito.any(), Mockito.any(), Mockito.eq("second@example.com"),
			Mockito.any(), Mockito.any()
		);
	}

	@Test
	public void testSendAssignedWelcomeEmailSkipsUnverifiedUser()
		throws Exception {

		Account account = _createAccount(_ACCOUNT_ID);

		UserAccount userAccount = new UserAccount();

		userAccount.setId(_USER_ID);

		Mockito.when(
			_userAccountService.getUserAccount(_USER_ID)
		).thenReturn(
			userAccount
		);

		_provisioningEmailService.sendAssignedWelcomeEmail(account, _USER_ID);

		Mockito.verifyNoInteractions(_entitlementService);
		Mockito.verifyNoInteractions(_notificationQueueEntryService);
	}

	@Test
	public void testSendAssignedWelcomeEmailSkipsWithoutSupportOrPartnerEntitlement()
		throws Exception {

		Account account = _createAccount(_ACCOUNT_ID);

		UserAccount userAccount = _createVerifiedUserAccount(_USER_ID);

		Mockito.when(
			_userAccountService.getUserAccount(_USER_ID)
		).thenReturn(
			userAccount
		);

		_provisioningEmailService.sendAssignedWelcomeEmail(account, _USER_ID);

		Mockito.verifyNoInteractions(_notificationQueueEntryService);
	}

	@Test
	public void testSendAssignedWelcomeEmailUsesPartnerEntitlement()
		throws Exception {

		Account account = _createAccount(_ACCOUNT_ID);

		UserAccount userAccount = _createVerifiedUserAccount(_USER_ID);

		Mockito.when(
			_userAccountService.getUserAccount(_USER_ID)
		).thenReturn(
			userAccount
		);

		Mockito.when(
			_entitlementService.hasEntitlement(
				_ACCOUNT_ID, EntitlementConstants.NAME_PARTNER)
		).thenReturn(
			true
		);

		_provisioningEmailService.sendAssignedWelcomeEmail(account, _USER_ID);

		Mockito.verify(
			_notificationQueueEntryService
		).addNotificationQueueEntry(
			Mockito.any(), Mockito.any(), Mockito.eq(_EMAIL_ADDRESS),
			Mockito.any(), Mockito.any()
		);
	}

	@Test
	public void testSendAssignedWelcomeEmailUsesSupportedLanguageId()
		throws Exception {

		Account account = _createAccount(_ACCOUNT_ID);

		UserAccount userAccount = _createVerifiedUserAccount(_USER_ID);

		userAccount.setLanguageId("pt_BR");

		Mockito.when(
			_userAccountService.getUserAccount(_USER_ID)
		).thenReturn(
			userAccount
		);

		Mockito.when(
			_entitlementService.hasEntitlement(
				_ACCOUNT_ID, EntitlementConstants.NAMES_SLAS)
		).thenReturn(
			true
		);

		_provisioningEmailService.sendAssignedWelcomeEmail(account, _USER_ID);

		Mockito.verify(
			_notificationTemplateService
		).getAndProcessTemplateJSONObject(
			Mockito.eq("PROVISIONING-WELCOME"), Mockito.eq("pt_BR"),
			Mockito.any()
		);
	}

	@Test
	public void testSendAssignedWelcomeEmailUsesUnitedStatesRegionAddress()
		throws Exception {

		Account account = _createAccount(_ACCOUNT_ID);

		UserAccount userAccount = _createVerifiedUserAccount(_USER_ID);

		Mockito.when(
			_userAccountService.getUserAccount(_USER_ID)
		).thenReturn(
			userAccount
		);

		Mockito.when(
			_entitlementService.hasEntitlement(
				_ACCOUNT_ID, EntitlementConstants.NAMES_SLAS)
		).thenReturn(
			true
		);

		_provisioningEmailService.sendAssignedWelcomeEmail(account, _USER_ID);

		Mockito.verify(
			_notificationQueueEntryService
		).addNotificationQueueEntry(
			_EMAIL_ADDRESS_US, "Liferay Provisioning", _EMAIL_ADDRESS,
			"Subject", "Body"
		);
	}

	@Test
	public void testSendAutoProvisionedWelcomeEmailSendsOnlyToEligibleMembers()
		throws Exception {

		Account account = _createAccount(_ACCOUNT_ID);

		UserAccount eligibleUserAccount = _createMemberUserAccount(
			_ACCOUNT_ID, _EMAIL_ADDRESS, _USER_ID,
			RoleConstants.NAME_ACCOUNT_MEMBER, true);
		UserAccount unverifiedUserAccount = _createMemberUserAccount(
			_ACCOUNT_ID, "unverified@example.com", _SECOND_USER_ID,
			RoleConstants.NAME_ACCOUNT_MEMBER, false);

		Mockito.when(
			_userAccountService.getAccountUserAccounts(_ACCOUNT_ID)
		).thenReturn(
			List.of(eligibleUserAccount, unverifiedUserAccount)
		);

		_provisioningEmailService.sendAutoProvisionedWelcomeEmail(account);

		Mockito.verify(
			_notificationQueueEntryService
		).addNotificationQueueEntry(
			Mockito.any(), Mockito.any(), Mockito.eq(_EMAIL_ADDRESS),
			Mockito.any(), Mockito.any()
		);

		Mockito.verify(
			_notificationQueueEntryService, Mockito.never()
		).addNotificationQueueEntry(
			Mockito.any(), Mockito.any(), Mockito.eq("unverified@example.com"),
			Mockito.any(), Mockito.any()
		);
	}

	@Test
	public void testSendPartnerUserUpdateEmailUsesGlobalRecipientTemplate()
		throws Exception {

		Account account = _createAccount(_ACCOUNT_ID);

		account.setName("Test Account");

		UserAccount userAccount = new UserAccount();

		userAccount.setEmailAddress(_EMAIL_ADDRESS);
		userAccount.setFamilyName("Doe");
		userAccount.setGivenName("Jane");

		_provisioningEmailService.sendPartnerUserUpdateEmail(
			account, userAccount, RoleConstants.NAME_PARTNER_MEMBER,
			"Assigned");

		ArgumentCaptor<Map<String, String>> placeholdersArgumentCaptor =
			ArgumentCaptor.forClass(Map.class);

		Mockito.verify(
			_notificationTemplateService
		).getAndProcessTemplateJSONObject(
			Mockito.eq("PROVISIONING-PARTNER-USER-UPDATE"), Mockito.any(),
			placeholdersArgumentCaptor.capture()
		);

		Map<String, String> placeholders =
			placeholdersArgumentCaptor.getValue();

		Assertions.assertTrue(placeholders.containsKey("ACCOUNT_NAME"));
		Assertions.assertTrue(placeholders.containsKey("ACCOUNT_ROLE"));
		Assertions.assertTrue(placeholders.containsKey("ACCOUNT_ROLE_ACTION"));
		Assertions.assertTrue(placeholders.containsKey("USER_EMAIL_ADDRESS"));
		Assertions.assertTrue(placeholders.containsKey("USER_FIRST_NAME"));
		Assertions.assertTrue(placeholders.containsKey("USER_LAST_NAME"));

		Mockito.verify(
			_notificationQueueEntryService
		).addNotificationQueueEntry(
			_EMAIL_ADDRESS_GLOBAL, "Liferay Provisioning",
			_PARTNER_USER_UPDATE_RECIPIENT, "Subject", "Body"
		);
	}

	@Test
	public void testSendVerifiedWelcomeEmailFallsBackToGlobalForMixedRegions()
		throws Exception {

		Account usAccount = _createAccount(_ACCOUNT_ID);
		Account brazilAccount = _createAccount(_SECOND_ACCOUNT_ID);

		Mockito.when(
			_accountService.fetchAccount(_ACCOUNT_ID)
		).thenReturn(
			usAccount
		);

		Mockito.when(
			_accountService.fetchAccount(_SECOND_ACCOUNT_ID)
		).thenReturn(
			brazilAccount
		);

		Mockito.when(
			_commerceOrderService.getAccountSupportInfo(
				_SECOND_ACCOUNT_ID, null)
		).thenReturn(
			new AccountSupportInfo("pt_BR", SupportRegionConstants.BRAZIL)
		);

		Mockito.when(
			_entitlementService.hasEntitlement(
				_ACCOUNT_ID, EntitlementConstants.NAMES_SLAS)
		).thenReturn(
			true
		);

		Mockito.when(
			_entitlementService.hasEntitlement(
				_SECOND_ACCOUNT_ID, EntitlementConstants.NAMES_SLAS)
		).thenReturn(
			true
		);

		UserAccount userAccount = _createUserAccountWithAccountBriefs(
			_EMAIL_ADDRESS, _USER_ID, RoleConstants.NAME_ACCOUNT_MEMBER,
			_ACCOUNT_ID, _SECOND_ACCOUNT_ID);

		_provisioningEmailService.sendVerifiedWelcomeEmail(userAccount);

		Mockito.verify(
			_notificationQueueEntryService
		).addNotificationQueueEntry(
			_EMAIL_ADDRESS_GLOBAL, "Liferay Provisioning", _EMAIL_ADDRESS,
			"Subject", "Body"
		);
	}

	@Test
	public void testSendVerifiedWelcomeEmailIncludesInvitationForMultipleProjects()
		throws Exception {

		Mockito.when(
			_accountService.fetchAccount(_ACCOUNT_ID)
		).thenReturn(
			_createAccount(_ACCOUNT_ID)
		);

		Mockito.when(
			_entitlementService.hasEntitlement(
				_ACCOUNT_ID, EntitlementConstants.NAMES_SLAS)
		).thenReturn(
			true
		);

		Mockito.when(
			_projectMembershipService.getProjectMemberships(
				_ACCOUNT_ID, _USER_ID)
		).thenReturn(
			List.of(
				_createProjectMembership("PROJECT-ERC-1"),
				_createProjectMembership("PROJECT-ERC-2"))
		);

		Mockito.when(
			_projectService.fetchProject("PROJECT-ERC-1")
		).thenReturn(
			_createProject("PROJECT-ERC-1", "First Project")
		);

		Mockito.when(
			_projectService.fetchProject("PROJECT-ERC-2")
		).thenReturn(
			_createProject("PROJECT-ERC-2", "Second Project")
		);

		UserAccount userAccount = _createUserAccountWithAccountBriefs(
			_EMAIL_ADDRESS, _USER_ID, RoleConstants.NAME_ACCOUNT_MEMBER,
			_ACCOUNT_ID);

		_provisioningEmailService.sendVerifiedWelcomeEmail(userAccount);

		ArgumentCaptor<Map<String, String>> placeholdersArgumentCaptor =
			ArgumentCaptor.forClass(Map.class);

		Mockito.verify(
			_notificationTemplateService
		).getAndProcessTemplateJSONObject(
			Mockito.eq("PROVISIONING-WELCOME"), Mockito.any(),
			placeholdersArgumentCaptor.capture()
		);

		Map<String, String> placeholders =
			placeholdersArgumentCaptor.getValue();

		Assertions.assertEquals("", placeholders.get("PROJECT_KEY"));
		Assertions.assertEquals("", placeholders.get("PROJECT_NAME_SUFFIX"));
		Assertions.assertTrue(
			placeholders.get(
				"PROJECT_INVITATION_MESSAGE"
			).contains(
				"First Project"
			));
		Assertions.assertTrue(
			placeholders.get(
				"PROJECT_INVITATION_MESSAGE"
			).contains(
				"Second Project"
			));
	}

	@Test
	public void testSendVerifiedWelcomeEmailIncludesPartnerAccount()
		throws Exception {

		Mockito.when(
			_accountService.fetchAccount(_ACCOUNT_ID)
		).thenReturn(
			_createAccount(_ACCOUNT_ID)
		);

		Mockito.when(
			_entitlementService.hasEntitlement(
				_ACCOUNT_ID, EntitlementConstants.NAME_PARTNER)
		).thenReturn(
			true
		);

		UserAccount userAccount = _createUserAccountWithAccountBriefs(
			_EMAIL_ADDRESS, _USER_ID, RoleConstants.NAME_PARTNER_MEMBER,
			_ACCOUNT_ID);

		_provisioningEmailService.sendVerifiedWelcomeEmail(userAccount);

		Mockito.verify(
			_accountService
		).fetchAccount(
			_ACCOUNT_ID
		);

		Mockito.verify(
			_notificationQueueEntryService
		).addNotificationQueueEntry(
			Mockito.any(), Mockito.any(), Mockito.eq(_EMAIL_ADDRESS),
			Mockito.any(), Mockito.any()
		);
	}

	@Test
	public void testSendVerifiedWelcomeEmailSkipsWhenNoEligibleAccounts()
		throws Exception {

		UserAccount userAccount = new UserAccount();

		userAccount.setEmailAddress(_EMAIL_ADDRESS);
		userAccount.setId(_USER_ID);

		_provisioningEmailService.sendVerifiedWelcomeEmail(userAccount);

		Mockito.verifyNoInteractions(_notificationQueueEntryService);
	}

	@Test
	public void testSendWelcomeEmailsRoutesExistingBusinessToAssigned()
		throws Exception {

		Account account = _createAccount(_ACCOUNT_ID);

		UserAccount userAccount = _createVerifiedUserAccount(_USER_ID);

		Mockito.when(
			_userAccountService.getUserAccount(_USER_ID)
		).thenReturn(
			userAccount
		);

		Mockito.when(
			_entitlementService.hasEntitlement(
				_ACCOUNT_ID, EntitlementConstants.NAMES_SLAS)
		).thenReturn(
			true
		);

		_provisioningEmailService.sendWelcomeEmails(
			account, OpportunityConstants.TYPE_EXISTING_BUSINESS,
			List.of(_USER_ID));

		Mockito.verify(
			_provisioningEmailService
		).sendAssignedWelcomeEmails(
			account, List.of(_USER_ID)
		);

		Mockito.verify(
			_notificationQueueEntryService
		).addNotificationQueueEntry(
			_EMAIL_ADDRESS_US, "Liferay Provisioning", _EMAIL_ADDRESS,
			"Subject", "Body"
		);
	}

	@Test
	public void testSendWelcomeEmailsRoutesNewBusinessToAutoProvisioned()
		throws Exception {

		Account account = _createAccount(_ACCOUNT_ID);

		UserAccount eligibleUserAccount = _createMemberUserAccount(
			_ACCOUNT_ID, _EMAIL_ADDRESS, _USER_ID,
			RoleConstants.NAME_ACCOUNT_MEMBER, true);

		Mockito.when(
			_userAccountService.getAccountUserAccounts(_ACCOUNT_ID)
		).thenReturn(
			List.of(eligibleUserAccount)
		);

		_provisioningEmailService.sendWelcomeEmails(
			account, OpportunityConstants.TYPE_NEW_BUSINESS, List.of());

		Mockito.verify(
			_provisioningEmailService
		).sendAutoProvisionedWelcomeEmail(
			account
		);

		Mockito.verify(
			_notificationQueueEntryService
		).addNotificationQueueEntry(
			Mockito.any(), Mockito.any(), Mockito.eq(_EMAIL_ADDRESS),
			Mockito.any(), Mockito.any()
		);
	}

	@Test
	public void testSendWelcomeEmailsRoutesNewProjectExistingBusinessToAutoProvisioned()
		throws Exception {

		Account account = _createAccount(_ACCOUNT_ID);

		UserAccount eligibleUserAccount = _createMemberUserAccount(
			_ACCOUNT_ID, _EMAIL_ADDRESS, _USER_ID,
			RoleConstants.NAME_ACCOUNT_MEMBER, true);

		Mockito.when(
			_userAccountService.getAccountUserAccounts(_ACCOUNT_ID)
		).thenReturn(
			List.of(eligibleUserAccount)
		);

		_provisioningEmailService.sendWelcomeEmails(
			account, OpportunityConstants.TYPE_NEW_PROJECT_EXISTING_BUSINESS,
			List.of());

		Mockito.verify(
			_provisioningEmailService
		).sendAutoProvisionedWelcomeEmail(
			account
		);

		Mockito.verify(
			_notificationQueueEntryService
		).addNotificationQueueEntry(
			Mockito.any(), Mockito.any(), Mockito.eq(_EMAIL_ADDRESS),
			Mockito.any(), Mockito.any()
		);
	}

	@Test
	public void testSendWelcomeEmailsRoutesRenewalToNoOp() throws Exception {
		Account account = _createAccount(_ACCOUNT_ID);

		_provisioningEmailService.sendWelcomeEmails(
			account, OpportunityConstants.TYPE_RENEWAL, List.of());

		Mockito.verify(
			_provisioningEmailService, Mockito.never()
		).sendAutoProvisionedWelcomeEmail(
			Mockito.any()
		);

		Mockito.verify(
			_provisioningEmailService, Mockito.never()
		).sendAssignedWelcomeEmails(
			Mockito.any(), Mockito.any()
		);

		Mockito.verifyNoInteractions(_notificationQueueEntryService);
	}

	@Test
	public void testSendWelcomeEmailsSwallowsAutoProvisionedFailure()
		throws Exception {

		Account account = _createAccount(_ACCOUNT_ID);

		Mockito.when(
			_userAccountService.getAccountUserAccounts(_ACCOUNT_ID)
		).thenThrow(
			new RuntimeException("Unable to fetch account user accounts")
		);

		Assertions.assertDoesNotThrow(
			() -> _provisioningEmailService.sendWelcomeEmails(
				account, OpportunityConstants.TYPE_NEW_BUSINESS, List.of()));

		Mockito.verifyNoInteractions(_notificationQueueEntryService);
	}

	private Account _createAccount(long id) {
		Account account = new Account();

		account.setId(id);

		return account;
	}

	private UserAccount _createMemberUserAccount(
		long accountId, String emailAddress, long id, String roleName,
		boolean verified) {

		RoleBrief roleBrief = new RoleBrief();

		roleBrief.setName(roleName);

		AccountBrief accountBrief = new AccountBrief();

		accountBrief.setId(accountId);
		accountBrief.setRoleBriefs(new RoleBrief[] {roleBrief});

		UserAccount userAccount = new UserAccount();

		userAccount.setAccountBriefs(new AccountBrief[] {accountBrief});
		userAccount.setEmailAddress(emailAddress);
		userAccount.setId(id);

		if (verified) {
			CustomValue customValue = new CustomValue();

			customValue.setData(true);

			CustomField customField = new CustomField();

			customField.setCustomValue(customValue);
			customField.setName("verified");

			userAccount.setCustomFields(new CustomField[] {customField});
		}

		return userAccount;
	}

	private Project _createProject(String externalReferenceCode, String name) {
		return new Project(
			new JSONObject(
			).put(
				"externalReferenceCode", externalReferenceCode
			).put(
				"name", name
			));
	}

	private ProjectMembership _createProjectMembership(
		String projectExternalReferenceCode) {

		return new ProjectMembership(
			new JSONObject(
			).put(
				"r_projectToProjectMembership_c_projectERC",
				projectExternalReferenceCode
			));
	}

	private UserAccount _createUserAccountWithAccountBriefs(
		String emailAddress, long id, String roleName, long... accountIds) {

		AccountBrief[] accountBriefs = new AccountBrief[accountIds.length];

		for (int i = 0; i < accountIds.length; i++) {
			RoleBrief roleBrief = new RoleBrief();

			roleBrief.setName(roleName);

			AccountBrief accountBrief = new AccountBrief();

			accountBrief.setId(accountIds[i]);
			accountBrief.setRoleBriefs(new RoleBrief[] {roleBrief});

			accountBriefs[i] = accountBrief;
		}

		UserAccount userAccount = _createVerifiedUserAccount(id);

		userAccount.setAccountBriefs(accountBriefs);
		userAccount.setEmailAddress(emailAddress);

		return userAccount;
	}

	private UserAccount _createVerifiedUserAccount(long id) {
		UserAccount userAccount = new UserAccount();

		userAccount.setEmailAddress(_EMAIL_ADDRESS);
		userAccount.setId(id);

		CustomValue customValue = new CustomValue();

		customValue.setData(true);

		CustomField customField = new CustomField();

		customField.setCustomValue(customValue);
		customField.setName("verified");

		userAccount.setCustomFields(new CustomField[] {customField});

		return userAccount;
	}

	private static final long _ACCOUNT_ID = 1000L;

	private static final String _EMAIL_ADDRESS = "contact@example.com";

	private static final String _EMAIL_ADDRESS_GLOBAL = "global@example.com";

	private static final String _EMAIL_ADDRESS_US = "us@example.com";

	private static final String _PARTNER_USER_UPDATE_RECIPIENT =
		"partner-update@example.com";

	private static final long _SECOND_ACCOUNT_ID = 2000L;

	private static final long _SECOND_USER_ID = 200L;

	private static final long _USER_ID = 100L;

	private AccountService _accountService;
	private CommerceOrderService _commerceOrderService;
	private EntitlementService _entitlementService;
	private MessageSource _messageSource;
	private NotificationQueueEntryService _notificationQueueEntryService;
	private NotificationTemplateService _notificationTemplateService;
	private ProjectMembershipService _projectMembershipService;
	private ProjectService _projectService;
	private ProvisioningEmailService _provisioningEmailService;
	private UserAccountService _userAccountService;

}