/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.liferay.headless.admin.user.client.dto.v1_0.Account;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.one.model.ProjectMembership;
import com.liferay.one.service.AccountService;
import com.liferay.one.service.ProjectMembershipService;
import com.liferay.one.service.ProvisioningAssignmentService;
import com.liferay.one.service.ProvisioningEmailService;
import com.liferay.one.service.UserAccountService;
import com.liferay.one.util.UserAccountUtil;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author Felipe Veloso
 */
@RequestMapping("/user-accounts")
@RestController
public class UserAccountsRestController extends OneBaseRestController {

	@PostMapping("/assignments")
	public void postAssignments(@RequestBody String json) throws Exception {
		JSONObject jsonObject = new JSONObject(json);

		if (jsonObject.isNull("accountId") || jsonObject.isNull("userId")) {
			throw new ResponseStatusException(
				HttpStatus.BAD_REQUEST,
				"\"accountId\" and \"userId\" are required");
		}

		JSONArray projectsJSONArray = jsonObject.optJSONArray("projects");

		_validateProjects(projectsJSONArray, true);

		long accountId = jsonObject.getLong("accountId");
		long userId = jsonObject.getLong("userId");

		Long accountRoleId = null;

		if (!jsonObject.isNull("accountRoleId")) {
			accountRoleId = jsonObject.getLong("accountRoleId");
		}

		boolean wasMember = _accountService.hasAccountUserAccount(
			accountId, userId);

		_accountService.addAccountUserAccount(accountId, userId, accountRoleId);

		if (projectsJSONArray != null) {
			for (int i = 0; i < projectsJSONArray.length(); i++) {
				JSONObject projectJSONObject = projectsJSONArray.getJSONObject(
					i);

				_projectMembershipService.addProjectMembership(
					accountId, userId,
					projectJSONObject.getString("projectExternalReferenceCode"),
					projectJSONObject.getString("roleExternalReferenceCode"));
			}
		}

		Account account = _accountService.fetchAccount(accountId);

		if (accountRoleId != null) {
			String accountRoleName = _accountService.getAccountRoleName(
				accountId, accountRoleId);

			if (accountRoleName != null) {
				_provisioningAssignmentService.assignAccountRole(
					account, userId, accountRoleName);
			}
		}
		else {
			_provisioningAssignmentService.assignCustomerGroup(userId);
		}

		if (!wasMember) {
			_provisioningEmailService.sendAssignedWelcomeEmail(userId, account);
		}
	}

	@PostMapping("/unassignments")
	public void postUnassignments(@RequestBody String json) throws Exception {
		JSONObject jsonObject = new JSONObject(json);

		if (jsonObject.isNull("accountId") || jsonObject.isNull("userId")) {
			throw new ResponseStatusException(
				HttpStatus.BAD_REQUEST,
				"\"accountId\" and \"userId\" are required");
		}

		JSONArray projectsJSONArray = jsonObject.optJSONArray("projects");

		_validateProjects(projectsJSONArray, false);

		long accountId = jsonObject.getLong("accountId");
		long userId = jsonObject.getLong("userId");

		if (!jsonObject.isNull("accountRoleId")) {
			long accountRoleId = jsonObject.getLong("accountRoleId");

			String accountRoleName = _accountService.getAccountRoleName(
				accountId, accountRoleId);

			_accountService.removeAccountUserAccountRole(
				accountId, userId, accountRoleId);

			if (accountRoleName != null) {
				_provisioningAssignmentService.unassignAccountRole(
					_accountService.fetchAccount(accountId), userId,
					accountRoleName);
			}
		}

		if (projectsJSONArray != null) {
			for (int i = 0; i < projectsJSONArray.length(); i++) {
				JSONObject projectJSONObject = projectsJSONArray.getJSONObject(
					i);

				_projectMembershipService.deleteProjectMembership(
					accountId, userId,
					projectJSONObject.getString(
						"projectExternalReferenceCode"));
			}
		}

		if (jsonObject.isNull("accountRoleId")) {
			_removeAccountMembership(accountId, userId);
		}
	}

	private void _removeAccountMembership(long accountId, long userId)
		throws Exception {

		UserAccount userAccount = _userAccountService.getUserAccount(userId);

		if (!UserAccountUtil.hasAccountMembership(userAccount, accountId) ||
			!UserAccountUtil.getAccountRoleNames(
				userAccount, accountId
			).isEmpty()) {

			return;
		}

		List<ProjectMembership> projectMemberships =
			_projectMembershipService.getProjectMemberships(accountId, userId);

		if (!projectMemberships.isEmpty()) {
			return;
		}

		_accountService.removeAccountUserAccount(accountId, userId);

		_provisioningAssignmentService.unassignAccountMembership(
			accountId, userId);
	}

	private void _validateProjects(
		JSONArray projectsJSONArray, boolean requireRoleExternalReferenceCode) {

		if (projectsJSONArray == null) {
			return;
		}

		for (int i = 0; i < projectsJSONArray.length(); i++) {
			JSONObject projectJSONObject = projectsJSONArray.optJSONObject(i);

			if (projectJSONObject == null) {
				throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"Each \"projects\" entry must be an object");
			}

			if (projectJSONObject.isNull("projectExternalReferenceCode")) {
				throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"\"projectExternalReferenceCode\" is required for each " +
						"project");
			}

			if (requireRoleExternalReferenceCode &&
				projectJSONObject.isNull("roleExternalReferenceCode")) {

				throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"\"roleExternalReferenceCode\" is required for each " +
						"project");
			}
		}
	}

	@Autowired
	private AccountService _accountService;

	@Autowired
	private ProjectMembershipService _projectMembershipService;

	@Autowired
	private ProvisioningAssignmentService _provisioningAssignmentService;

	@Autowired
	private ProvisioningEmailService _provisioningEmailService;

	@Autowired
	private UserAccountService _userAccountService;

}