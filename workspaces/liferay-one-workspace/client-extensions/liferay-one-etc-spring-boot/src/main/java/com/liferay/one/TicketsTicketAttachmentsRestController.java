/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.liferay.headless.admin.user.client.dto.v1_0.Account;
import com.liferay.headless.admin.user.client.dto.v1_0.AccountBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.OrganizationBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.RoleBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.one.constants.RoleConstants;
import com.liferay.one.jira.exception.OrganizationNotFoundException;
import com.liferay.one.jira.model.Organization;
import com.liferay.one.jira.model.SupportIssue;
import com.liferay.one.jira.service.JiraService;
import com.liferay.one.service.AccountService;
import com.liferay.one.service.UserAccountService;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.util.ArrayUtil;

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

		return _getResponseEntity(true, jwt, ticketId);
	}

	@GetMapping("/upload-access-check")
	public ResponseEntity<String> getUploadAccessCheck(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("ticketId") String ticketId)
		throws Exception {

		return _getResponseEntity(false, jwt, ticketId);
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

	private ResponseEntity<String> _getResponseEntity(
			boolean allowClosedTicket, Jwt jwt, String ticketId)
		throws Exception {

		SupportIssue supportIssue = _jiraService.getSupportIssue(ticketId);

		if (supportIssue == null) {
			return new ResponseEntity<>(
				"INVALID_TICKET_NUMBER", HttpStatus.NOT_FOUND);
		}

		if (supportIssue.isClosed() && !allowClosedTicket) {
			return new ResponseEntity<>(
				"TICKET_IS_CLOSED", HttpStatus.BAD_REQUEST);
		}

		Organization organization = supportIssue.getOrganization();

		if (!_hasViewPermission(organization.getExternalKey(), jwt)) {
			return new ResponseEntity<>(
				"FORBIDDEN_ACCESS", HttpStatus.FORBIDDEN);
		}

		return new ResponseEntity<>(StringPool.BLANK, HttpStatus.OK);
	}

	private boolean _hasViewPermission(
			String accountExternalReferenceCode, Jwt jwt)
		throws Exception {

		UserAccount userAccount = _userAccountService.getMyUserAccount(jwt);

		for (RoleBrief roleBrief : userAccount.getRoleBriefs()) {
			String roleBriefName = roleBrief.getName();

			if (roleBriefName.equals(RoleConstants.NAME_PROVISIONING_MEMBER)) {
				return true;
			}
		}

		for (AccountBrief accountBrief : userAccount.getAccountBriefs()) {
			if (!accountExternalReferenceCode.equals(
					accountBrief.getExternalReferenceCode())) {

				continue;
			}

			for (RoleBrief roleBrief : accountBrief.getRoleBriefs()) {
				if (ArrayUtil.contains(
						RoleConstants.NAMES_SUPPORT_ACCOUNT_TICKET,
						roleBrief.getName())) {

					return true;
				}
			}
		}

		Account account = _accountService.getAccount(
			accountExternalReferenceCode, jwt);

		for (OrganizationBrief organizationBrief :
				userAccount.getOrganizationBriefs()) {

			if (ArrayUtil.contains(
					account.getOrganizationIds(), organizationBrief.getId())) {

				return true;
			}
		}

		return false;
	}

	private static final Log _log = LogFactory.getLog(
		TicketsTicketAttachmentsRestController.class);

	@Autowired
	private AccountService _accountService;

	@Autowired
	private JiraService _jiraService;

	@Autowired
	private UserAccountService _userAccountService;

}