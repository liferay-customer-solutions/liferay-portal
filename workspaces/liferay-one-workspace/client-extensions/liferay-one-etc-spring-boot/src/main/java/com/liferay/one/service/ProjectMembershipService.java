/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.one.model.ProjectMembership;
import com.liferay.petra.string.StringBundler;

import java.util.List;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Felipe Veloso
 */
@Component
public class ProjectMembershipService extends OneBaseService {

	public void addProjectMembership(
			long accountId, long userId, String projectExternalReferenceCode,
			String roleExternalReferenceCode)
		throws Exception {

		_accountService.ensureAccountUserAccount(accountId, userId);

		List<ProjectMembership> projectMemberships = _getProjectMemberships(
			accountId, userId, projectExternalReferenceCode);

		if (!projectMemberships.isEmpty()) {
			return;
		}

		JSONObject jsonObject = new JSONObject();

		jsonObject.put(
			"r_accountEntryToProjectMembership_accountEntryId", accountId
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

	public void deleteProjectMembership(
			long accountId, long userId, String projectExternalReferenceCode)
		throws Exception {

		List<ProjectMembership> projectMemberships = _getProjectMemberships(
			accountId, userId, projectExternalReferenceCode);

		for (ProjectMembership projectMembership : projectMemberships) {
			delete(
				getAuthorization(), "",
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
			long accountId, long userId, String projectExternalReferenceCode)
		throws Exception {

		return getAllItems(
			"/o/c/projectmemberships",
			StringBundler.concat(
				"r_accountEntryToProjectMembership_accountEntryId eq '",
				accountId, "' and r_userToProjectMembership_userId eq '",
				userId, "' and r_projectToProjectMembership_c_projectERC eq '",
				projectExternalReferenceCode, "'"),
			ProjectMembership::new);
	}

	@Autowired
	private AccountService _accountService;

}