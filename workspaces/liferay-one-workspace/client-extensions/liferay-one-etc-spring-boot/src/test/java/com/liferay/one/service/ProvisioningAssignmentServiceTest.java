/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.admin.user.client.dto.v1_0.Account;
import com.liferay.headless.admin.user.client.dto.v1_0.AccountBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.RoleBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.one.constants.PropertyConstants;
import com.liferay.one.constants.RoleConstants;
import com.liferay.one.okta.service.OktaService;
import com.liferay.portal.kernel.workflow.WorkflowConstants;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Felipe Franca
 */
public class ProvisioningAssignmentServiceTest {

	@BeforeEach
	public void setUp() throws Exception {
		_provisioningAssignmentService = new ProvisioningAssignmentService();

		_oktaService = Mockito.mock(OktaService.class);
		_propertyService = Mockito.mock(PropertyService.class);
		_provisioningEmailService = Mockito.mock(
			ProvisioningEmailService.class);
		_subscriptionEntryService = Mockito.mock(
			SubscriptionEntryService.class);
		_userAccountService = Mockito.mock(UserAccountService.class);

		_account = new Account();

		_account.setId(_ACCOUNT_ID);

		_userAccount = new UserAccount();

		_userAccount.setEmailAddress(_EMAIL_ADDRESS);
		_userAccount.setId(_USER_ID);

		Mockito.when(
			_userAccountService.getUserAccount(_USER_ID)
		).thenReturn(
			_userAccount
		);

		Mockito.when(
			_oktaService.fetchContactStatusByEmailAddress(_EMAIL_ADDRESS)
		).thenReturn(
			WorkflowConstants.STATUS_APPROVED
		);

		ReflectionTestUtils.setField(
			_provisioningAssignmentService, "_oktaService", _oktaService);
		ReflectionTestUtils.setField(
			_provisioningAssignmentService, "_propertyService",
			_propertyService);
		ReflectionTestUtils.setField(
			_provisioningAssignmentService, "_provisioningEmailService",
			_provisioningEmailService);
		ReflectionTestUtils.setField(
			_provisioningAssignmentService, "_subscriptionEntryService",
			_subscriptionEntryService);
		ReflectionTestUtils.setField(
			_provisioningAssignmentService, "_userAccountService",
			_userAccountService);
	}

	@Test
	public void testAssignAccountRoleActivatesInactiveContact()
		throws Exception {

		Mockito.when(
			_oktaService.fetchContactStatusByEmailAddress(_EMAIL_ADDRESS)
		).thenReturn(
			WorkflowConstants.STATUS_INACTIVE
		);

		_provisioningAssignmentService.assignAccountRole(
			_account, _USER_ID, RoleConstants.NAME_ACCOUNT_MEMBER);

		Mockito.verify(
			_oktaService
		).activateUser(
			_EMAIL_ADDRESS
		);

		Mockito.verify(
			_oktaService, Mockito.never()
		).addMembership(
			Mockito.any(), Mockito.any()
		);
	}

	@Test
	public void testAssignAccountRoleAddsCustomerMembership() throws Exception {
		_provisioningAssignmentService.assignAccountRole(
			_account, _USER_ID, RoleConstants.NAME_ACCOUNT_MEMBER);

		Mockito.verify(
			_oktaService
		).addMembership(
			"Customers", _EMAIL_ADDRESS
		);

		Mockito.verify(
			_oktaService, Mockito.never()
		).addMembership(
			Mockito.eq("Partners"), Mockito.any()
		);

		Mockito.verifyNoInteractions(_provisioningEmailService);
	}

	@Test
	public void testAssignAccountRoleAddsPartnerMembershipAndSendsEmail()
		throws Exception {

		_provisioningAssignmentService.assignAccountRole(
			_account, _USER_ID, RoleConstants.NAME_PARTNER_MEMBER);

		Mockito.verify(
			_oktaService
		).addMembership(
			"Customers", _EMAIL_ADDRESS
		);

		Mockito.verify(
			_oktaService
		).addMembership(
			"Partners", _EMAIL_ADDRESS
		);

		Mockito.verify(
			_provisioningEmailService
		).sendPartnerUserUpdateEmail(
			_account, _userAccount, RoleConstants.NAME_PARTNER_MEMBER,
			"Assigned"
		);
	}

	@Test
	public void testAssignAccountRoleAssignsCloudNativeContactToApplication()
		throws Exception {

		Mockito.when(
			_propertyService.getPropertyValue(
				_ACCOUNT_ID, PropertyConstants.NAME_OKTA_APPLICATION)
		).thenReturn(
			_OKTA_APPLICATION_ID
		);

		_provisioningAssignmentService.assignAccountRole(
			_account, _USER_ID, RoleConstants.NAME_CLOUD_NATIVE_CONTACT);

		Mockito.verify(
			_oktaService
		).assignUserToApplication(
			_OKTA_APPLICATION_ID, _EMAIL_ADDRESS
		);
	}

	@Test
	public void testAssignAccountRoleSkipsCloudNativeContactWithoutApplication()
		throws Exception {

		_provisioningAssignmentService.assignAccountRole(
			_account, _USER_ID, RoleConstants.NAME_CLOUD_NATIVE_CONTACT);

		Mockito.verify(
			_oktaService, Mockito.never()
		).assignUserToApplication(
			Mockito.any(), Mockito.any()
		);
	}

	@Test
	public void testAssignAccountRoleSkipsMembershipWhenContactStatusIsNull()
		throws Exception {

		Mockito.when(
			_oktaService.fetchContactStatusByEmailAddress(_EMAIL_ADDRESS)
		).thenReturn(
			null
		);

		_provisioningAssignmentService.assignAccountRole(
			_account, _USER_ID, RoleConstants.NAME_ACCOUNT_MEMBER);

		Mockito.verify(
			_oktaService, Mockito.never()
		).addMembership(
			Mockito.any(), Mockito.any()
		);

		Mockito.verify(
			_oktaService, Mockito.never()
		).activateUser(
			Mockito.any()
		);
	}

	@Test
	public void testAssignCustomerGroupAddsCustomerMembership()
		throws Exception {

		_provisioningAssignmentService.assignCustomerGroup(_USER_ID);

		Mockito.verify(
			_oktaService
		).addMembership(
			"Customers", _EMAIL_ADDRESS
		);
	}

	@Test
	public void testUnassignAccountMembershipAlwaysDeletesSubscriptionEntries()
		throws Exception {

		_provisioningAssignmentService.unassignAccountMembership(
			_ACCOUNT_ID, _USER_ID);

		Mockito.verify(
			_subscriptionEntryService
		).deleteAccountLicenseKeySubscriptionEntries(
			_ACCOUNT_ID, _USER_ID
		);
	}

	@Test
	public void testUnassignAccountMembershipKeepsCustomerMembershipWhenStillAMember()
		throws Exception {

		AccountBrief accountBrief = new AccountBrief();

		accountBrief.setId(_ACCOUNT_ID);

		_userAccount.setAccountBriefs(new AccountBrief[] {accountBrief});

		_provisioningAssignmentService.unassignAccountMembership(
			_ACCOUNT_ID, _USER_ID);

		Mockito.verify(
			_oktaService, Mockito.never()
		).removeMembership(
			Mockito.eq("Customers"), Mockito.any()
		);
	}

	@Test
	public void testUnassignAccountMembershipRemovesCustomerMembershipWhenNoAccountBriefs()
		throws Exception {

		_provisioningAssignmentService.unassignAccountMembership(
			_ACCOUNT_ID, _USER_ID);

		Mockito.verify(
			_oktaService
		).removeMembership(
			"Customers", _EMAIL_ADDRESS
		);
	}

	@Test
	public void testUnassignAccountMembershipRemovesPartnerMembershipWhenNoPartnerRoleRemains()
		throws Exception {

		_provisioningAssignmentService.unassignAccountMembership(
			_ACCOUNT_ID, _USER_ID);

		Mockito.verify(
			_oktaService
		).removeMembership(
			"Partners", _EMAIL_ADDRESS
		);
	}

	@Test
	public void testUnassignAccountRoleRemovesPartnerMembershipAndSendsEmail()
		throws Exception {

		_provisioningAssignmentService.unassignAccountRole(
			_account, _USER_ID, RoleConstants.NAME_PARTNER_MEMBER);

		Mockito.verify(
			_oktaService
		).removeMembership(
			"Partners", _EMAIL_ADDRESS
		);

		Mockito.verify(
			_provisioningEmailService
		).sendPartnerUserUpdateEmail(
			_account, _userAccount, RoleConstants.NAME_PARTNER_MEMBER,
			"Unassigned"
		);
	}

	@Test
	public void testUnassignAccountRoleSkipsRemovalWhenStillPartnerElsewhere()
		throws Exception {

		RoleBrief partnerRoleBrief = new RoleBrief();

		partnerRoleBrief.setName(RoleConstants.NAME_PARTNER_MANAGER);

		AccountBrief accountBrief = new AccountBrief();

		accountBrief.setId(_ACCOUNT_ID + 1);
		accountBrief.setRoleBriefs(new RoleBrief[] {partnerRoleBrief});

		_userAccount.setAccountBriefs(new AccountBrief[] {accountBrief});

		_provisioningAssignmentService.unassignAccountRole(
			_account, _USER_ID, RoleConstants.NAME_PARTNER_MEMBER);

		Mockito.verify(
			_oktaService, Mockito.never()
		).removeMembership(
			Mockito.eq("Partners"), Mockito.any()
		);
	}

	@Test
	public void testUnassignAccountRoleUnassignsCloudNativeContactFromApplication()
		throws Exception {

		Mockito.when(
			_propertyService.getPropertyValue(
				_ACCOUNT_ID, PropertyConstants.NAME_OKTA_APPLICATION)
		).thenReturn(
			_OKTA_APPLICATION_ID
		);

		_provisioningAssignmentService.unassignAccountRole(
			_account, _USER_ID, RoleConstants.NAME_CLOUD_NATIVE_CONTACT);

		Mockito.verify(
			_oktaService
		).unassignUserFromApplication(
			_OKTA_APPLICATION_ID, _EMAIL_ADDRESS
		);
	}

	private static final long _ACCOUNT_ID = 1000L;

	private static final String _EMAIL_ADDRESS = "contact@example.com";

	private static final String _OKTA_APPLICATION_ID = "APP-1";

	private static final long _USER_ID = 100L;

	private Account _account;
	private OktaService _oktaService;
	private PropertyService _propertyService;
	private ProvisioningAssignmentService _provisioningAssignmentService;
	private ProvisioningEmailService _provisioningEmailService;
	private SubscriptionEntryService _subscriptionEntryService;
	private UserAccount _userAccount;
	private UserAccountService _userAccountService;

}