/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.liferay.headless.admin.user.client.dto.v1_0.Account;
import com.liferay.headless.admin.user.client.dto.v1_0.AccountBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.AccountRole;
import com.liferay.headless.admin.user.client.dto.v1_0.RoleBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.one.constants.EntitlementConstants;
import com.liferay.one.constants.RoleConstants;
import com.liferay.one.jira.service.AccountAssetService;
import com.liferay.one.jira.synchronizer.AccountSynchronizer;
import com.liferay.one.jira.synchronizer.AccountUserAccountRoleSynchronizer;
import com.liferay.one.jira.synchronizer.AccountUserAccountSynchronizer;
import com.liferay.one.license.LicenseKeyCSVExporter;
import com.liferay.one.model.AccountInvitation;
import com.liferay.one.model.Entitlement;
import com.liferay.one.model.EntitlementDefinition;
import com.liferay.one.model.LicenseKey;
import com.liferay.one.model.Project;
import com.liferay.one.model.ProjectMembership;
import com.liferay.one.okta.service.OktaService;
import com.liferay.one.permission.AccountPermission;
import com.liferay.one.permission.AdminPermission;
import com.liferay.one.permission.LicenseKeyPermission;
import com.liferay.one.permission.ProjectMembershipPermission;
import com.liferay.one.service.AccountInvitationEmailService;
import com.liferay.one.service.AccountInvitationService;
import com.liferay.one.service.AccountRoleService;
import com.liferay.one.service.AccountService;
import com.liferay.one.service.EmailAddressValidatorService;
import com.liferay.one.service.EntitlementDefinitionService;
import com.liferay.one.service.EntitlementService;
import com.liferay.one.service.LicenseKeyService;
import com.liferay.one.service.ProjectMembershipService;
import com.liferay.one.service.ProjectService;
import com.liferay.one.service.ProvisioningAssignmentService;
import com.liferay.one.service.ProvisioningEmailService;
import com.liferay.one.service.UserAccountService;
import com.liferay.one.util.FindUtil;
import com.liferay.one.util.TermCountUtil;
import com.liferay.one.util.UserAccountUtil;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Validator;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

	@DeleteMapping("/{externalReferenceCode}/invitations/{accountInvitationId}")
	public void deleteInvitations(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("externalReferenceCode") String externalReferenceCode,
			@PathVariable("accountInvitationId") long accountInvitationId)
		throws Exception {

		AccountInvitation accountInvitation = _getPendingAccountInvitation(
			accountInvitationId, externalReferenceCode, jwt);

		_accountInvitationService.deleteAccountInvitation(
			accountInvitation.getAccountInvitationId());
	}

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

		_deleteUserAccountAccountRole(
			_accountService.getAccount(externalReferenceCode, jwt),
			accountRoleId, externalReferenceCode, jwt, userId);
	}

	/**
	 * Ports
	 * <code>AccountResourceImpl#deleteAccountContactByEmailAddresContactEmailAddressRole</code>
	 * from <code>osb-provisioning-rest-impl</code>. The path mirrors the assign
	 * side, <code>postUserAccountsByEmailAddressAccountRoles</code>, rather
	 * than the legacy <code>/contacts/{email}/roles/{names}</code> shape.
	 *
	 * <p>
	 * The legacy contact role vocabulary does not carry over. Legacy took an
	 * array of contact role names and validated Koroneiki specific pairings of
	 * <code>ContactRoleConstants.NAME_PARTNER_MANAGER</code> and
	 * <code>NAME_SUPPORT_ADMINISTRATOR</code> along with support seat counts.
	 * Liferay One addresses roles by account role id and has no contact role
	 * equivalent, so this takes the single role of the existing by user id
	 * endpoint.
	 * </p>
	 */
	@DeleteMapping(
		"/{externalReferenceCode}/user-accounts/by-email-address" +
			"/{emailAddress}/account-roles/{accountRoleId}"
	)
	public void deleteUserAccountsByEmailAddressAccountRole(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("externalReferenceCode") String externalReferenceCode,
			@PathVariable("emailAddress") String emailAddress,
			@PathVariable("accountRoleId") long accountRoleId)
		throws Exception {

		_accountPermission.check(externalReferenceCode, ActionKeys.UPDATE, jwt);

		Account account = _accountService.getAccount(
			externalReferenceCode, jwt);

		UserAccount userAccount =
			_userAccountService.fetchUserAccountByEmailAddress(emailAddress);

		if ((userAccount == null) ||
			!UserAccountUtil.hasAccountMembership(
				userAccount, account.getId())) {

			throw new ResponseStatusException(
				HttpStatus.NOT_FOUND,
				"No account member exists with the email address " +
					emailAddress);
		}

		_deleteUserAccountAccountRole(
			account, accountRoleId, externalReferenceCode, jwt,
			userAccount.getId());
	}

	@GetMapping("/{externalReferenceCode}/invitations")
	public ResponseEntity<String> getInvitations(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("externalReferenceCode") String externalReferenceCode)
		throws Exception {

		_accountPermission.check(externalReferenceCode, ActionKeys.VIEW, jwt);

		JSONArray jsonArray = new JSONArray();

		List<AccountInvitation> accountInvitations =
			_accountInvitationService.getPendingAccountInvitations(
				externalReferenceCode);

		for (AccountInvitation accountInvitation : accountInvitations) {
			jsonArray.put(_toJSONObject(accountInvitation));
		}

		return new ResponseEntity<>(jsonArray.toString(), HttpStatus.OK);
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

	@GetMapping("/{externalReferenceCode}/license-keys")
	public List<LicenseKey> getLicenseKeys(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("externalReferenceCode") String externalReferenceCode)
		throws Exception {

		Account account = _accountService.getAccount(
			externalReferenceCode, jwt);

		_licenseKeyPermission.check(account.getId(), ActionKeys.VIEW, jwt);

		return _licenseKeyService.getLicenseKeysByAccountEntryId(
			account.getId());
	}

	@GetMapping("/{externalReferenceCode}/license-keys/export")
	public ResponseEntity<String> getLicenseKeysExport(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("externalReferenceCode") String externalReferenceCode)
		throws Exception {

		Account account = _accountService.getAccount(
			externalReferenceCode, jwt);

		_licenseKeyPermission.check(account.getId(), ActionKeys.VIEW, jwt);

		return ResponseEntity.ok(
		).contentType(
			_CONTENT_TYPE_CSV
		).header(
			HttpHeaders.CONTENT_DISPOSITION,
			"attachment; filename=\"" + _licenseKeyCSVExporter.getFileName() +
				"\""
		).body(
			_licenseKeyCSVExporter.toCSV(
				_licenseKeyService.getLicenseKeysByAccountEntryId(
					account.getId()))
		);
	}

	@GetMapping("/{accountKey}/products/{productExternalReferenceCode}/usage")
	public ResponseEntity<String> getProductUsage(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("accountKey") String accountKey,
			@PathVariable("productExternalReferenceCode") String
				productExternalReferenceCode)
		throws Exception {

		Account account = _accountService.getAccount(accountKey, jwt);

		_licenseKeyPermission.check(account.getId(), ActionKeys.VIEW, jwt);

		if (!ArrayUtil.contains(
				EntitlementConstants.EXTERNAL_REFERENCE_CODES_SELF_HOSTED,
				productExternalReferenceCode)) {

			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}

		EntitlementDefinition entitlementDefinition =
			_entitlementDefinitionService.fetchEntitlementDefinition(
				productExternalReferenceCode);

		if (entitlementDefinition == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}

		List<Entitlement> entitlements = _entitlementService.getEntitlements(
			account.getId(),
			entitlementDefinition.getEntitlementDefinitionId());

		if (entitlements.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}

		return ResponseEntity.ok(
		).contentType(
			MediaType.APPLICATION_JSON
		).body(
			_getProductUsageJSON(account, entitlements)
		);
	}

	@PostMapping("/{externalReferenceCode}/invitations")
	public void postInvitations(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("externalReferenceCode") String externalReferenceCode,
			@RequestBody String json)
		throws Exception {

		JSONObject jsonObject = new JSONObject(json);

		String emailAddress = jsonObject.optString("emailAddress");
		String familyName = jsonObject.optString("familyName");
		String givenName = jsonObject.optString("givenName");
		String projectExternalReferenceCode = jsonObject.optString(
			"projectExternalReferenceCode");
		String projectRoleExternalReferenceCode = jsonObject.optString(
			"projectRoleExternalReferenceCode");

		Account account = _accountService.getAccount(
			externalReferenceCode, jwt);

		_validateInvitation(emailAddress, familyName, givenName);

		UserAccount userAccount =
			_userAccountService.fetchUserAccountByEmailAddress(emailAddress);

		Project project = null;

		if (Validator.isNotNull(projectExternalReferenceCode)) {
			_projectMembershipPermission.check(
				ActionKeys.UPDATE, jwt, projectExternalReferenceCode);

			project = _projectService.fetchProject(
				projectExternalReferenceCode);

			_validateProjectInvitation(
				externalReferenceCode, project, projectExternalReferenceCode,
				projectRoleExternalReferenceCode, userAccount);
		}
		else {
			_accountPermission.check(
				externalReferenceCode, ActionKeys.UPDATE, jwt);

			_validateAccountInvitation(account, userAccount);
		}

		List<String> roleExternalReferenceCodes =
			_getRoleExternalReferenceCodes(
				jsonObject.optJSONArray("roleExternalReferenceCodes"));

		UserAccount inviterUserAccount = getMyUserAccount(jwt);

		AccountInvitation pendingAccountInvitation =
			_accountInvitationService.fetchPendingAccountInvitation(
				externalReferenceCode, emailAddress,
				projectExternalReferenceCode);

		AccountInvitation accountInvitation = null;

		if (pendingAccountInvitation == null) {
			accountInvitation = _accountInvitationService.addAccountInvitation(
				externalReferenceCode, emailAddress, familyName, givenName,
				projectExternalReferenceCode, projectRoleExternalReferenceCode,
				roleExternalReferenceCodes);
		}
		else {
			accountInvitation =
				_accountInvitationService.updateAccountInvitation(
					pendingAccountInvitation.getAccountInvitationId(),
					familyName, givenName, projectRoleExternalReferenceCode,
					roleExternalReferenceCodes);
		}

		if (project == null) {
			_accountInvitationEmailService.sendInvitationEmail(
				account, accountInvitation, inviterUserAccount.getName());
		}
		else {
			_accountInvitationEmailService.sendInvitationEmail(
				account, accountInvitation, inviterUserAccount.getName(),
				project);
		}
	}

	@PostMapping(
		"/{externalReferenceCode}/invitations/{accountInvitationId}/resend"
	)
	public void postInvitationsResend(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("externalReferenceCode") String externalReferenceCode,
			@PathVariable("accountInvitationId") long accountInvitationId)
		throws Exception {

		AccountInvitation accountInvitation = _getPendingAccountInvitation(
			accountInvitationId, externalReferenceCode, jwt);

		AccountInvitation renewedAccountInvitation =
			_accountInvitationService.renewAccountInvitation(
				accountInvitation.getAccountInvitationId());

		UserAccount inviterUserAccount = getMyUserAccount(jwt);

		Account account = _accountService.getAccount(
			externalReferenceCode, jwt);

		String projectExternalReferenceCode =
			accountInvitation.getProjectExternalReferenceCode();

		if (Validator.isNull(projectExternalReferenceCode)) {
			_accountInvitationEmailService.sendInvitationEmail(
				account, renewedAccountInvitation,
				inviterUserAccount.getName());
		}
		else {
			_accountInvitationEmailService.sendInvitationEmail(
				account, renewedAccountInvitation, inviterUserAccount.getName(),
				_projectService.fetchProject(projectExternalReferenceCode));
		}
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

		AccountRole accountRole = _accountRoleService.fetchAccountRole(
			accountRoleId);

		if (accountRole != null) {
			_provisioningAssignmentService.assignAccountRole(
				account, userId, accountRole.getName());
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

		JSONObject jsonObject = new JSONObject(json);

		Account account = _accountService.getAccount(
			externalReferenceCode, jwt);

		Map<Long, String> accountRoleNames = _getAccountRoleNames(
			jsonObject.optJSONArray("accountRoleIds"));

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

	/**
	 * Extracted from <code>deleteUserAccountsAccountRole</code> so the by user
	 * id and by email address paths unassign identically. Legacy reached the
	 * same effect through
	 * <code>AccountWebService#unassignContactRolesByEmailAddress</code>.
	 */
	private void _deleteUserAccountAccountRole(
			Account account, long accountRoleId, String externalReferenceCode,
			Jwt jwt, long userId)
		throws Exception {

		AccountRole accountRole = _accountRoleService.fetchAccountRole(
			accountRoleId);

		_accountService.removeAccountUserAccountRole(
			accountRoleId, externalReferenceCode, jwt, userId);

		_unassignContactRole(account, accountRoleId, userId);

		_syncMembership(account, userId);

		if (accountRole != null) {
			_provisioningAssignmentService.unassignAccountRole(
				account, userId, accountRole.getName());
		}
	}

	private Map<Long, String> _getAccountRoleNames(
			JSONArray accountRoleIdsJSONArray)
		throws Exception {

		Map<Long, String> accountRoleNames = new LinkedHashMap<>();

		if (accountRoleIdsJSONArray == null) {
			return accountRoleNames;
		}

		for (int i = 0; i < accountRoleIdsJSONArray.length(); i++) {
			long accountRoleId = accountRoleIdsJSONArray.getLong(i);

			AccountRole accountRole = _accountRoleService.fetchAccountRole(
				accountRoleId);

			if (accountRole == null) {
				throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"Unable to find account role " + accountRoleId);
			}

			accountRoleNames.put(accountRoleId, accountRole.getName());
		}

		return accountRoleNames;
	}

	private AccountInvitation _getPendingAccountInvitation(
			long accountInvitationId, String externalReferenceCode, Jwt jwt)
		throws Exception {

		AccountInvitation accountInvitation =
			_accountInvitationService.fetchAccountInvitation(
				accountInvitationId);

		if ((accountInvitation == null) || accountInvitation.isAccepted() ||
			!Objects.equals(
				accountInvitation.getAccountExternalReferenceCode(),
				externalReferenceCode)) {

			throw new ResponseStatusException(
				HttpStatus.NOT_FOUND,
				"Unable to find a pending invitation " + accountInvitationId +
					" for this account");
		}

		String projectExternalReferenceCode =
			accountInvitation.getProjectExternalReferenceCode();

		if (Validator.isNull(projectExternalReferenceCode)) {
			_accountPermission.check(
				externalReferenceCode, ActionKeys.UPDATE, jwt);
		}
		else {
			_projectMembershipPermission.check(
				ActionKeys.UPDATE, jwt, projectExternalReferenceCode);
		}

		return accountInvitation;
	}

	private String _getProductUsageJSON(
			Account account, List<Entitlement> entitlements)
		throws Exception {

		int currentYear = TermCountUtil.getYear(Instant.now());

		TreeMap<Instant, Integer> consumptionCounts = new TreeMap<>();
		TreeMap<Instant, Integer> subscriptionCounts = new TreeMap<>();

		Set<Long> entitlementIds = new HashSet<>();

		for (Entitlement entitlement : entitlements) {
			entitlementIds.add(entitlement.getEntitlementId());

			Double quantity = entitlement.getQuantity();

			int count = 0;

			if (quantity != null) {
				count = quantity.intValue();
			}

			TermCountUtil.consolidate(
				subscriptionCounts, currentYear,
				entitlement.getStartDateInstant(),
				entitlement.getEndDateInstant(), count);
		}

		for (LicenseKey licenseKey :
				_licenseKeyService.getLicenseKeysByAccountEntryId(
					account.getId())) {

			if (!entitlementIds.contains(licenseKey.getEntitlementId())) {
				continue;
			}

			TermCountUtil.consolidate(
				consumptionCounts, currentYear,
				licenseKey.getStartDateInstant(),
				licenseKey.getCustomExpirationDateInstant(), 1);
		}

		Map<Integer, Integer> maxConcurrentConsumptions =
			TermCountUtil.getMaxConcurrentCounts(
				consumptionCounts, currentYear);
		Map<Integer, Integer> maxConcurrentQuantities =
			TermCountUtil.getMaxConcurrentCounts(
				subscriptionCounts, currentYear);

		JSONArray jsonArray = new JSONArray();

		for (int year = currentYear - 1; year <= (currentYear + 1); year++) {
			jsonArray.put(
				new JSONObject(
				).put(
					"maxConcurrentConsumption",
					GetterUtil.getInteger(maxConcurrentConsumptions.get(year))
				).put(
					"maxConcurrentQuantity",
					GetterUtil.getInteger(maxConcurrentQuantities.get(year))
				).put(
					"year", year
				));
		}

		JSONObject jsonObject = new JSONObject(
		).put(
			"annualSubscriptions", jsonArray
		).put(
			"currentConsumption",
			TermCountUtil.getCurrentCount(consumptionCounts)
		);

		return jsonObject.toString();
	}

	private List<String> _getRoleExternalReferenceCodes(
			JSONArray roleExternalReferenceCodesJSONArray)
		throws Exception {

		List<String> roleExternalReferenceCodes = new ArrayList<>();

		if ((roleExternalReferenceCodesJSONArray == null) ||
			(roleExternalReferenceCodesJSONArray.length() == 0)) {

			return roleExternalReferenceCodes;
		}

		for (int i = 0; i < roleExternalReferenceCodesJSONArray.length(); i++) {
			String roleExternalReferenceCode =
				roleExternalReferenceCodesJSONArray.getString(i);

			AccountRole accountRole =
				_accountRoleService.fetchAccountRoleByExternalReferenceCode(
					roleExternalReferenceCode);

			if (accountRole == null) {
				throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"Unable to find account role " + roleExternalReferenceCode);
			}

			roleExternalReferenceCodes.add(roleExternalReferenceCode);
		}

		return roleExternalReferenceCodes;
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

	private JSONObject _toJSONObject(AccountInvitation accountInvitation)
		throws Exception {

		JSONArray roleNamesJSONArray = new JSONArray();

		for (String roleExternalReferenceCode :
				accountInvitation.getRoleExternalReferenceCodes()) {

			AccountRole accountRole =
				_accountRoleService.fetchAccountRoleByExternalReferenceCode(
					roleExternalReferenceCode);

			if (accountRole != null) {
				roleNamesJSONArray.put(accountRole.getName());
			}
		}

		Instant customExpirationDateInstant =
			accountInvitation.getCustomExpirationDateInstant();

		String expirationDate = null;

		if (customExpirationDateInstant != null) {
			expirationDate = DateTimeFormatter.ISO_INSTANT.format(
				customExpirationDateInstant);
		}

		return new JSONObject(
		).put(
			"emailAddress", accountInvitation.getEmailAddress()
		).put(
			"expirationDate", expirationDate
		).put(
			"familyName", accountInvitation.getFamilyName()
		).put(
			"givenName", accountInvitation.getGivenName()
		).put(
			"id", accountInvitation.getAccountInvitationId()
		).put(
			"projectExternalReferenceCode",
			accountInvitation.getProjectExternalReferenceCode()
		).put(
			"projectRoleExternalReferenceCode",
			accountInvitation.getProjectRoleExternalReferenceCode()
		).put(
			"roleNames", roleNamesJSONArray
		);
	}

	private void _unassignContactRole(
		Account account, long accountRoleId, long userId) {

		try {
			AccountRole accountRole = _accountRoleService.fetchAccountRole(
				accountRoleId);

			if (accountRole == null) {
				return;
			}

			UserAccount userAccount = _userAccountService.getUserAccount(
				userId);

			_accountUserAccountRoleSynchronizer.syncUnassignRole(
				accountRole.getExternalReferenceCode(),
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

	private void _validateAccountInvitation(
		Account account, UserAccount userAccount) {

		if ((userAccount != null) &&
			UserAccountUtil.hasAccountMembership(
				userAccount, account.getId())) {

			throw new ResponseStatusException(
				HttpStatus.CONFLICT,
				"The user is already a member of this account");
		}
	}

	private void _validateInvitation(
		String emailAddress, String familyName, String givenName) {

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
	}

	private void _validateProjectInvitation(
			String externalReferenceCode, Project project,
			String projectExternalReferenceCode,
			String projectRoleExternalReferenceCode, UserAccount userAccount)
		throws Exception {

		if ((project == null) ||
			!Objects.equals(
				project.getAccountExternalReferenceCode(),
				externalReferenceCode)) {

			throw new ResponseStatusException(
				HttpStatus.BAD_REQUEST,
				"Unable to find project " + projectExternalReferenceCode +
					" for this account");
		}

		if (!ArrayUtil.contains(
				RoleConstants.ERCS_SUPPORT_PROJECT,
				projectRoleExternalReferenceCode)) {

			throw new ResponseStatusException(
				HttpStatus.BAD_REQUEST,
				"Unable to find project role " +
					projectRoleExternalReferenceCode);
		}

		if (userAccount == null) {
			return;
		}

		ProjectMembership projectMembership =
			_projectMembershipService.fetchProjectMembership(
				projectExternalReferenceCode, projectRoleExternalReferenceCode,
				userAccount.getId());

		if (projectMembership != null) {
			throw new ResponseStatusException(
				HttpStatus.CONFLICT,
				"The user already has this role in this project");
		}
	}

	private static final MediaType _CONTENT_TYPE_CSV = MediaType.parseMediaType(
		"text/csv");

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
	private AccountRoleService _accountRoleService;

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
	private EntitlementDefinitionService _entitlementDefinitionService;

	@Autowired
	private EntitlementService _entitlementService;

	@Autowired
	private LicenseKeyCSVExporter _licenseKeyCSVExporter;

	@Autowired
	private LicenseKeyPermission _licenseKeyPermission;

	@Autowired
	private LicenseKeyService _licenseKeyService;

	@Autowired
	private OktaService _oktaService;

	@Autowired
	private ProjectMembershipPermission _projectMembershipPermission;

	@Autowired
	private ProjectMembershipService _projectMembershipService;

	@Autowired
	private ProjectService _projectService;

	@Autowired
	private ProvisioningAssignmentService _provisioningAssignmentService;

	@Autowired
	private ProvisioningEmailService _provisioningEmailService;

	@Autowired
	private UserAccountService _userAccountService;

}