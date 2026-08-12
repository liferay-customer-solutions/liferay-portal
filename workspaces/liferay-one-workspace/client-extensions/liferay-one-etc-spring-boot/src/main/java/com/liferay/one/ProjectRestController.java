/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.liferay.one.exception.GoogleCloudFunctionUnavailableException;
import com.liferay.one.exception.InvalidUsageProductException;
import com.liferay.one.jira.service.AccountAssetService;
import com.liferay.one.jira.synchronizer.AccountSynchronizer;
import com.liferay.one.permission.BusinessEventPermission;
import com.liferay.one.service.ProjectMembershipService;
import com.liferay.one.service.ProjectService;
import com.liferay.portal.kernel.security.permission.ActionKeys;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

	@GetMapping("/{externalReferenceCode}/usage")
	public ResponseEntity<String> getUsage(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("externalReferenceCode") String externalReferenceCode,
			@RequestParam(
				name = "productExternalReferenceCode", required = false
			)
			String productExternalReferenceCode)
		throws Exception {

		_businessEventPermission.check(
			ActionKeys.VIEW, jwt, externalReferenceCode);

		JSONObject projectUsageJSONObject = _projectService.getProjectUsage(
			productExternalReferenceCode, externalReferenceCode);

		return new ResponseEntity<>(
			projectUsageJSONObject.toString(), HttpStatus.OK);
	}

	@ExceptionHandler(GoogleCloudFunctionUnavailableException.class)
	public ResponseEntity<ProblemDetail> handleException(
		GoogleCloudFunctionUnavailableException
			googleCloudFunctionUnavailableException) {

		_log.error(
			"Unable to reach the DataOps usage API",
			googleCloudFunctionUnavailableException);

		return new ResponseEntity<>(
			ProblemDetail.forStatusAndDetail(
				HttpStatus.BAD_GATEWAY,
				"The usage service is temporarily unavailable"),
			HttpStatus.BAD_GATEWAY);
	}

	@ExceptionHandler(InvalidUsageProductException.class)
	public ResponseEntity<ProblemDetail> handleException(
		InvalidUsageProductException invalidUsageProductException) {

		if (_log.isWarnEnabled()) {
			_log.warn(
				"Unable to resolve a usage dashboard for the product",
				invalidUsageProductException);
		}

		return new ResponseEntity<>(
			ProblemDetail.forStatusAndDetail(
				HttpStatus.BAD_REQUEST,
				invalidUsageProductException.getMessage()),
			HttpStatus.BAD_REQUEST);
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
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("externalReferenceCode") String externalReferenceCode)
		throws Exception {

		_businessEventPermission.check(
			ActionKeys.UPDATE, jwt, externalReferenceCode);

		_accountSynchronizer.syncProject(
			_projectService.getProject(externalReferenceCode));

		return new ResponseEntity<>(HttpStatus.OK);
	}

	private static final Log _log = LogFactory.getLog(
		ProjectRestController.class);

	@Autowired
	private AccountAssetService _accountAssetService;

	@Autowired
	private AccountSynchronizer _accountSynchronizer;

	@Autowired
	private BusinessEventPermission _businessEventPermission;

	@Autowired
	private ProjectMembershipService _projectMembershipService;

	@Autowired
	private ProjectService _projectService;

}