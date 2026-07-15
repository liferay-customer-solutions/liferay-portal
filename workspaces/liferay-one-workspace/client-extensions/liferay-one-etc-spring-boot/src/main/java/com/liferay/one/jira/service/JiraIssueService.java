/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.jira.service;

import com.liferay.one.jira.constants.IssueConstants;
import com.liferay.one.jira.converter.OrganizationConverter;
import com.liferay.one.jira.exception.OrganizationNotFoundException;
import com.liferay.one.jira.model.Organization;
import com.liferay.one.jira.model.SupportIssue;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Jenny Chen
 */
@Component
public class JiraIssueService extends BaseJiraService {

	public void addComment(String body, String issueKey) {
		post(
			body,
			HashMapBuilder.put(
				HttpHeaders.AUTHORIZATION, getAuthorization()
			).put(
				HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE
			).build(),
			UriComponentsBuilder.fromUriString(
				StringBundler.concat(
					_jiraURL, "/rest/api/3/issue/", issueKey, "/comment")
			).build(
			).toUri());
	}

	public String addIssue(
			Map<String, Object> customFields, JSONObject descriptionJSONObject,
			String issueTypeId, String projectKey, String summary)
		throws Exception {

		JSONObject fieldsJSONObject = new JSONObject(
		).put(
			"description", descriptionJSONObject
		).put(
			"issuetype",
			new JSONObject(
			).put(
				"id", issueTypeId
			)
		).put(
			"project",
			new JSONObject(
			).put(
				"key", projectKey
			)
		).put(
			"summary", summary
		);

		for (Map.Entry<String, Object> entry : customFields.entrySet()) {
			fieldsJSONObject.put(entry.getKey(), entry.getValue());
		}

		try {
			JSONObject issueJSONObject = new JSONObject(
				post(
					new JSONObject(
					).put(
						"fields", fieldsJSONObject
					).toString(),
					HashMapBuilder.put(
						HttpHeaders.AUTHORIZATION, getAuthorization()
					).put(
						HttpHeaders.CONTENT_TYPE,
						MediaType.APPLICATION_JSON_VALUE
					).build(),
					UriComponentsBuilder.fromUriString(
						StringBundler.concat(
							_jiraURL, _URL_REST_API_3, "/issue")
					).build(
					).toUri()));

			return issueJSONObject.getString("key");
		}
		catch (Exception exception) {
			throw new Exception("Unable to add Jira issue", exception);
		}
	}

	public SupportIssue getSupportIssue(String issueKey)
		throws OrganizationNotFoundException {

		try {
			JSONObject issueJSONObject = new JSONObject(
				get(
					getAuthorization(),
					UriComponentsBuilder.fromUriString(
						StringBundler.concat(
							_jiraURL, _URL_REST_API_3, "/issue/", issueKey)
					).queryParam(
						"expand", "renderedFields"
					).build(
					).toUri()));

			Organization organization = _getOrganization(
				issueJSONObject.optJSONObject("fields"));

			return new SupportIssue(issueJSONObject, organization);
		}
		catch (OrganizationNotFoundException organizationNotFoundException) {
			throw organizationNotFoundException;
		}
		catch (Exception exception) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					"Unable to get Jira issue with key " + issueKey, exception);
			}
		}

		return null;
	}

	public List<SupportIssue> getSupportIssues(
			String externalReferenceCode, String[] issueKeys)
		throws Exception {

		StringBundler sb = new StringBundler(12);

		sb.append("Organization in aqlFunction('\\\"External Key\\\" = \\\"");
		sb.append(externalReferenceCode);
		sb.append("\\\"') and (status not in ('");
		sb.append(
			StringUtil.merge(IssueConstants.STATUSES_SOLVED_AND_CLOSED, "','"));
		sb.append("')) and ");
		sb.append(
			IssueConstants.toJQLCustomField(
				_jiraIssueSupportHCFieldRequestType));
		sb.append(" = '");
		sb.append(IssueConstants.TYPE_GENERAL_REQUEST);
		sb.append("'");

		if (ArrayUtil.isNotEmpty(issueKeys)) {
			sb.append(" or key in ('");
			sb.append(StringUtil.merge(issueKeys, "','"));
			sb.append("')");
		}

		return search(
			sb.toString(), new String[] {"key", "labels", "status", "summary"});
	}

	public List<SupportIssue> search(String jql, String[] returnFields)
		throws Exception {

		List<SupportIssue> supportIssues = new ArrayList<>();

		String nextPageToken = StringPool.BLANK;

		while (true) {
			JSONObject searchResponseJSONObject = _searchJSONObject(
				jql, _MAX_RESULTS, nextPageToken, returnFields);

			if (searchResponseJSONObject == null) {
				break;
			}

			JSONArray issuesJSONArray = searchResponseJSONObject.getJSONArray(
				"issues");

			for (int i = 0; i < issuesJSONArray.length(); i++) {
				JSONObject issueJSONObject = issuesJSONArray.getJSONObject(i);

				String issueKey = issueJSONObject.getString("key");

				String ticketURL =
					_jiraProjectSupportHCURL + StringPool.SLASH + issueKey;

				if (issueKey.startsWith(_jiraProjectSupportFLS)) {
					ticketURL =
						_jiraProjectSupportFLSURL + StringPool.SLASH + issueKey;
				}

				SupportIssue supportIssue = new SupportIssue(
					issueJSONObject, ticketURL);

				supportIssues.add(supportIssue);
			}

			nextPageToken = searchResponseJSONObject.optString("nextPageToken");

			if (Validator.isNull(nextPageToken)) {
				break;
			}
		}

		return supportIssues;
	}

	private Organization _getOrganization(JSONObject jsonObject)
		throws OrganizationNotFoundException {

		if (jsonObject == null) {
			throw new OrganizationNotFoundException();
		}

		JSONArray jsonArray = jsonObject.optJSONArray(
			_jiraIssueSupportHCFieldOrganization);

		if ((jsonArray == null) || jsonArray.isEmpty()) {
			throw new OrganizationNotFoundException();
		}

		try {
			JSONObject organizationJSONObject = jsonArray.getJSONObject(0);

			return _organizationConverter.toOrganization(
				_jiraAssetService.getObject(
					organizationJSONObject.getString("objectId")));
		}
		catch (Exception exception) {
			throw new OrganizationNotFoundException(exception);
		}
	}

	private JSONObject _searchJSONObject(
			String jql, int maxResults, String nextPageToken,
			String[] returnFields)
		throws Exception {

		try {
			return new JSONObject(
				get(
					getAuthorization(),
					UriComponentsBuilder.fromUriString(
						StringBundler.concat(
							_jiraURL, _URL_REST_API_3, "/search/jql")
					).queryParam(
						"expand", "renderedFields"
					).queryParam(
						"fields", StringUtil.merge(returnFields)
					).queryParam(
						"jql", jql
					).queryParam(
						"maxResults", maxResults
					).queryParam(
						"nextPageToken", nextPageToken
					).build(
					).toUri()));
		}
		catch (Exception exception) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					"Unable to get Jira issues with JQL " + jql, exception);
			}
		}

		return null;
	}

	private static final int _MAX_RESULTS = 100;

	private static final String _URL_REST_API_3 = "/rest/api/3";

	private static final Log _log = LogFactory.getLog(JiraIssueService.class);

	@Autowired
	private JiraAssetService _jiraAssetService;

	@Value("${liferay.one.jira.issue.support.hc.field.organization}")
	private String _jiraIssueSupportHCFieldOrganization;

	@Value("${liferay.one.jira.issue.support.hc.field.request.type}")
	private String _jiraIssueSupportHCFieldRequestType;

	@Value("${liferay.one.jira.project.support.fls}")
	private String _jiraProjectSupportFLS;

	@Value("${liferay.one.jira.project.support.fls.url}")
	private String _jiraProjectSupportFLSURL;

	@Value("${liferay.one.jira.project.support.hc.url}")
	private String _jiraProjectSupportHCURL;

	@Value("${liferay.one.jira.url}")
	private String _jiraURL;

	@Autowired
	private OrganizationConverter _organizationConverter;

}