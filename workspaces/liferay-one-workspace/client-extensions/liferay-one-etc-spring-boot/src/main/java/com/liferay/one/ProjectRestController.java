/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.liferay.one.jira.service.AccountAssetService;
import com.liferay.one.jira.service.AccountSyncService;
import com.liferay.one.permission.BusinessEventPermission;
import com.liferay.one.service.ProjectMembershipService;
import com.liferay.one.service.ProjectService;
import com.liferay.portal.kernel.security.permission.ActionKeys;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author Amos Fong
 */
@RequestMapping("/projects")
@RestController
public class ProjectRestController extends OneBaseRestController {

	@DeleteMapping(
		"/{projectId}/user-accounts/{userId}/account-roles" +
			"/{accountRoleExternalReferenceCode}"
	)
	public void deleteProjectMemberships(
			@AuthenticationPrincipal Jwt jwt, @PathVariable String projectId,
			@PathVariable long userId,
			@PathVariable String accountRoleExternalReferenceCode)
		throws Exception {

		_projectMembershipService.deleteProjectMembership(
			jwt, projectId, accountRoleExternalReferenceCode, userId);
	}

	@GetMapping("/{externalReferenceCode}/jira/object-key")
	public ResponseEntity<String> getJiraObjectKey(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("externalReferenceCode") String externalReferenceCode)
		throws Exception {

		_businessEventPermission.check(
			ActionKeys.VIEW, jwt, externalReferenceCode);

		return new ResponseEntity<>(
			_accountAssetService.getAccountObjectKey(externalReferenceCode),
			HttpStatus.OK);
	}

	@PostMapping(
		"/{projectId}/user-accounts/{userId}/account-roles" +
			"/{accountRoleExternalReferenceCode}"
	)
	public void postProjectMemberships(
			@AuthenticationPrincipal Jwt jwt, @PathVariable String projectId,
			@PathVariable long userId,
			@PathVariable String accountRoleExternalReferenceCode)
		throws Exception {

		_projectMembershipService.addProjectMembership(
			jwt, projectId, accountRoleExternalReferenceCode, userId);
	}

	@PostMapping("/{externalReferenceCode}/sync-to-jsm")
	public ResponseEntity<Void> postSyncToJSM(
		@PathVariable("externalReferenceCode") String externalReferenceCode) {

		try {
			_accountSyncService.syncProject(
				_projectService.getProject(externalReferenceCode));
		}
		catch (Exception exception) {
			throw new ResponseStatusException(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"There was a problem synchronizing the JIRA object keys",
				exception);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@Autowired
	private AccountAssetService _accountAssetService;

	@Autowired
	private AccountSyncService _accountSyncService;

	@Autowired
	private BusinessEventPermission _businessEventPermission;

	@Autowired
	private ProjectMembershipService _projectMembershipService;

	@Autowired
	private ProjectService _projectService;

}