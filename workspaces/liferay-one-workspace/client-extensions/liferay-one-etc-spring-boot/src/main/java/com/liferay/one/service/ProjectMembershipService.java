/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.one.model.Project;
import com.liferay.one.model.ProjectMembership;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.util.Validator;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Felipe Veloso
 */
@Component
public class ProjectMembershipService extends OneBaseService {

	/**
	 * @return <code>true</code> if a project membership was added;
	 *         <code>false</code> if one already existed or the project was not
	 *         found
	 */
	public boolean addProjectMembership(
			Jwt jwt, String projectExternalReferenceCode,
			String roleExternalReferenceCode, long userId)
		throws Exception {

		List<ProjectMembership> projectMemberships = _getProjectMemberships(
			jwt, projectExternalReferenceCode, roleExternalReferenceCode,
			userId);

		if (!projectMemberships.isEmpty()) {
			return false;
		}

		Project project = _projectService.fetchProject(
			projectExternalReferenceCode, jwt);

		if (project == null) {
			return false;
		}

		if (!_userAccountService.hasAccountUserAccount(
				project.getAccountId(), userId)) {

			_accountService.addAccountUserAccount(
				project.getAccountId(), jwt, userId);
		}

		JSONObject jsonObject = new JSONObject();

		jsonObject.put(
			"r_accountEntryToProjectMembership_accountEntryId",
			project.getAccountId()
		).put(
			"r_projectToProjectMembership_c_projectERC",
			projectExternalReferenceCode
		).put(
			"r_userToProjectMembership_userId", userId
		).put(
			"roleExternalReferenceCode", roleExternalReferenceCode
		);

		post(
			getAuthorization(jwt), jsonObject.toString(),
			UriComponentsBuilder.fromPath(
				"/o/c/projectmemberships"
			).build(
			).toUri());

		return true;
	}

	public void addProjectMembership(
			String projectExternalReferenceCode, long userId)
		throws Exception {

		addProjectMembership(
			projectExternalReferenceCode,
			_PROJECT_USER_ROLE_EXTERNAL_REFERENCE_CODE, userId);
	}

	public void addProjectMembership(
			String projectExternalReferenceCode,
			String roleExternalReferenceCode, long userId)
		throws Exception {

		List<ProjectMembership> projectMemberships = _getProjectMemberships(
			null, projectExternalReferenceCode, roleExternalReferenceCode,
			userId);

		if (!projectMemberships.isEmpty()) {
			return;
		}

		Project project = _projectService.fetchProject(
			projectExternalReferenceCode);

		if (project == null) {
			return;
		}

		JSONObject jsonObject = new JSONObject();

		jsonObject.put(
			"r_accountEntryToProjectMembership_accountEntryId",
			project.getAccountId()
		).put(
			"r_projectToProjectMembership_c_projectERC",
			projectExternalReferenceCode
		).put(
			"r_userToProjectMembership_userId", userId
		).put(
			"roleExternalReferenceCode", roleExternalReferenceCode
		);

		post(
			getAuthorization(), jsonObject.toString(),
			UriComponentsBuilder.fromPath(
				"/o/c/projectmemberships"
			).build(
			).toUri());
	}

	/**
	 * @return <code>true</code> if at least one project membership was deleted;
	 *         <code>false</code> if none matched
	 */
	public boolean deleteProjectMembership(
			Jwt jwt, String projectExternalReferenceCode,
			String roleExternalReferenceCode, long userId)
		throws Exception {

		List<ProjectMembership> projectMemberships = _getProjectMemberships(
			jwt, projectExternalReferenceCode, roleExternalReferenceCode,
			userId);

		if (projectMemberships.isEmpty()) {
			return false;
		}

		for (ProjectMembership projectMembership : projectMemberships) {
			delete(
				getAuthorization(jwt), StringPool.BLANK,
				UriComponentsBuilder.fromPath(
					"/o/c/projectmemberships/by-external-reference-code" +
						"/{externalReferenceCode}"
				).buildAndExpand(
					projectMembership.getExternalReferenceCode()
				).toUri());
		}

		return true;
	}

	public ProjectMembership fetchProjectMembership(
			String projectExternalReferenceCode,
			String roleExternalReferenceCode, long userId)
		throws Exception {

		StringBundler sb = new StringBundler(8);

		sb.append("(r_projectToProjectMembership_c_projectERC eq '");
		sb.append(escapeODataString(projectExternalReferenceCode));
		sb.append("') and (r_userToProjectMembership_userId eq '");
		sb.append(userId);
		sb.append("')");

		if (Validator.isNotNull(roleExternalReferenceCode)) {
			sb.append(" and (roleExternalReferenceCode eq '");
			sb.append(roleExternalReferenceCode);
			sb.append("')");
		}

		String filterString = sb.toString();

		String response = get(
			getAuthorization(),
			UriComponentsBuilder.fromPath(
				"/o/c/projectmemberships"
			).queryParam(
				"filter", filterString
			).queryParam(
				"page", 1
			).queryParam(
				"pageSize", 1
			).build(
			).toUri());

		if (Validator.isNull(response)) {
			return null;
		}

		JSONObject jsonObject = new JSONObject(response);

		JSONArray jsonArray = jsonObject.optJSONArray("items");

		if ((jsonArray == null) || (jsonArray.length() == 0)) {
			return null;
		}

		return new ProjectMembership(jsonArray.getJSONObject(0));
	}

	public List<ProjectMembership> getProjectMemberships(
			long accountId, long userId)
		throws Exception {

		return getAllItems(
			"/o/c/projectmemberships",
			StringBundler.concat(
				"r_accountEntryToProjectMembership_accountEntryId eq '",
				accountId, "' and r_userToProjectMembership_userId eq '",
				userId, "'"),
			ProjectMembership::new);
	}

	public List<ProjectMembership> getProjectMemberships(
			String projectExternalReferenceCode)
		throws Exception {

		return getAllItems(
			"/o/c/projectmemberships",
			"r_projectToProjectMembership_c_projectERC eq '" +
				projectExternalReferenceCode + "'",
			ProjectMembership::new);
	}

	public List<ProjectMembership> getProjectMemberships(
			String projectExternalReferenceCode, long userId)
		throws Exception {

		return _getProjectMemberships(
			null, projectExternalReferenceCode, null, userId);
	}

	public List<ProjectMembership> getProjectMembershipsByUserId(long userId)
		throws Exception {

		return getAllItems(
			"/o/c/projectmemberships",
			"r_userToProjectMembership_userId eq '" + userId + "'",
			ProjectMembership::new);
	}

	private List<ProjectMembership> _getProjectMemberships(
			Jwt jwt, String projectExternalReferenceCode,
			String roleExternalReferenceCode, long userId)
		throws Exception {

		String filterString = StringBundler.concat(
			"r_userToProjectMembership_userId eq '", userId,
			"' and r_projectToProjectMembership_c_projectERC eq '",
			projectExternalReferenceCode, "'");

		if (roleExternalReferenceCode != null) {
			filterString = StringBundler.concat(
				filterString, " and roleExternalReferenceCode eq '",
				roleExternalReferenceCode, "'");
		}

		return getAllItems(
			"/o/c/projectmemberships", filterString, ProjectMembership::new,
			jwt);
	}

	private static final String _PROJECT_USER_ROLE_EXTERNAL_REFERENCE_CODE =
		"C_PROJECT_USER";

	@Autowired
	private AccountService _accountService;

	@Autowired
	private ProjectService _projectService;

	@Autowired
	private UserAccountService _userAccountService;

}