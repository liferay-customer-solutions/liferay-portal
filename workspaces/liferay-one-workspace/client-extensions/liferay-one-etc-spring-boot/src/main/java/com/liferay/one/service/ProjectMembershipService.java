/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.one.model.ProjectMembership;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

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

	public List<ProjectMembership> getProjectMemberships(
			long accountId, long userId)
		throws Exception {

		List<ProjectMembership> projectMemberships = new ArrayList<>();

		String filterString = StringBundler.concat(
			"r_accountEntryToProjectMembership_accountEntryId eq '", accountId,
			"' and r_userToProjectMembership_userId eq '", userId, "'");

		int page = 1;

		while (true) {
			String response = get(
				getAuthorization(),
				UriComponentsBuilder.fromPath(
					"/o/c/projectmemberships"
				).queryParam(
					"filter", filterString
				).queryParam(
					"page", page
				).queryParam(
					"pageSize", _PAGE_SIZE
				).build(
				).toUri());

			if (Validator.isNull(response)) {
				return projectMemberships;
			}

			JSONObject jsonObject = new JSONObject(response);

			JSONArray jsonArray = jsonObject.optJSONArray("items");

			if (jsonArray == null) {
				return projectMemberships;
			}

			for (int i = 0; i < jsonArray.length(); i++) {
				projectMemberships.add(
					new ProjectMembership(jsonArray.getJSONObject(i)));
			}

			if (jsonArray.length() < _PAGE_SIZE) {
				return projectMemberships;
			}

			page++;
		}
	}

	private static final int _PAGE_SIZE = 200;

}