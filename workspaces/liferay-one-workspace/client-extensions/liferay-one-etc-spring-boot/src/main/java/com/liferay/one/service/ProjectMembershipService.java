/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.one.model.Project;
import com.liferay.one.model.ProjectMembership;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;

import java.util.List;

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

	public void addProjectMembership(
			Jwt jwt, String projectExternalReferenceCode,
			String roleExternalReferenceCode, long userId)
		throws Exception {

		List<ProjectMembership> projectMemberships = _getProjectMemberships(
			jwt, projectExternalReferenceCode, roleExternalReferenceCode,
			userId);

		if (!projectMemberships.isEmpty()) {
			return;
		}

		Project project = _projectService.fetchProject(
			projectExternalReferenceCode, jwt);

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
	}

	public void deleteProjectMembership(
			Jwt jwt, String projectExternalReferenceCode,
			String roleExternalReferenceCode, long userId)
		throws Exception {

		List<ProjectMembership> projectMemberships = _getProjectMemberships(
			jwt, projectExternalReferenceCode, roleExternalReferenceCode,
			userId);

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

	@Autowired
	private AccountService _accountService;

	@Autowired
	private ProjectService _projectService;

	@Autowired
	private UserAccountService _userAccountService;

}