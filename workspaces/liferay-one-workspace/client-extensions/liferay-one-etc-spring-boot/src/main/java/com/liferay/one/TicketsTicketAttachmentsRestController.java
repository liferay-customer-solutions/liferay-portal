/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.liferay.headless.admin.user.client.dto.v1_0.RoleBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.one.constants.RoleConstants;
import com.liferay.one.jira.exception.OrganizationNotFoundException;
import com.liferay.one.jira.model.JiraOrganization;
import com.liferay.one.jira.model.JiraSupportIssue;
import com.liferay.one.jira.service.JiraIssueService;
import com.liferay.one.permission.ProjectMembershipPermission;
import com.liferay.one.service.UserAccountService;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.security.permission.ActionKeys;

import java.security.Principal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Karoline Silva
 */
@RequestMapping("/tickets/{ticketId}/ticket-attachments")
@RestController
public class TicketsTicketAttachmentsRestController
	extends OneBaseRestController {

	@GetMapping("/download-access-check")
	public ResponseEntity<String> getDownloadAccessCheck(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("ticketId") String ticketId)
		throws Exception {

		return _getResponseEntity(ActionKeys.VIEW, true, jwt, ticketId);
	}

	@GetMapping("/upload-access-check")
	public ResponseEntity<String> getUploadAccessCheck(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("ticketId") String ticketId)
		throws Exception {

		return _getResponseEntity(ActionKeys.UPDATE, false, jwt, ticketId);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<String> handleException(Exception exception) {
		_log.error(exception);

		return new ResponseEntity<>(
			"UNEXPECTED_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@ExceptionHandler(OrganizationNotFoundException.class)
	public ResponseEntity<String> handleException(
		OrganizationNotFoundException organizationNotFoundException) {

		_log.error(organizationNotFoundException);

		return new ResponseEntity<>(
			"JIRA_ORGANIZATION_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@ExceptionHandler(PrincipalException.class)
	public ResponseEntity<String> handleException(
		Principal principal, PrincipalException principalException) {

		_log.error(principalException);

		return new ResponseEntity<>("FORBIDDEN_ACCESS", HttpStatus.FORBIDDEN);
	}

	private void _checkPermission(
			String actionId, Jwt jwt, String projectExternalReferenceCode)
		throws Exception {

		UserAccount userAccount = _userAccountService.getMyUserAccount(jwt);

		for (RoleBrief roleBrief : userAccount.getRoleBriefs()) {
			String roleBriefName = roleBrief.getName();

			if (roleBriefName.equals(RoleConstants.NAME_PROVISIONING_MEMBER)) {
				return;
			}
		}

		_projectMembershipPermission.check(
			actionId, jwt, projectExternalReferenceCode);
	}

	private ResponseEntity<String> _getResponseEntity(
			String actionId, boolean allowClosedTicket, Jwt jwt,
			String ticketId)
		throws Exception {

		JiraSupportIssue jiraSupportIssue =
			_jiraIssueService.getJiraSupportIssue(ticketId);

		if (jiraSupportIssue == null) {
			return new ResponseEntity<>(
				"INVALID_TICKET_NUMBER", HttpStatus.NOT_FOUND);
		}

		if (jiraSupportIssue.isClosed() && !allowClosedTicket) {
			return new ResponseEntity<>(
				"TICKET_IS_CLOSED", HttpStatus.BAD_REQUEST);
		}

		JiraOrganization jiraOrganization =
			jiraSupportIssue.getJiraOrganization();

		_checkPermission(actionId, jwt, jiraOrganization.getExternalKey());

		return new ResponseEntity<>(StringPool.BLANK, HttpStatus.OK);
	}

	private static final Log _log = LogFactory.getLog(
		TicketsTicketAttachmentsRestController.class);

	@Autowired
	private JiraIssueService _jiraIssueService;

	@Autowired
	private ProjectMembershipPermission _projectMembershipPermission;

	@Autowired
	private UserAccountService _userAccountService;

}