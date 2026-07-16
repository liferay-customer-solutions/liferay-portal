/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.one.model.Project;
import com.liferay.portal.kernel.util.Validator;

import java.net.URI;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONObject;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Felipe Veloso
 */
@Component
public class ProjectService extends OneBaseService {

	public Project fetchProject(String externalReferenceCode) throws Exception {
		return _fetchProject(getAuthorization(), externalReferenceCode);
	}

	public Project fetchProject(String externalReferenceCode, Jwt jwt)
		throws Exception {

		return _fetchProject(getAuthorization(jwt), externalReferenceCode);
	}

	public Project getProject(String externalReferenceCode) throws Exception {
		String response = get(
			getAuthorization(),
			UriComponentsBuilder.fromPath(
				"/o/c/projects/by-external-reference-code" +
					"/{externalReferenceCode}"
			).buildAndExpand(
				externalReferenceCode
			).toUri());

		return new Project(new JSONObject(response));
	}

	public List<Project> getProjects(long accountId) throws Exception {
		return getAllItems(
			"/o/c/projects",
			"r_accountEntryToProject_accountEntryId eq '" + accountId + "'",
			Project::new);
	}

	public void upsertProject(
			com.liferay.one.salesforce.model.Project salesforceProject)
		throws Exception {

		upsertProject(null, salesforceProject);
	}

	public void upsertProject(
			String accountExternalReferenceCode,
			com.liferay.one.salesforce.model.Project salesforceProject)
		throws Exception {

		String accountEntryERC = salesforceProject.getAccountId();

		if (Validator.isNull(accountEntryERC)) {
			accountEntryERC = accountExternalReferenceCode;
		}

		if (Validator.isNull(salesforceProject.getId()) ||
			Validator.isNull(salesforceProject.getName()) ||
			Validator.isNull(accountEntryERC)) {

			if (_log.isWarnEnabled()) {
				_log.warn(
					"Unable to upsert project " + salesforceProject.getId() +
						" without an ID, name, and account");
			}

			return;
		}

		JSONObject jsonObject = new JSONObject();

		if (Validator.isNotNull(salesforceProject.getAIHubAccountName())) {
			jsonObject.put(
				"aiHubAccountName", salesforceProject.getAIHubAccountName());
		}

		if (Validator.isNotNull(salesforceProject.getAllowedEmailDomains())) {
			jsonObject.put(
				"allowedEmailDomains",
				salesforceProject.getAllowedEmailDomains());
		}

		if (Validator.isNotNull(salesforceProject.getDataCenterLocation())) {
			jsonObject.put(
				"dataCenterLocation",
				salesforceProject.getDataCenterLocation());
		}

		jsonObject.put("externalReferenceCode", salesforceProject.getId());

		if (Validator.isNotNull(salesforceProject.getFriendlyWorkspaceURL())) {
			jsonObject.put(
				"friendlyWorkspaceURL",
				salesforceProject.getFriendlyWorkspaceURL());
		}

		if (Validator.isNotNull(salesforceProject.getLiferayVersion())) {
			jsonObject.put(
				"liferayVersion", salesforceProject.getLiferayVersion());
		}

		jsonObject.put(
			"name", salesforceProject.getName()
		).put(
			"r_accountEntryToProject_accountEntryERC", accountEntryERC
		);

		if (Validator.isNotNull(
				salesforceProject.getSecurityContactEmailAddress())) {

			jsonObject.put(
				"securityContactEmailAddress",
				salesforceProject.getSecurityContactEmailAddress());
		}

		URI uri = UriComponentsBuilder.fromPath(
			"/o/c/projects/by-external-reference-code/" +
				salesforceProject.getId()
		).build(
		).toUri();

		try {
			patch(getAuthorization(), jsonObject.toString(), uri);
		}
		catch (WebClientResponseException webClientResponseException) {
			int statusCode = webClientResponseException.getStatusCode(
			).value();

			if (statusCode != HttpStatus.NOT_FOUND.value()) {
				throw webClientResponseException;
			}

			put(getAuthorization(), jsonObject.toString(), uri);
		}
	}

	private Project _fetchProject(
			String authorization, String externalReferenceCode)
		throws Exception {

		String response = null;

		try {
			response = get(
				authorization,
				UriComponentsBuilder.fromPath(
					"/o/c/projects/by-external-reference-code" +
						"/{externalReferenceCode}"
				).buildAndExpand(
					externalReferenceCode
				).toUri());
		}
		catch (WebClientResponseException webClientResponseException) {
			int statusCode = webClientResponseException.getStatusCode(
			).value();

			if (statusCode != HttpStatus.NOT_FOUND.value()) {
				throw webClientResponseException;
			}
		}

		if (Validator.isNull(response)) {
			return null;
		}

		return new Project(new JSONObject(response));
	}

	private static final Log _log = LogFactory.getLog(ProjectService.class);

}