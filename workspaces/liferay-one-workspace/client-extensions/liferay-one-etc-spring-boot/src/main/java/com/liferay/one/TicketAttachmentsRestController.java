/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.google.cloud.storage.StorageException;

import com.liferay.client.extension.util.spring.boot3.client.LiferayOAuth2AccessTokenManager;
import com.liferay.headless.admin.user.client.dto.v1_0.RoleBrief;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.one.constants.RoleConstants;
import com.liferay.one.exception.FileServerUnavailableException;
import com.liferay.one.exception.TicketAttachmentAlreadyApprovedException;
import com.liferay.one.exception.TicketAttachmentNotFoundException;
import com.liferay.one.jira.constants.IssueConstants;
import com.liferay.one.jira.exception.OrganizationNotFoundException;
import com.liferay.one.jira.model.JiraOrganization;
import com.liferay.one.jira.model.JiraSupportIssue;
import com.liferay.one.jira.service.JiraIssueService;
import com.liferay.one.model.Project;
import com.liferay.one.model.TicketAttachment;
import com.liferay.one.permission.ProjectMembershipPermission;
import com.liferay.one.service.GoogleCloudStorageService;
import com.liferay.one.service.NotificationQueueEntryService;
import com.liferay.one.service.ProjectService;
import com.liferay.one.service.TicketAttachmentService;
import com.liferay.one.service.UserAccountService;
import com.liferay.petra.function.UnsafeFunction;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.util.StackTraceUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.workflow.WorkflowConstants;

import java.security.Principal;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Amos Fong
 */
@RequestMapping("/ticket-attachments")
@RestController
public class TicketAttachmentsRestController extends OneBaseRestController {

	@DeleteMapping("/{ticketAttachmentId}")
	public ResponseEntity<String> delete(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("ticketAttachmentId") long ticketAttachmentId)
		throws Exception {

		TicketAttachment ticketAttachment =
			_ticketAttachmentService.getTicketAttachment(
				"Bearer " + jwt.getTokenValue(), ticketAttachmentId);

		_checkPermission(
			ActionKeys.UPDATE, jwt, ticketAttachment.getProjectKey());

		_ticketAttachmentService.updateTicketAttachmentState(
			"Bearer " + jwt.getTokenValue(), WorkflowConstants.STATUS_IN_TRASH,
			ticketAttachmentId);

		try {
			_googleCloudStorageService.deleteObject(
				ticketAttachment.getGCSBucketName(),
				ticketAttachment.getGCSObjectName());

			_ticketAttachmentService.deleteTicketAttachment(
				"Bearer " + jwt.getTokenValue(), ticketAttachmentId);

			return new ResponseEntity<>(HttpStatus.OK);
		}
		catch (Exception exception) {
			_log.error(exception);

			return new ResponseEntity<>(StringPool.BLANK, HttpStatus.ACCEPTED);
		}
	}

	@GetMapping("/by-external-reference-code/{externalReferenceCode}/download")
	public ResponseEntity<String> getByExternalReferenceCodeDownload(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("externalReferenceCode") String externalReferenceCode)
		throws Exception {

		return _getResponseEntity(
			jwt,
			authorization -> _ticketAttachmentService.getTicketAttachment(
				authorization, externalReferenceCode));
	}

	@GetMapping("/by-id/{id}/download")
	public ResponseEntity<String> getByIdDownload(
			@AuthenticationPrincipal Jwt jwt, @PathVariable("id") long id)
		throws Exception {

		return _getResponseEntity(
			jwt,
			authorization -> _ticketAttachmentService.getTicketAttachment(
				authorization, id));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<String> handleException(Exception exception) {
		_log.error(exception);

		return new ResponseEntity<>(
			"UNEXPECTED_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@ExceptionHandler(FileServerUnavailableException.class)
	public ResponseEntity<String> handleException(
		FileServerUnavailableException fileServerUnavailableException) {

		_log.error(fileServerUnavailableException);

		return new ResponseEntity<>(
			"FILE_SERVER_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE);
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

	@ExceptionHandler(StorageException.class)
	public ResponseEntity<String> handleException(
		StorageException storageException) {

		_log.error(storageException);

		if (storageException.getCode() == 404) {
			return new ResponseEntity<>(
				"FILE_NOT_FOUND_IN_STORAGE", HttpStatus.NOT_FOUND);
		}

		return new ResponseEntity<>(
			"FILE_SERVER_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE);
	}

	@ExceptionHandler(TicketAttachmentAlreadyApprovedException.class)
	public ResponseEntity<String> handleException(
		TicketAttachmentAlreadyApprovedException
			ticketAttachmentAlreadyApprovedException) {

		_log.error(ticketAttachmentAlreadyApprovedException);

		return new ResponseEntity<>(
			"ATTACHMENT_ALREADY_EXISTS", HttpStatus.CONFLICT);
	}

	@ExceptionHandler(TicketAttachmentNotFoundException.class)
	public ResponseEntity<String> handleException(
		TicketAttachmentNotFoundException ticketAttachmentNotFoundException) {

		_log.error(ticketAttachmentNotFoundException);

		return new ResponseEntity<>(
			"ATTACHMENT_NOT_FOUND", HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(WebClientResponseException.class)
	public ResponseEntity<String> handleException(
		WebClientResponseException webClientResponseException) {

		_log.error(webClientResponseException);

		int statusCode = webClientResponseException.getStatusCode(
		).value();

		if (statusCode == HttpStatus.FORBIDDEN.value()) {
			return new ResponseEntity<>(
				"FORBIDDEN_ACCESS", HttpStatus.FORBIDDEN);
		}

		if (statusCode == HttpStatus.SERVICE_UNAVAILABLE.value()) {
			return new ResponseEntity<>(
				"FILE_SERVER_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE);
		}

		return new ResponseEntity<>(
			"UNEXPECTED_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@PostMapping("/{ticketAttachmentId}/complete-upload")
	public ResponseEntity<String> postCompleteUpload(
			@AuthenticationPrincipal Jwt jwt, @RequestBody String json,
			@PathVariable("ticketAttachmentId") long ticketAttachmentId)
		throws Exception {

		TicketAttachment ticketAttachment =
			_ticketAttachmentService.getTicketAttachment(
				"Bearer " + jwt.getTokenValue(), ticketAttachmentId);

		_checkPermission(
			ActionKeys.UPDATE, jwt, ticketAttachment.getProjectKey());

		ticketAttachment = _ticketAttachmentService.approveTicketAttachment(
			"Bearer " + jwt.getTokenValue(), ticketAttachmentId);

		JSONObject jsonObject = new JSONObject(json);

		String jiraIssueCommentBody = _buildJiraIssueCommentBody(
			jsonObject.optString("commentBody"), jwt, ticketAttachment);

		try {
			_jiraIssueService.addComment(
				jiraIssueCommentBody, ticketAttachment.getJiraIssueKey());

			return new ResponseEntity<>(HttpStatus.OK);
		}
		catch (Exception exception) {
			_log.error(exception);

			_ticketAttachmentService.updateTicketAttachmentDraftCommentBody(
				"Bearer " + jwt.getTokenValue(), jiraIssueCommentBody,
				ticketAttachmentId);

			return new ResponseEntity<>(
				"COMMENT_POST_FAILED_RETRYING", HttpStatus.ACCEPTED);
		}
	}

	@PostMapping("/initiate-upload")
	public ResponseEntity<String> postInitiateUpload(
			@AuthenticationPrincipal Jwt jwt, @RequestBody String json,
			@RequestHeader(name = HttpHeaders.ORIGIN) String origin)
		throws Exception {

		JSONObject jsonObject = new JSONObject(json);

		String fileName = jsonObject.getString("fileName");
		String fileSize = jsonObject.getString("fileSize");
		String gcsSessionURL = jsonObject.optString("gcsSessionURL");
		String md5Checksum = jsonObject.optString("md5Checksum");

		String ticketId = jsonObject.getString("ticketId");

		JiraSupportIssue jiraSupportIssue =
			_jiraIssueService.getJiraSupportIssue(ticketId);

		if (jiraSupportIssue == null) {
			return new ResponseEntity<>(
				"INVALID_TICKET_NUMBER", HttpStatus.NOT_FOUND);
		}

		if (jiraSupportIssue.isClosed()) {
			return new ResponseEntity<>(
				"TICKET_IS_CLOSED", HttpStatus.BAD_REQUEST);
		}

		JiraOrganization jiraOrganization =
			jiraSupportIssue.getJiraOrganization();

		String projectERC = jiraOrganization.getExternalKey();

		_checkPermission(ActionKeys.UPDATE, jwt, projectERC);

		Project project = _projectService.getProject(projectERC);

		TicketAttachment ticketAttachment =
			_ticketAttachmentService.fetchTicketAttachment(
				"Bearer " + jwt.getTokenValue(), fileName, ticketId,
				md5Checksum);

		if (ticketAttachment != null) {
			if (ticketAttachment.isApproved()) {
				throw new TicketAttachmentAlreadyApprovedException();
			}
		}
		else {
			ticketAttachment = _ticketAttachmentService.addTicketAttachment(
				project.getAccountExternalReferenceCode(),
				"Bearer " + jwt.getTokenValue(),
				jsonObject.optString("externalReferenceCode"), fileName,
				fileSize, ticketId, md5Checksum, projectERC,
				TicketAttachment.STATUS_DRAFT, jsonObject.optString("type"));
		}

		JSONObject responseJSONObject = new JSONObject();

		responseJSONObject.put(
			"projectKey", projectERC
		).put(
			"ticketAttachmentId", ticketAttachment.getTicketAttachmentId()
		);

		if (Validator.isNotNull(gcsSessionURL)) {
			responseJSONObject.put("gcsSessionURL", gcsSessionURL);
		}
		else {
			responseJSONObject.put(
				"gcsSessionURL",
				_googleCloudStorageService.getUploadSessionURL(
					ticketAttachment.getGCSBucketName(), fileSize,
					ticketAttachment.getGCSObjectName(), origin));
		}

		return new ResponseEntity<>(
			responseJSONObject.toString(), HttpStatus.OK);
	}

	@Scheduled(cron = "0 0 0,12 * * *")
	public void scheduledCleanUp() throws Exception {
		if (_log.isInfoEnabled()) {
			_log.info("Cleaning up JSM large file attachments");
		}

		StringBundler sb = new StringBundler(9);

		sb.append("(project in (");
		sb.append(_jiraProjectSupportFLS);
		sb.append(StringPool.COMMA);
		sb.append(_jiraProjectSupportHC);
		sb.append(")) and (status in ('");
		sb.append(StringUtil.merge(IssueConstants.STATUSES_CLOSED, "', '"));
		sb.append("')) and (status changed to ('");
		sb.append(StringUtil.merge(IssueConstants.STATUSES_CLOSED, "', '"));
		sb.append("') after -8d before -7d)");

		List<JiraSupportIssue> jiraSupportIssues = _jiraIssueService.search(
			sb.toString(), new String[] {"key"});

		for (JiraSupportIssue jiraSupportIssue : jiraSupportIssues) {
			_deleteTicketAttachments(jiraSupportIssue.getKey());
		}
	}

	@Scheduled(cron = "0 0 * * * *")
	public void scheduledDeleteTicketAttachment() throws Exception {
		List<TicketAttachment> ticketAttachments =
			_ticketAttachmentService.search(
				_getAuthorization(),
				"state eq " + WorkflowConstants.STATUS_IN_TRASH, 1, 500);

		for (TicketAttachment ticketAttachment : ticketAttachments) {
			try {
				_googleCloudStorageService.deleteObject(
					ticketAttachment.getGCSBucketName(),
					ticketAttachment.getGCSObjectName());

				_ticketAttachmentService.deleteTicketAttachment(
					_getAuthorization(),
					ticketAttachment.getTicketAttachmentId());
			}
			catch (Exception exception) {
				_log.error(exception);

				_notificationQueueEntryService.addNotificationQueueEntry(
					"solutions@liferay.com", "Liferay One",
					"is-support@liferay.com", "Liferay One Error Notification",
					StringBundler.concat(
						"<p>There was an error deleting a large file from ",
						"Google Cloud Storage.</p>",
						StackTraceUtil.getStackTrace(exception)));
			}
		}
	}

	@Scheduled(cron = "0 0 */1 * * ?")
	public void scheduledUpdateTicketAttachmentDraftCommentBody()
		throws Exception {

		List<TicketAttachment> ticketAttachments =
			_ticketAttachmentService.search(
				_getAuthorization(),
				"draftCommentBody ne null and draftCommentBody ne '' and " +
					"(state eq 0 or state eq null) and status/any(s:s eq 0)",
				1, 500);

		for (TicketAttachment ticketAttachment : ticketAttachments) {
			try {
				_jiraIssueService.addComment(
					ticketAttachment.getDraftCommentBody(),
					ticketAttachment.getJiraIssueKey());

				_ticketAttachmentService.updateTicketAttachmentDraftCommentBody(
					_getAuthorization(), StringPool.BLANK,
					ticketAttachment.getTicketAttachmentId());
			}
			catch (Exception exception) {
				_log.error(exception);

				_notificationQueueEntryService.addNotificationQueueEntry(
					"solutions@liferay.com", "Liferay One",
					"is-support@liferay.com", "Liferay One Error Notification",
					StringBundler.concat(
						"<p>There was an error posting a large file uploader ",
						"comment to JSM.</p>",
						StackTraceUtil.getStackTrace(exception)));
			}
		}
	}

	private String _buildJiraIssueCommentBody(
			String commentBody, Jwt jwt, TicketAttachment ticketAttachment)
		throws Exception {

		StringBundler sb = new StringBundler(3);

		sb.append(_onePortalURL);
		sb.append("/support/ticket-attachments/#/id/");
		sb.append(ticketAttachment.getTicketAttachmentId());

		return new JSONObject(
		).put(
			"body",
			new JSONObject(
			).put(
				"content",
				new JSONArray(
				).put(
					new JSONObject(
					).put(
						"content",
						new JSONArray(
						).put(
							new JSONObject(
							).put(
								"text",
								_getCommentAuthorInfo(
									jwt, ticketAttachment.getUserId())
							).put(
								"type", "text"
							)
						)
					).put(
						"type", "paragraph"
					)
				).put(
					new JSONObject(
					).put(
						"content",
						new JSONArray(
						).put(
							new JSONObject(
							).put(
								"content",
								new JSONArray(
								).putAll(
									_getCommentBodyJSONArray(commentBody)
								)
							).put(
								"type", "paragraph"
							)
						)
					).put(
						"type", "blockquote"
					)
				).put(
					new JSONObject(
					).put(
						"content",
						new JSONArray(
						).put(
							new JSONObject(
							).put(
								"marks",
								new JSONArray(
								).put(
									new JSONObject(
									).put(
										"attrs",
										new JSONObject(
										).put(
											"href", sb.toString()
										)
									).put(
										"type", "link"
									)
								)
							).put(
								"text", ticketAttachment.getFileName()
							).put(
								"type", "text"
							)
						)
					).put(
						"type", "paragraph"
					)
				)
			).put(
				"type", "doc"
			).put(
				"version", 1
			)
		).toString();
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

	private void _deleteTicketAttachments(String jiraIssueKey)
		throws Exception {

		List<TicketAttachment> ticketAttachments =
			_ticketAttachmentService.search(
				_getAuthorization(), "jiraIssueKey eq '" + jiraIssueKey + "'",
				1, 500);

		for (TicketAttachment ticketAttachment : ticketAttachments) {
			if (_log.isDebugEnabled()) {
				_log.debug(
					"Deleting ticket attachment " +
						ticketAttachment.getTicketAttachmentId());
			}

			_ticketAttachmentService.deleteTicketAttachment(
				_getAuthorization(), ticketAttachment.getTicketAttachmentId());

			_googleCloudStorageService.deleteObject(
				ticketAttachment.getGCSBucketName(),
				ticketAttachment.getGCSObjectName());
		}
	}

	private String _getAuthorization() {
		return _liferayOAuth2AccessTokenManager.getAuthorization(
			"liferay-one-etc-spring-boot-oahs");
	}

	private String _getCommentAuthorInfo(Jwt jwt, long userId) {
		StringBundler sb = new StringBundler(6);

		JSONObject jsonObject = new JSONObject(
			get(
				"Bearer " + jwt.getTokenValue(),
				UriComponentsBuilder.fromPath(
					"/o/headless-admin-user/v1.0/user-accounts/" + userId
				).build(
				).toUri()));

		sb.append(jsonObject.getString("name"));

		sb.append(" (");
		sb.append(jsonObject.getString("emailAddress"));
		sb.append(") ");

		String languageId = jsonObject.optString("languageId");

		if (languageId.equals("es_ES")) {
			sb.append("escribió");
		}
		else if (languageId.equals("ja_JP")) {
			sb.append("_さんが書きました");
		}
		else if (languageId.equals("pt_BR")) {
			sb.append("escreveu");
		}
		else {
			sb.append("wrote");
		}

		sb.append(":");

		return sb.toString();
	}

	private JSONArray _getCommentBodyJSONArray(String commentBody) {
		JSONArray jsonArray = new JSONArray();

		Matcher matcher = _pattern.matcher(commentBody);

		for (String part : commentBody.split(_URL_REGEX)) {
			if (Validator.isNotNull(part)) {
				jsonArray.put(
					new JSONObject(
					).put(
						"text", part
					).put(
						"type", "text"
					));
			}

			if (matcher.find()) {
				String link = matcher.group(1);

				jsonArray.put(
					new JSONObject(
					).put(
						"marks",
						new JSONArray(
						).put(
							new JSONObject(
							).put(
								"attrs",
								new JSONObject(
								).put(
									"href", link
								)
							).put(
								"type", "link"
							)
						)
					).put(
						"text", link
					).put(
						"type", "text"
					));
			}
		}

		return jsonArray;
	}

	private ResponseEntity<String> _getResponseEntity(
			Jwt jwt,
			UnsafeFunction<String, TicketAttachment, Exception> unsafeFunction)
		throws Exception {

		String authorization = "Bearer " + jwt.getTokenValue();

		TicketAttachment ticketAttachment = unsafeFunction.apply(authorization);

		_checkPermission(
			ActionKeys.VIEW, jwt, ticketAttachment.getProjectKey());

		return new ResponseEntity<>(
			_googleCloudStorageService.getDownloadURL(
				ticketAttachment.getGCSBucketName(),
				ticketAttachment.getGCSObjectName()),
			HttpStatus.OK);
	}

	private static final String _URL_REGEX =
		"((?:https?:\\/\\/|www\\.)[^\\s()]+\\b)";

	private static final Log _log = LogFactory.getLog(
		TicketAttachmentsRestController.class);

	private static final Pattern _pattern = Pattern.compile(_URL_REGEX);

	@Autowired
	private GoogleCloudStorageService _googleCloudStorageService;

	@Autowired
	private JiraIssueService _jiraIssueService;

	@Value("${liferay.one.jira.project.support.fls}")
	private String _jiraProjectSupportFLS;

	@Value("${liferay.one.jira.project.support.hc}")
	private String _jiraProjectSupportHC;

	@Autowired
	private LiferayOAuth2AccessTokenManager _liferayOAuth2AccessTokenManager;

	@Autowired
	private NotificationQueueEntryService _notificationQueueEntryService;

	@Value("${liferay.one.portal.url}")
	private String _onePortalURL;

	@Autowired
	private ProjectMembershipPermission _projectMembershipPermission;

	@Autowired
	private ProjectService _projectService;

	@Autowired
	private TicketAttachmentService _ticketAttachmentService;

	@Autowired
	private UserAccountService _userAccountService;

}