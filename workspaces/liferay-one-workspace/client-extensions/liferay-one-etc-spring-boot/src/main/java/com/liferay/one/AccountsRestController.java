/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.liferay.headless.admin.user.client.dto.v1_0.Account;
import com.liferay.headless.admin.user.client.dto.v1_0.AccountBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.RoleBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.one.constants.EntitlementConstants;
import com.liferay.one.jira.service.AccountAssetService;
import com.liferay.one.jira.synchronizer.AccountSynchronizer;
import com.liferay.one.jira.synchronizer.AccountUserAccountRoleSynchronizer;
import com.liferay.one.jira.synchronizer.AccountUserAccountSynchronizer;
import com.liferay.one.model.AccountInvitation;
import com.liferay.one.okta.service.OktaService;
import com.liferay.one.permission.AccountPermission;
import com.liferay.one.permission.AdminPermission;
import com.liferay.one.service.AccountInvitationEmailService;
import com.liferay.one.service.AccountInvitationService;
import com.liferay.one.service.AccountService;
import com.liferay.one.service.EmailAddressValidatorService;
import com.liferay.one.service.EntitlementService;
import com.liferay.one.service.ProvisioningAssignmentService;
import com.liferay.one.service.ProvisioningEmailService;
import com.liferay.one.service.UserAccountService;
import com.liferay.one.util.FindUtil;
import com.liferay.one.util.UserAccountUtil;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author Jenny Chen
 */
@RequestMapping("/accounts")
@RestController
public class AccountsRestController extends OneBaseRestController {

	@DeleteMapping("/{externalReferenceCode}/user-accounts/{userId}")
	public void deleteUserAccounts(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("externalReferenceCode") String externalReferenceCode,
			@PathVariable("userId") long userId)
		throws Exception {

		Account account = _accountService.getAccount(
			externalReferenceCode, jwt);

		UserAccount userAccount = _userAccountService.getUserAccount(userId);

		_accountService.removeAccountUserAccount(
			externalReferenceCode, jwt, userId);

		_unassignContactRoles(account, userAccount);

		_syncMembership(account, userId);

		_provisioningAssignmentService.unassignAccountMembership(
			account.getId(), userId);
	}

	@DeleteMapping(
		"/{externalReferenceCode}/user-accounts/{userId}/account-roles" +
			"/{accountRoleId}"
	)
	public void deleteUserAccountsAccountRole(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("externalReferenceCode") String externalReferenceCode,
			@PathVariable("userId") long userId,
			@PathVariable("accountRoleId") long accountRoleId)
		throws Exception {

		_accountPermission.check(externalReferenceCode, ActionKeys.UPDATE, jwt);

		Account account = _accountService.getAccount(
			externalReferenceCode, jwt);

		String accountRoleName = _accountService.getAccountRoleName(
			account.getId(), accountRoleId);

		_accountService.removeAccountUserAccountRole(
			accountRoleId, externalReferenceCode, jwt, userId);

		_unassignContactRole(account, accountRoleId, userId);

		_syncMembership(account, userId);

		if (accountRoleName != null) {
			_provisioningAssignmentService.unassignAccountRole(
				account, userId, accountRoleName);
		}
	}

	@GetMapping("/{externalReferenceCode}/jira/object-key")
	public ResponseEntity<String> getJiraObjectKey(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("externalReferenceCode") String externalReferenceCode)
		throws Exception {

		_accountPermission.check(externalReferenceCode, ActionKeys.VIEW, jwt);

		return new ResponseEntity<>(
			_accountAssetService.getAccountObjectKey(externalReferenceCode),
			HttpStatus.OK);
	}

	@PostMapping("/{externalReferenceCode}/invitations")
	public void postInvitations(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("externalReferenceCode") String externalReferenceCode,
			@RequestBody String json)
		throws Exception {

		_accountPermission.check(externalReferenceCode, ActionKeys.UPDATE, jwt);

		JSONObject jsonObject = null;

		try {
			jsonObject = new JSONObject(json);
		}
		catch (JSONException jsonException) {
			throw new ResponseStatusException(
				HttpStatus.BAD_REQUEST, "Request body is not valid JSON",
				jsonException);
		}

		String emailAddress = jsonObject.optString("emailAddress");
		String familyName = jsonObject.optString("familyName");
		String givenName = jsonObject.optString("givenName");

		if (Validator.isNull(emailAddress) || Validator.isNull(familyName) ||
			Validator.isNull(givenName)) {

			throw new ResponseStatusException(
				HttpStatus.BAD_REQUEST,
				"\"emailAddress\", \"familyName\", and \"givenName\" are " +
					"required");
		}

		if (!Validator.isEmailAddress(emailAddress)) {
			throw new ResponseStatusException(
				HttpStatus.BAD_REQUEST, "Email address is not valid");
		}

		if (_emailAddressValidatorService.isLiferayDomain(emailAddress)) {
			throw new ResponseStatusException(
				HttpStatus.BAD_REQUEST,
				"Email address uses a reserved Liferay domain");
		}

		Account account = _accountService.getAccount(
			externalReferenceCode, jwt);

		UserAccount userAccount =
			_userAccountService.fetchUserAccountByEmailAddress(emailAddress);

		if ((userAccount != null) &&
			UserAccountUtil.hasAccountMembership(
				userAccount, account.getId())) {

			throw new ResponseStatusException(
				HttpStatus.CONFLICT,
				"The user is already a member of this account");
		}

		List<String> roleNames = _getRoleNames(
			account.getId(), jsonObject.optJSONArray("roleNames"));

		UserAccount inviterUserAccount = getMyUserAccount(jwt);

		AccountInvitation accountInvitation =
			_accountInvitationService.fetchPendingAccountInvitation(
				externalReferenceCode, emailAddress);

		if (accountInvitation == null) {
			accountInvitation = _accountInvitationService.addAccountInvitation(
				externalReferenceCode, emailAddress, familyName, givenName,
				roleNames);
		}
		else {
			accountInvitation =
				_accountInvitationService.updateAccountInvitation(
					accountInvitation.getAccountInvitationId(), familyName,
					givenName, roleNames);
		}

		_accountInvitationEmailService.sendInvitationEmail(
			account, accountInvitation, inviterUserAccount.getName());
	}

	@PostMapping("/{externalReferenceCode}/sync-to-jsm")
	public ResponseEntity<Void> postSyncToJSM(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("externalReferenceCode") String externalReferenceCode)
		throws Exception {

		_adminPermission.check(jwt);

		_accountSynchronizer.syncAccount(
			_accountService.getAccount(externalReferenceCode));

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping("/{externalReferenceCode}/user-accounts/{userId}")
	public void postUserAccounts(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("externalReferenceCode") String externalReferenceCode,
			@PathVariable("userId") long userId)
		throws Exception {

		Account account = _accountService.getAccount(
			externalReferenceCode, jwt);

		boolean hasAccount = _userAccountService.hasAccountUserAccount(
			account.getId(), userId);

		_accountService.addAccountUserAccount(account.getId(), jwt, userId);

		_syncMembership(account, userId);

		_provisioningAssignmentService.assignCustomerGroup(userId);

		if (!hasAccount) {
			_provisioningEmailService.sendAssignedWelcomeEmail(account, userId);
		}
	}

	@PostMapping(
		"/{externalReferenceCode}/user-accounts/{userId}/account-roles" +
			"/{accountRoleId}"
	)
	public void postUserAccountsAccountRole(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("externalReferenceCode") String externalReferenceCode,
			@PathVariable("userId") long userId,
			@PathVariable("accountRoleId") long accountRoleId)
		throws Exception {

		Account account = _accountService.getAccount(
			externalReferenceCode, jwt);

		_accountService.addAccountUserAccountRole(
			accountRoleId, externalReferenceCode, jwt, userId);

		_syncMembership(account, userId);

		String accountRoleName = _accountService.getAccountRoleName(
			account.getId(), accountRoleId);

		if (accountRoleName != null) {
			_provisioningAssignmentService.assignAccountRole(
				account, userId, accountRoleName);
		}
	}

	@PostMapping(
		"/{externalReferenceCode}/user-accounts/by-email-address" +
			"/{emailAddress}/account-roles"
	)
	public void postUserAccountsByEmailAddressAccountRoles(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("externalReferenceCode") String externalReferenceCode,
			@PathVariable("emailAddress") String emailAddress,
			@RequestBody String json)
		throws Exception {

		_accountPermission.check(externalReferenceCode, ActionKeys.UPDATE, jwt);

		if (_emailAddressValidatorService.isLiferayDomain(emailAddress)) {
			throw new ResponseStatusException(
				HttpStatus.BAD_REQUEST,
				"Email address uses a reserved Liferay domain");
		}

		JSONObject jsonObject = null;

		try {
			jsonObject = new JSONObject(json);
		}
		catch (JSONException jsonException) {
			throw new ResponseStatusException(
				HttpStatus.BAD_REQUEST, "Request body is not valid JSON",
				jsonException);
		}

		Account account = _accountService.getAccount(
			externalReferenceCode, jwt);

		Map<Long, String> accountRoleNames = _getAccountRoleNames(
			account.getId(), jsonObject.optJSONArray("accountRoleIds"));

		UserAccount userAccount =
			_userAccountService.fetchUserAccountByEmailAddress(emailAddress);

		if (userAccount != null) {
			Set<String> currentAccountRoleNames =
				UserAccountUtil.getAccountRoleNames(
					userAccount, account.getId());

			for (String accountRoleName : accountRoleNames.values()) {
				if (currentAccountRoleNames.contains(accountRoleName)) {
					throw new ResponseStatusException(
						HttpStatus.CONFLICT,
						"Account role " + accountRoleName +
							" is already assigned");
				}
			}
		}

		if (_oktaService.fetchContactByEmailAddress(emailAddress) == null) {
			_createOktaContact(account, emailAddress, jsonObject);
		}

		boolean hasAccount = false;

		if ((userAccount != null) &&
			UserAccountUtil.hasAccountMembership(
				userAccount, account.getId())) {

			hasAccount = true;
		}

		if (!hasAccount) {
			_accountService.addAccountUserAccountByEmailAddress(
				account.getId(), emailAddress, jwt);
		}

		if (userAccount == null) {
			userAccount = _userAccountService.fetchUserAccountByEmailAddress(
				emailAddress);
		}

		long userId = userAccount.getId();

		for (Map.Entry<Long, String> entry : accountRoleNames.entrySet()) {
			_accountService.addAccountUserAccountRole(
				entry.getKey(), externalReferenceCode, jwt, userId);
		}

		_syncMembership(account, userId);

		if (accountRoleNames.isEmpty()) {
			_provisioningAssignmentService.assignCustomerGroup(userId);
		}

		for (Map.Entry<Long, String> entry : accountRoleNames.entrySet()) {
			_provisioningAssignmentService.assignAccountRole(
				account, userId, entry.getValue());
		}

		if (!hasAccount) {
			_provisioningEmailService.sendAssignedWelcomeEmail(account, userId);
		}
	}

	private void _createOktaContact(
			Account account, String emailAddress, JSONObject jsonObject)
		throws Exception {

		if (!_entitlementService.hasEntitlement(
				account.getId(),
				ArrayUtil.append(
					EntitlementConstants.NAMES_SLAS,
					EntitlementConstants.NAME_PARTNER))) {

			throw new ResponseStatusException(
				HttpStatus.UNPROCESSABLE_ENTITY,
				"Unable to create an Okta user for an account without " +
					"support or partner entitlements");
		}

		String firstName = jsonObject.optString("firstName");
		String lastName = jsonObject.optString("lastName");

		if (Validator.isNull(firstName) || Validator.isNull(lastName)) {
			throw new ResponseStatusException(
				HttpStatus.BAD_REQUEST,
				"\"firstName\" and \"lastName\" are required to create a new " +
					"Okta user");
		}

		_oktaService.createContact(
			emailAddress, firstName, StringPool.BLANK, lastName);
	}

	private Map<Long, String> _getAccountRoleNames(
			long accountId, JSONArray accountRoleIdsJSONArray)
		throws Exception {

		Map<Long, String> accountRoleNames = new LinkedHashMap<>();

		if (accountRoleIdsJSONArray == null) {
			return accountRoleNames;
		}

		Map<Long, String> allAccountRoleNames =
			_accountService.getAccountRoleNames(accountId);

		for (int i = 0; i < accountRoleIdsJSONArray.length(); i++) {
			long accountRoleId = accountRoleIdsJSONArray.getLong(i);

			String accountRoleName = allAccountRoleNames.get(accountRoleId);

			if (accountRoleName == null) {
				throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"Unable to find account role " + accountRoleId);
			}

			accountRoleNames.put(accountRoleId, accountRoleName);
		}

		return accountRoleNames;
	}

	private List<String> _getRoleNames(
			long accountId, JSONArray roleNamesJSONArray)
		throws Exception {

		List<String> roleNames = new ArrayList<>();

		if ((roleNamesJSONArray == null) ||
			(roleNamesJSONArray.length() == 0)) {

			return roleNames;
		}

		Map<Long, String> allAccountRoleNames =
			_accountService.getAccountRoleNames(accountId);

		for (int i = 0; i < roleNamesJSONArray.length(); i++) {
			String roleName = roleNamesJSONArray.getString(i);

			if (!allAccountRoleNames.containsValue(roleName)) {
				throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"Unable to find account role " + roleName);
			}

			roleNames.add(roleName);
		}

		return roleNames;
	}

	private void _syncMembership(Account account, long userId) {
		try {
			_accountUserAccountSynchronizer.syncAccountUserAccountMembership(
				account, _userAccountService.getUserAccount(userId));
		}
		catch (Exception exception) {
			_log.error(
				"Unable to sync membership for user " + userId, exception);
		}
	}

	private void _unassignContactRole(
		Account account, long accountRoleId, long userId) {

		try {
			String accountRoleExternalReferenceCode =
				_accountService.getAccountRoleExternalReferenceCode(
					account.getId(), accountRoleId);

			if (accountRoleExternalReferenceCode == null) {
				return;
			}

			UserAccount userAccount = _userAccountService.getUserAccount(
				userId);

			_accountUserAccountRoleSynchronizer.syncUnassignRole(
				accountRoleExternalReferenceCode,
				userAccount.getExternalReferenceCode(),
				account.getExternalReferenceCode());
		}
		catch (Exception exception) {
			_log.error(
				"Unable to sync account contact role unassignment for user " +
					userId,
				exception);
		}
	}

	private void _unassignContactRoles(
		Account account, UserAccount userAccount) {

		AccountBrief accountBrief = FindUtil.findFirst(
			userAccount.getAccountBriefs(),
			accountBrief1 -> Objects.equals(
				account.getExternalReferenceCode(),
				accountBrief1.getExternalReferenceCode()));

		if (accountBrief == null) {
			return;
		}

		RoleBrief[] roleBriefs = accountBrief.getRoleBriefs();

		if (roleBriefs == null) {
			return;
		}

		for (RoleBrief roleBrief : roleBriefs) {
			try {
				_accountUserAccountRoleSynchronizer.syncUnassignRole(
					roleBrief.getExternalReferenceCode(),
					userAccount.getExternalReferenceCode(),
					account.getExternalReferenceCode());
			}
			catch (Exception exception) {
				_log.error(
					"Unable to sync account contact role unassignment for " +
						"role " + roleBrief.getExternalReferenceCode(),
					exception);
			}
		}
	}

	private static final Log _log = LogFactory.getLog(
		AccountsRestController.class);

	@Autowired
	private AccountAssetService _accountAssetService;

	@Autowired
	private AccountInvitationEmailService _accountInvitationEmailService;

	@Autowired
	private AccountInvitationService _accountInvitationService;

	@Autowired
	private AccountPermission _accountPermission;

	@Autowired
	private AccountService _accountService;

	@Autowired
	private AccountSynchronizer _accountSynchronizer;

	@Autowired
	private AccountUserAccountRoleSynchronizer
		_accountUserAccountRoleSynchronizer;

	@Autowired
	private AccountUserAccountSynchronizer _accountUserAccountSynchronizer;

	@Autowired
	private AdminPermission _adminPermission;

	@Autowired
	private EmailAddressValidatorService _emailAddressValidatorService;

	@Autowired
	private EntitlementService _entitlementService;

	@Autowired
	private OktaService _oktaService;

	@Autowired
	private ProvisioningAssignmentService _provisioningAssignmentService;

	@Autowired
	private ProvisioningEmailService _provisioningEmailService;

	@Autowired
	private UserAccountService _userAccountService;

}